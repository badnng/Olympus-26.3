"""Stage verified JAR assets in a draft before publishing; never clobber an asset."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile

from build_metadata import check_tag, read_properties


TRUSTED_REPOSITORY = "badnng/Olympus-26.3"


def validate_repository(repository):
    if repository != TRUSTED_REPOSITORY:
        raise ValueError("Release publication is restricted to the configured Olympus repository")


def filename(value):
    if not isinstance(value, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._+-]*", value):
        raise ValueError("Invalid release asset filename")
    return value


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def load_bundle(directory, repository, tag):
    root = Path(directory).resolve(strict=True)
    metadata = root / "build-info.json"
    if metadata.is_symlink():
        raise ValueError("Build metadata must be a regular artifact file")
    info = json.loads(metadata.read_text(encoding="utf-8"))
    if info.get("repository") != repository or info.get("tag") != tag or tag != "v" + info.get("versionName", ""):
        raise ValueError("Release artifact repository, tag or version does not match this run")
    if not re.fullmatch(r"[0-9a-f]{40}", info.get("commit", "")):
        raise ValueError("Build metadata has no valid source commit")
    if os.environ.get("GITHUB_SHA") and info["commit"] != os.environ["GITHUB_SHA"]:
        raise ValueError("Release artifact was built from a different source commit")
    entries = info.get("files", [])
    if not isinstance(entries, list) or not entries:
        raise ValueError("Release artifact has no verified JAR manifest")
    paths = {}
    checksums = []
    for entry in entries:
        name = filename(entry["name"])
        path = root / name
        if name in paths or not name.endswith(".jar") or path.is_symlink() or path.resolve().parent != root or not path.is_file():
            raise ValueError("Invalid or duplicate release JAR")
        if entry.get("size") != path.stat().st_size or path.stat().st_size <= 0 or entry.get("sha256") != digest(path):
            raise ValueError("Release JAR size or SHA-256 verification failed")
        paths[name] = path
        checksums.append(f"{entry['sha256']}  {name}")
    primary = filename(info.get("jar"))
    if primary not in paths or info.get("sha256") != digest(paths[primary]):
        raise ValueError("Production JAR does not match the build metadata")
    checksum_file = root / "SHA256SUMS"
    if checksum_file.is_symlink() or checksum_file.read_text(encoding="utf-8").splitlines() != checksums:
        raise ValueError("SHA256SUMS does not match the verified release JARs")
    paths.update({"SHA256SUMS": checksum_file, "build-info.json": metadata})
    if {path.name for path in root.iterdir()} != set(paths):
        raise ValueError("Release artifact contains unexpected attachments")
    return info, paths


class GitHub:
    def __init__(self, repository):
        validate_repository(repository)
        self.repository = repository

    def command(self, arguments, *, input_text=None, allow_missing=False):
        result = subprocess.run(["gh", *arguments], input=input_text, text=True, encoding="utf-8", capture_output=True)
        if result.returncode:
            if allow_missing and "(HTTP 404)" in result.stderr:
                return None
            raise RuntimeError("GitHub release operation failed; check authentication, permissions and network")
        return result.stdout

    def find(self, tag):
        # REST's tag endpoint only finds published releases. GraphQL also exposes drafts.
        owner, name = self.repository.split("/", 1)
        query = "query($owner:String!,$name:String!,$tag:String!){repository(owner:$owner,name:$name){release(tagName:$tag){databaseId}}}"
        reference = json.loads(self.command(["api", "graphql", "-f", f"query={query}",
                                            "-f", f"owner={owner}", "-f", f"name={name}", "-f", f"tag={tag}"]))
        repository = reference.get("data", {}).get("repository")
        if repository is None:
            raise RuntimeError("Cannot read the configured release repository")
        record = repository.get("release")
        if record is None:
            return None
        release_id = record.get("databaseId")
        if type(release_id) is not int or release_id <= 0:
            raise RuntimeError("GitHub returned an invalid release identifier")
        response = self.command(["api", f"repos/{self.repository}/releases/{release_id}"], allow_missing=True)
        return None if response is None else json.loads(response)

    def create_draft(self, tag, info, notes):
        arguments = ["release", "create", tag, "--repo", self.repository, "--verify-tag", "--draft",
                     "--title", f"{info['projectName']} {tag} (Minecraft {info['minecraftVersion']})", "--notes-file", str(notes)]
        if info["prerelease"]:
            arguments.append("--prerelease")
        self.command(arguments)

    def upload(self, tag, paths):
        self.command(["release", "upload", tag, *map(str, paths), "--repo", self.repository])

    def download(self, tag, name, directory):
        self.command(["release", "download", tag, "--repo", self.repository, "--pattern", name, "--dir", str(directory)])

    def publish(self, release_id, prerelease):
        # Let GitHub choose the highest stable release instead of forcing an older tag to latest.
        payload = {"draft": False, "prerelease": prerelease, "make_latest": "false" if prerelease else "legacy"}
        self.command(["api", "--method", "PATCH", f"repos/{self.repository}/releases/{int(release_id)}", "--input", "-"],
                     input_text=json.dumps(payload))


def require_draft(release):
    if release is not None and release.get("draft") is not True:
        raise RuntimeError("This tag already has a published release; publish a new version instead")


def assets_signature(release):
    # Reading an asset changes its download_count, which is not a content change.
    return sorted((asset.get("id"), asset["name"], asset["size"], asset.get("digest"), asset.get("state"))
                  for asset in release.get("assets", []))


def verify_assets(client, tag, release, paths):
    assets = release.get("assets", [])
    names = [asset["name"] for asset in assets]
    if len(names) != len(set(names)) or set(names) != set(paths):
        raise RuntimeError("Draft assets are incomplete or contain unexpected files; leaving the release unpublished")
    with tempfile.TemporaryDirectory(prefix="olympus-release-verify-") as temporary:
        directory = Path(temporary)
        for asset in assets:
            name = asset["name"]
            if asset["size"] != paths[name].stat().st_size:
                raise RuntimeError("Draft asset size verification failed; leaving the release unpublished")
            client.download(tag, name, directory)
            if digest(directory / name) != digest(paths[name]):
                raise RuntimeError("Draft asset SHA-256 verification failed; leaving the release unpublished")


def publish_bundle(client, directory, repository, tag):
    validate_repository(repository)
    info, paths = load_bundle(directory, repository, tag)
    release = client.find(tag)
    require_draft(release)
    if release is None:
        with tempfile.TemporaryDirectory(prefix="olympus-release-notes-") as temporary:
            notes = Path(temporary) / "release-notes.md"
            notes.write_text(
                f"Olympus {info['versionName']} for Minecraft {info['minecraftVersion']}.\n\n"
                f"Requires NeoForge {info['neoForgeVersion']} or newer for this Minecraft version, plus Curios and GeckoLib.\n\n"
                f"Source commit: `{info['commit']}`. Built from {info['runUrl']}.\n\n"
                "Includes the mod, available source/Javadoc JARs, SHA256SUMS and build-info.json.\n"
                "Original Olympus authors: Xylonity and mongoose_artist. Source and assets retain their respective licenses.\n",
                encoding="utf-8")
            client.create_draft(tag, info, notes)
        release = client.find(tag)
    require_draft(release)
    if release is None:
        raise RuntimeError("New draft release could not be verified")
    existing = {asset["name"]: asset for asset in release.get("assets", [])}
    if not set(existing) <= set(paths):
        raise RuntimeError("Existing draft contains unrelated assets; refusing to overwrite or remove them")
    # On a failed-job rerun, reuse only byte-identical partial assets. No --clobber is ever used.
    with tempfile.TemporaryDirectory(prefix="olympus-release-resume-") as temporary:
        for name in existing:
            client.download(tag, name, Path(temporary))
            if digest(Path(temporary) / name) != digest(paths[name]):
                raise RuntimeError("Existing draft asset differs; refusing to overwrite it")
    missing = [path for name, path in paths.items() if name not in existing]
    if missing:
        client.upload(tag, missing)
    release = client.find(tag)
    require_draft(release)
    if release is None:
        raise RuntimeError("Draft disappeared before publication")
    verify_assets(client, tag, release, paths)
    # Re-check after downloading assets, immediately before the publication mutation.
    current = client.find(tag)
    require_draft(current)
    if current is None or current["id"] != release["id"] or assets_signature(current) != assets_signature(release):
        raise RuntimeError("Draft changed during verification; leaving it unpublished")
    client.publish(release["id"], info["prerelease"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["check", "publish"])
    parser.add_argument("--repository", required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--directory", type=Path)
    parser.add_argument("--project-root", type=Path, default=Path.cwd())
    args = parser.parse_args()
    try:
        validate_repository(args.repository)
        check_tag(read_properties(args.project_root / "gradle.properties"), args.tag)
        client = GitHub(args.repository)
        if args.command == "check":
            require_draft(client.find(args.tag))
            print("Tag version matches; no published release will be overwritten")
        elif args.directory is None:
            parser.error("--directory is required for publication")
        else:
            publish_bundle(client, args.directory, args.repository, args.tag)
            print("All release assets verified before publication")
    except (ValueError, RuntimeError, KeyError, OSError, json.JSONDecodeError) as error:
        parser.exit(1, f"Release operation failed: {error}\n")


if __name__ == "__main__":
    main()
