"""Collect validated NeoForge build outputs; uses only the Python 3.11+ stdlib."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tomllib
import zipfile


def read_properties(path):
    """Read the simple key=value properties used by this Gradle project."""
    properties = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        key, separator, value = line.partition("=")
        if not separator or key.strip() in properties:
            raise ValueError("Expected unique key=value entries in gradle.properties")
        properties[key.strip()] = value.strip()
    for key in ("version", "mod_id", "minecraft_version", "minecraft_version_range",
                "neoforge_version", "neoforge_loader_version_range"):
        if not properties.get(key):
            raise ValueError(f"Missing Gradle property: {key}")
    for key in ("version", "mod_id", "minecraft_version", "neoforge_version"):
        if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._+-]*", properties[key]):
            raise ValueError(f"Unsafe artifact naming property: {key}")
    return properties


def check_tag(properties, tag):
    if tag != f"v{properties['version']}":
        raise ValueError("Release tag must exactly match v{version} from gradle.properties")
    return "-" in properties["version"].split("+", 1)[0]


def read_manifest(raw):
    # JAR attributes can continue on the next physical line with a leading space.
    unfolded = re.sub(r"\r?\n ", "", raw.decode("utf-8"))
    fields = {}
    for line in unfolded.splitlines():
        if not line:
            break  # Only the main manifest section describes the artifact.
        key, separator, value = line.partition(": ")
        if not separator or key in fields:
            raise ValueError("Invalid or duplicate manifest attribute")
        fields[key] = value
    return fields


def validate_jar(path, properties, production=False):
    if not path.is_file() or path.is_symlink() or path.stat().st_size == 0:
        raise ValueError(f"Missing, empty or symlinked JAR: {path.name}")
    try:
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
            if not names or len(names) != len(set(names)):
                raise ValueError(f"Empty archive or duplicate entries: {path.name}")
            for name in names:
                if "olympus_tests" in name.lower() or name.startswith("dev/xylonity/olympus/test/"):
                    raise ValueError(f"Test mod content in distribution JAR: {path.name}")
            if archive.testzip() is not None:
                raise ValueError(f"Corrupt JAR: {path.name}")
            if not production:
                return
            manifest = read_manifest(archive.read("META-INF/MANIFEST.MF"))
            for key, expected in {
                "Implementation-Version": properties["version"],
                "Specification-Version": properties["version"],
                "Implementation-Title": "neoforge",
                "Built-On-Minecraft": properties["minecraft_version"],
            }.items():
                if manifest.get(key) != expected:
                    raise ValueError(f"JAR manifest {key} does not match current Gradle configuration")
            metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode("utf-8"))
            mod_id = properties["mod_id"]
            mods = metadata.get("mods", [])
            if not isinstance(mods, list) or len(mods) != 1 or not isinstance(mods[0], dict) or mods[0].get("modId") != mod_id:
                raise ValueError("Production JAR must contain exactly the configured production mod")
            if mods[0].get("version") != properties["version"]:
                raise ValueError("JAR mod version does not match current Gradle configuration")
            if metadata.get("modLoader") != "javafml" or metadata.get("loaderVersion") != properties["neoforge_loader_version_range"]:
                raise ValueError("JAR loader requirements do not match current Gradle configuration")
            dependencies = metadata.get("dependencies", {})
            if not isinstance(dependencies, dict) or set(dependencies) != {mod_id}:
                raise ValueError("Unexpected dependency owner in production JAR")
            requirements = dependencies[mod_id]
            if not isinstance(requirements, list) or not all(isinstance(item, dict) for item in requirements):
                raise ValueError("Invalid dependencies in production JAR")
            if any(item.get("modId") == "olympus_tests" for item in requirements):
                raise ValueError("Test mod dependency in production JAR")
            for dependency, version_range in {
                "minecraft": properties["minecraft_version_range"],
                "neoforge": f"[{properties['neoforge_version']},)",
            }.items():
                matching = [item for item in requirements if item.get("modId") == dependency]
                if len(matching) != 1 or matching[0].get("type") != "required" or matching[0].get("versionRange") != version_range or matching[0].get("side") != "BOTH":
                    raise ValueError(f"JAR {dependency} requirement does not match current Gradle configuration")
    except (zipfile.BadZipFile, KeyError, UnicodeError, tomllib.TOMLDecodeError) as error:
        raise ValueError(f"Invalid JAR metadata: {path.name}") from error


def git(root, *args):
    return subprocess.check_output(["git", "-C", str(root), *args], text=True, encoding="utf-8").strip()


def github_output(name, value):
    if "\n" in str(value) or "\r" in str(value):
        raise ValueError("GitHub output must be one line")
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
            stream.write(f"{name}={value}\n")


def collect(project_root, output_dir, tag=None):
    root = Path(project_root).resolve()
    destination = Path(output_dir).resolve()
    properties = read_properties(root / "gradle.properties")
    prerelease = check_tag(properties, tag) if tag else False
    base = f"{properties['mod_id']}-neoforge-{properties['minecraft_version']}-{properties['version']}"
    libraries = root / "neoforge/build/libs"
    if destination == libraries or libraries in destination.parents:
        raise ValueError("Output directory must be outside build/libs")
    expected = {base + ".jar": "production", base + "-sources.jar": "sources", base + "-javadoc.jar": "javadoc"}
    unexpected = [p.name for p in libraries.glob(f"{base}-*.jar") if p.name not in expected]
    if unexpected:
        raise ValueError("Ambiguous current-version JAR classifiers: " + ", ".join(sorted(unexpected)))
    selected = [(libraries / name, kind) for name, kind in expected.items()
                if kind == "production" or (libraries / name).exists()]
    for path, kind in selected:
        validate_jar(path, properties, production=kind == "production")
    # Refuse to mix packages from different runs, including leftover supplemental JARs.
    if destination.exists() and (not destination.is_dir() or any(destination.iterdir())):
        raise ValueError("Output directory must be empty to avoid stale release attachments")
    commit = git(root, "rev-parse", "HEAD")
    run_id = os.environ.get("GITHUB_RUN_ID", "local")
    attempt = os.environ.get("GITHUB_RUN_ATTEMPT", "1")
    artifact_name = f"{properties['mod_id']}-neoforge-{properties['minecraft_version']}-{run_id}-{attempt}"
    if not re.fullmatch(r"[A-Za-z0-9._+-]+", artifact_name):
        raise ValueError("Unsafe artifact name")
    repository = os.environ.get("GITHUB_REPOSITORY", "badnng/Olympus-26.3")
    server = os.environ.get("GITHUB_SERVER_URL", "https://github.com").rstrip("/")
    info = {
        "projectName": properties.get("mod_name", "Olympus"),
        "versionName": properties["version"],
        "buildVersion": properties["version"],
        "modId": properties["mod_id"],
        "loader": "neoforge",
        "minecraftVersion": properties["minecraft_version"],
        "neoForgeVersion": properties["neoforge_version"],
        "jar": base + ".jar",
        "supplementalJars": [p.name for p, kind in selected if kind != "production"],
        "repository": repository,
        "branch": os.environ.get("GITHUB_HEAD_REF") or os.environ.get("GITHUB_REF_NAME") or git(root, "branch", "--show-current"),
        "commit": commit,
        "commitMessage": git(root, "log", "-1", "--format=%B"),
        "runNumber": os.environ.get("GITHUB_RUN_NUMBER", "local"),
        "runUrl": f"{server}/{repository}/actions/runs/{run_id}" if run_id != "local" else "",
        "artifactName": artifact_name,
        "tag": tag,
        "prerelease": prerelease,
        "files": [],
    }
    destination.mkdir(parents=True, exist_ok=True)
    for path, kind in selected:
        copied = destination / path.name
        shutil.copy2(path, copied)
        with copied.open("rb") as stream:
            digest = hashlib.file_digest(stream, "sha256").hexdigest()
        info["files"].append({"name": copied.name, "sha256": digest, "size": copied.stat().st_size, "kind": kind})
        if kind == "production":
            info["sha256"] = digest
    (destination / "SHA256SUMS").write_text("".join(f"{item['sha256']}  {item['name']}\n" for item in info["files"]), encoding="utf-8")
    (destination / "build-info.json").write_text(json.dumps(info, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    for name, value in {"artifact_name": artifact_name, "jar": info["jar"], "version": properties["version"], "prerelease": str(prerelease).lower()}.items():
        github_output(name, value)
    return info


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["collect", "check-tag"])
    parser.add_argument("--project-root", type=Path, default=Path("."))
    parser.add_argument("--output-dir", type=Path, default=Path("artifacts"))
    parser.add_argument("--tag")
    args = parser.parse_args()
    try:
        if args.command == "check-tag":
            if not args.tag:
                parser.error("check-tag requires --tag")
            prerelease = check_tag(read_properties(args.project_root / "gradle.properties"), args.tag)
            github_output("prerelease", str(prerelease).lower())
        else:
            collect(args.project_root, args.output_dir, args.tag)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"Metadata collection failed: {error}\n")


if __name__ == "__main__":
    main()
