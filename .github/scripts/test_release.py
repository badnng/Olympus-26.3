"""Offline release boundary checks. All GitHub operations are simulated."""

import copy
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import release


class FakeGitHub:
    def __init__(self, files, *, published=False, exists=False, corrupt_download=False, fail_upload=False):
        self.files = files
        self.exists = exists
        self.published = published
        self.corrupt_download = corrupt_download
        self.fail_upload = fail_upload
        self.remote = {}
        self.calls = []

    def find(self, tag):
        self.calls.append("find")
        if not self.exists:
            return None
        return {"id": 9, "draft": not self.published, "assets": [
            {"id": index, "name": name, "size": len(data), "download_count": len(self.calls), "state": "uploaded"}
            for index, (name, data) in enumerate(sorted(self.remote.items()))]}

    def create_draft(self, tag, info, notes):
        assert Path(notes).is_file()
        assert "Xylonity" in Path(notes).read_text(encoding="utf-8")
        self.calls.append("create-draft")
        self.exists = True

    def upload(self, tag, paths):
        self.calls.append("upload")
        if self.fail_upload:
            raise RuntimeError("simulated upload failure")
        for path in paths:
            assert path.name not in self.remote
            self.remote[path.name] = path.read_bytes()

    def download(self, tag, name, directory):
        self.calls.append("download")
        Path(directory, name).write_bytes(b"corrupt" if self.corrupt_download else self.remote[name])

    def publish(self, release_id, prerelease):
        self.calls.append("publish")
        self.published = True


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.addCleanup(self.temporary.cleanup)
        self.environment = patch.dict(os.environ, {}, clear=True)
        self.environment.start()
        self.addCleanup(self.environment.stop)
        jar = self.root / "olympus-neoforge-26.3-1.0.0.jar"
        jar.write_bytes(b"verified production JAR fixture")
        sha = hashlib.sha256(jar.read_bytes()).hexdigest()
        self.info = {"projectName": "Olympus", "versionName": "1.0.0", "tag": "v1.0.0",
                     "repository": release.TRUSTED_REPOSITORY, "commit": "a" * 40, "jar": jar.name,
                     "sha256": sha, "prerelease": False, "minecraftVersion": "26.3", "neoForgeVersion": "26.3.0.92",
                     "runUrl": "https://github.com/badnng/Olympus-26.3/actions/runs/1",
                     "files": [{"name": jar.name, "sha256": sha, "size": jar.stat().st_size, "kind": "production"}]}
        self.write_info()
        (self.root / "SHA256SUMS").write_text(f"{sha}  {jar.name}\n", encoding="utf-8")

    def write_info(self):
        (self.root / "build-info.json").write_text(json.dumps(self.info), encoding="utf-8")

    def client(self, **options):
        return FakeGitHub({p.name: p.read_bytes() for p in self.root.iterdir()}, **options)

    def publish(self, client):
        release.publish_bundle(client, self.root, release.TRUSTED_REPOSITORY, "v1.0.0")

    def test_publish_requires_all_uploaded_assets_to_be_downloaded_and_verified(self):
        client = self.client()
        self.publish(client)
        self.assertTrue(client.published)
        self.assertEqual(set(client.remote), {p.name for p in self.root.iterdir()})
        self.assertLess(client.calls.index("create-draft"), client.calls.index("upload"))
        self.assertEqual(client.calls.count("download"), len(client.remote))
        self.assertEqual(client.calls[-1], "publish")

    def test_published_tag_is_never_uploaded_or_modified(self):
        client = self.client(published=True, exists=True)
        with self.assertRaisesRegex(RuntimeError, "already has a published"):
            self.publish(client)
        self.assertEqual(client.calls, ["find"])

    def test_failed_upload_leaves_an_unpublished_draft(self):
        client = self.client(fail_upload=True)
        with self.assertRaisesRegex(RuntimeError, "upload failure"):
            self.publish(client)
        self.assertTrue(client.exists)
        self.assertFalse(client.published)

    def test_corrupted_remote_asset_prevents_publication(self):
        client = self.client(corrupt_download=True)
        with self.assertRaisesRegex(RuntimeError, "SHA-256"):
            self.publish(client)
        self.assertFalse(client.published)

    def test_identical_partial_draft_resumes_without_clobbering(self):
        client = self.client(exists=True)
        name = self.info["jar"]
        client.remote[name] = (self.root / name).read_bytes()
        self.publish(client)
        self.assertTrue(client.published)
        self.assertEqual(client.calls.count("upload"), 1)

    def test_conflicting_partial_draft_is_not_overwritten(self):
        client = self.client(exists=True)
        client.remote[self.info["jar"]] = b"different"
        with self.assertRaisesRegex(RuntimeError, "refusing to overwrite"):
            self.publish(client)
        self.assertNotIn("upload", client.calls)
        self.assertFalse(client.published)

    def test_unrelated_draft_assets_are_preserved_without_publication(self):
        client = self.client(exists=True)
        client.remote["unrelated.txt"] = b"keep this"
        with self.assertRaisesRegex(RuntimeError, "unrelated assets"):
            self.publish(client)
        self.assertEqual(client.remote, {"unrelated.txt": b"keep this"})

    def test_fork_or_wrong_tag_artifacts_are_rejected_before_any_api_call(self):
        for key, value in [("repository", "someone/fork"), ("tag", "v2.0.0"), ("versionName", "0.9.0")]:
            with self.subTest(key=key):
                original = copy.deepcopy(self.info)
                self.info[key] = value
                self.write_info()
                client = self.client()
                with self.assertRaises(ValueError):
                    self.publish(client)
                self.assertEqual(client.calls, [])
                self.info = original

    def test_local_tampering_wrong_commit_and_unexpected_files_are_rejected(self):
        client = self.client()
        with patch.dict(os.environ, {"GITHUB_SHA": "b" * 40}):
            with self.assertRaisesRegex(ValueError, "different source commit"):
                self.publish(client)
        extra = self.root / "private.txt"
        extra.write_text("must not publish")
        with self.assertRaisesRegex(ValueError, "unexpected attachments"):
            self.publish(client)
        extra.unlink()
        (self.root / self.info["jar"]).write_bytes(b"tampered")
        with self.assertRaisesRegex(ValueError, "SHA-256"):
            self.publish(client)
        self.assertEqual(client.calls, [])

    def test_asset_traversal_is_rejected(self):
        self.info["files"][0]["name"] = "../secret.jar"
        self.write_info()
        with self.assertRaisesRegex(ValueError, "filename"):
            self.publish(self.client())

    def test_graphql_finds_drafts_that_the_rest_tag_endpoint_cannot_find(self):
        client = release.GitHub(release.TRUSTED_REPOSITORY)
        reference = {"data": {"repository": {"release": {"databaseId": 9}}}}
        draft = {"id": 9, "draft": True, "assets": []}
        with patch.object(release.subprocess, "run", side_effect=[
            subprocess.CompletedProcess([], 0, json.dumps(reference), ""),
            subprocess.CompletedProcess([], 0, json.dumps(draft), ""),
        ]) as run:
            self.assertEqual(client.find("v1.0.0"), draft)
            self.assertEqual(run.call_args_list[0].args[0][1:3], ["api", "graphql"])
            self.assertEqual(run.call_args_list[1].args[0][-1], "repos/badnng/Olympus-26.3/releases/9")
            self.assertNotIn("/releases/tags/", str(run.call_args_list))

    def test_missing_release_and_github_errors_are_distinguished_without_logging_secrets(self):
        client = release.GitHub(release.TRUSTED_REPOSITORY)
        response = {"data": {"repository": {"release": None}}}
        with patch.object(release.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, json.dumps(response), "")):
            self.assertIsNone(client.find("v1.0.0"))
        with patch.object(release.subprocess, "run", return_value=subprocess.CompletedProcess([], 1, "", "token secret network error")):
            with self.assertRaises(RuntimeError) as error:
                client.find("v1.0.0")
            self.assertNotIn("secret", str(error.exception))

    def test_publication_uses_rest_legacy_latest_and_never_clobbers(self):
        client = release.GitHub(release.TRUSTED_REPOSITORY)
        with patch.object(client, "command") as command:
            client.publish(9, False)
            self.assertEqual(json.loads(command.call_args.kwargs["input_text"]), {"draft": False, "prerelease": False, "make_latest": "legacy"})
            client.upload("v1.0.0", [self.root / self.info["jar"]])
            self.assertNotIn("--clobber", command.call_args.args[0])
            client.publish(9, True)
            self.assertEqual(json.loads(command.call_args.kwargs["input_text"])["make_latest"], "false")


if __name__ == "__main__":
    unittest.main()
