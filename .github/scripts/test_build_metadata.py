"""Offline NeoForge packaging checks; fixtures never build or contact services."""

import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

import build_metadata as metadata


PROPERTIES = """version=1.0.0
mod_id=olympus
mod_name=Olympus
minecraft_version=26.3
minecraft_version_range=[26.3,26.4)
neoforge_version=26.3.0.92
neoforge_loader_version_range=[4,)
"""
MANIFEST = """Manifest-Version: 1.0
Implementation-Version: 1.0.0
Specification-Version: 1.0.0
Implementation-Title: neoforge
Built-On-Minecraft: 26.3

"""
MODS = """modLoader="javafml"
loaderVersion="[4,)"
[[mods]]
modId="olympus"
version="1.0.0"
[[dependencies.olympus]]
modId="minecraft"
type="required"
versionRange="[26.3,26.4)"
side="BOTH"
[[dependencies.olympus]]
modId="neoforge"
type="required"
versionRange="[26.3.0.92,)"
side="BOTH"
"""
BASE = "olympus-neoforge-26.3-1.0.0"


class PackagingTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / "gradle.properties").write_text(PROPERTIES, encoding="utf-8")
        self.libraries = self.root / "neoforge/build/libs"
        self.libraries.mkdir(parents=True)
        self.output = self.root / "artifacts"
        self.git_patch = patch.object(metadata, "git", side_effect=lambda root, *args: {
            "rev-parse": "a" * 40, "branch": "main", "log": "A test commit",
        }[args[0]])
        self.git_patch.start()
        self.addCleanup(self.git_patch.stop)
        self.environment = patch.dict(os.environ, {"GITHUB_RUN_ID": "123", "GITHUB_RUN_ATTEMPT": "2", "GITHUB_RUN_NUMBER": "45"}, clear=True)
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def jar(self, name=BASE + ".jar", manifest=MANIFEST, mods=MODS, extra=None):
        path = self.libraries / name
        with zipfile.ZipFile(path, "w") as archive:
            if manifest is not None:
                archive.writestr("META-INF/MANIFEST.MF", manifest)
            if mods is not None:
                archive.writestr("META-INF/neoforge.mods.toml", mods)
            archive.writestr("dev/xylonity/olympus/OlympusNeoForge.class", b"fake fixture bytecode")
            if extra:
                archive.writestr(extra, b"test")
        return path

    def collect(self, **kwargs):
        return metadata.collect(self.root, self.output, **kwargs)

    def test_complete_package_and_checksums(self):
        for suffix in ("", "-sources", "-javadoc"):
            self.jar(BASE + suffix + ".jar")
        info = self.collect(tag="v1.0.0")
        self.assertEqual(info["projectName"], "Olympus")
        self.assertEqual(info["jar"], BASE + ".jar")
        self.assertEqual(info["artifactName"], "olympus-neoforge-26.3-123-2")
        self.assertEqual(info["minecraftVersion"], "26.3")
        self.assertEqual(info["neoForgeVersion"], "26.3.0.92")
        self.assertEqual(len(info["supplementalJars"]), 2)
        self.assertEqual(len(list(self.output.iterdir())), 5)
        expected_sums = ""
        for item in info["files"]:
            payload = (self.output / item["name"]).read_bytes()
            self.assertEqual(item["sha256"], hashlib.sha256(payload).hexdigest())
            self.assertEqual(item["size"], len(payload))
            expected_sums += f"{item['sha256']}  {item['name']}\n"
        self.assertEqual((self.output / "SHA256SUMS").read_text(), expected_sums)
        self.assertEqual(info["sha256"], info["files"][0]["sha256"])
        self.assertEqual(json.loads((self.output / "build-info.json").read_text()), info)
        self.assertNotIn("versionCode", info)

    def test_old_minecraft_and_version_jars_are_ignored(self):
        self.jar()
        self.jar("olympus-neoforge-26.1.2-1.0.0.jar")
        self.jar("olympus-neoforge-26.3-0.9.0.jar")
        self.assertEqual(len(self.collect()["files"]), 1)

    def test_missing_current_artifact_never_uses_old_jar(self):
        self.jar("olympus-neoforge-26.1.2-1.0.0.jar")
        with self.assertRaisesRegex(ValueError, "Missing"):
            self.collect()
        self.assertFalse(self.output.exists())

    def test_extra_current_classifier_is_ambiguous(self):
        self.jar()
        self.jar(BASE + "-all.jar")
        with self.assertRaisesRegex(ValueError, "Ambiguous"):
            self.collect()

    def test_corrupt_and_empty_jar_rejected(self):
        for payload in (b"", b"this is not a JAR"):
            with self.subTest(payload=payload):
                (self.libraries / (BASE + ".jar")).write_bytes(payload)
                with self.assertRaises(ValueError):
                    self.collect()

    def test_manifest_stale_version_minecraft_or_missing_rejected(self):
        for manifest in (None, MANIFEST.replace("1.0.0", "0.9.0"), MANIFEST.replace("26.3", "26.1.2")):
            with self.subTest(manifest=manifest):
                self.jar(manifest=manifest)
                with self.assertRaises(ValueError):
                    self.collect()

    def test_metadata_requirements_and_mod_identity(self):
        for mods in (None, "not valid TOML!", MODS.replace('modId="olympus"', 'modId="other"'),
                     MODS.replace('version="1.0.0"', 'version="0.9.0"'),
                     MODS.replace("26.3.0.92", "26.1.2.1"), MODS.replace("[26.3,26.4)", "[26.1,26.2)"),
                     MODS.replace('type="required"', 'type="optional"'),
                     MODS + '\n[[mods]]\nmodId="olympus_tests"\nversion="1.0.0"\n'):
            with self.subTest(mods=mods):
                self.jar(mods=mods)
                with self.assertRaises(ValueError):
                    self.collect()

    def test_test_mod_content_rejected_in_all_archives(self):
        self.jar()
        for suffix in ("", "-sources", "-javadoc"):
            with self.subTest(suffix=suffix):
                path = self.jar(BASE + suffix + ".jar", extra="data/olympus_tests/structure/platform.nbt")
                with self.assertRaisesRegex(ValueError, "Test mod"):
                    self.collect()
                path.unlink()
                self.jar()

    def test_corrupt_supplemental_jar_rejected(self):
        self.jar()
        (self.libraries / (BASE + "-sources.jar")).write_bytes(b"broken")
        with self.assertRaises(ValueError):
            self.collect()
        self.assertFalse(self.output.exists())

    def test_test_classes_and_test_dependency_rejected(self):
        self.jar(extra="dev/xylonity/olympus/test/OlympusGameTests.class")
        with self.assertRaisesRegex(ValueError, "Test mod"):
            self.collect()
        self.jar(mods=MODS + '\n[[dependencies.olympus]]\nmodId="olympus_tests"\ntype="required"\n')
        with self.assertRaisesRegex(ValueError, "Test mod dependency"):
            self.collect()

    def test_malformed_metadata_shape_rejected(self):
        for mods in ('mods="wrong"', MODS.replace('[[dependencies.olympus]]', '[dependencies.olympus]', 1).split('[[dependencies.olympus]]')[0]):
            with self.subTest(mods=mods):
                self.jar(mods=mods)
                with self.assertRaises(ValueError):
                    self.collect()

    def test_duplicate_archive_entries_rejected(self):
        path = self.jar()
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", UserWarning)
            with zipfile.ZipFile(path, "a") as archive:
                archive.writestr("META-INF/MANIFEST.MF", MANIFEST)
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.collect()

    def test_nonempty_output_refused_without_removing_files(self):
        self.jar()
        self.output.mkdir()
        stale = self.output / "unrelated.txt"
        stale.write_text("keep me")
        with self.assertRaisesRegex(ValueError, "empty"):
            self.collect()
        self.assertEqual(stale.read_text(), "keep me")

    def test_output_inside_libraries_refused(self):
        self.jar()
        with self.assertRaisesRegex(ValueError, "outside"):
            metadata.collect(self.root, self.libraries / "packaged")

    def test_tag_exact_version_required(self):
        self.jar()
        for tag in ("v2.0.0", "1.0.0", "v1.0.0-rc1", "v1.0.0\n"):
            with self.subTest(tag=tag), self.assertRaisesRegex(ValueError, "exactly"):
                self.collect(tag=tag)
        properties = metadata.read_properties(self.root / "gradle.properties")
        properties["version"] = "1.1.0-rc.1"
        self.assertTrue(metadata.check_tag(properties, "v1.1.0-rc.1"))
        properties["version"] = "1.1.0+neoforge-26.3"
        self.assertFalse(metadata.check_tag(properties, "v1.1.0+neoforge-26.3"))

    def test_github_outputs(self):
        self.jar()
        output = self.root / "github-output"
        with patch.dict(os.environ, {"GITHUB_OUTPUT": str(output)}):
            self.collect()
        fields = dict(line.split("=", 1) for line in output.read_text().splitlines())
        self.assertEqual(fields["version"], "1.0.0")
        self.assertEqual(fields["prerelease"], "false")
        self.assertEqual(fields["jar"], BASE + ".jar")

    def test_manifest_continuation_lines(self):
        self.jar(manifest=MANIFEST.replace("Implementation-Version: 1.0.0", "Implementation-Version: 1.\r\n 0.0"))
        self.assertEqual(self.collect()["versionName"], "1.0.0")

    def test_unsafe_or_missing_properties(self):
        for properties in (PROPERTIES.replace("version=1.0.0", "version=../../bad"), PROPERTIES.replace("version=1.0.0", ""), PROPERTIES + "version=2\n"):
            with self.subTest(properties=properties):
                (self.root / "gradle.properties").write_text(properties)
                with self.assertRaises(ValueError):
                    self.collect()


if __name__ == "__main__":
    unittest.main()
