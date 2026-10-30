<p><img src="https://i.imgur.com/ibQFISb.png" width="1124" height="258"></p>
<p style="text-align: center;"><a href="https://discord.gg/Pxj3gdhGj8" target="_blank" rel="nofollow noopener"><img src="https://media.forgecdn.net/attachments/description/1018043/description_a255b953-62ec-43d7-b4b3-8a1a94b36bac.png" alt=""></a> &nbsp;<a href="https://www.curseforge.com/minecraft/mc-mods/curios" target="_blank" rel="nofollow noopener"><img src="https://media.forgecdn.net/attachments/description/1667111/description_07ca6e33-5cf2-473a-a519-d95733922700.png" width="190" height="64"></a> &nbsp;<a href="https://www.curseforge.com/minecraft/mc-mods/geckolib" target="_blank" rel="nofollow noopener"><img src="https://media.forgecdn.net/attachments/description/1018043/description_1b32b3dd-8aa3-4fa4-b6f4-d7c186884e91.png" alt=""></a> &nbsp;<a href="https://www.curseforge.com/minecraft/mc-mods/olympus-mythology" target="_blank" rel="nofollow noopener"><img src="https://media.forgecdn.net/attachments/description/1018043/description_d108d080-9787-40f4-9eab-b5a715e7826b.png" alt=""></a> &nbsp;</p>
<p>&nbsp;</p>
<p><strong>Olympus!</strong> is a mod that adds artifacts from the Greek mythological pantheon to the game, each with its own unique abilities and properties. Each artifact you acquire during your adventures, grants you new abilities in combat, quality of life, and farming. The mod also provides new decorative blocks, mobs for battle, and themed structures.<br><br><em>The mod requires <a href="Curios API" target="_blank" rel="nofollow noopener">Curios API</a> and <a href="https://www.curseforge.com/minecraft/mc-mods/geckolib" target="_blank" rel="nofollow noopener">GeckoLib</a> (for Minecraft 26.3) to work.</em><br><br>This mod is <strong>highly configurable</strong> through the config file. For any questions, problems, contributions, new ideas about content/mods, and more, you can join our <a href="https://www.curseforge.com/linkout?remoteUrl=https://discord.com/invite/Pxj3gdhGj8" target="_blank" rel="nofollow noopener">official Discord</a> and comment in the appropriate channels.<br><br></p>

## NeoForge 26.3 port

This port preserves Olympus by **Xylonity and mongoose_artist**, its original gameplay values, and the separate source and asset licenses. It targets Minecraft **26.3** and requires NeoForge **26.3.0.92 or newer**, Java **25**, Curios **17.0.0-beta.3+26.3 or newer for 26.3**, and GeckoLib **5.5.7 or newer for 26.3**. The disabled Fabric scaffold is not part of this port.

The migration adapts Renderpearl pipelines and reversed depth, model submission and enchanted Curios rendering, entity interpolation, damage cooldowns, item attack animations, bone meal APIs, biome sampling, world generation data, recipe unlock criteria, and shader interfaces. Existing textures, models, artifact values, and original attribution are retained.

NeoForge 26.3 uses synced configs in place of the old server config type. The file remains named `olympus-server.toml`. When bringing over customized world settings, copy the old world's `serverconfig/olympus-server.toml` to `syncedconfig/olympus-server.toml` in that world; the global fallback is `config/olympus-server.toml`.

### Build and automated verification

Run from this repository with Java 25:

```powershell
.\gradlew.bat build
.\gradlew.bat :neoforge:runGameTestServer
.\gradlew.bat :neoforge:runClientTest
.\gradlew.bat :neoforge:verifyMigration
```

`build` and `check` include the dedicated GameTest suite. `runClientTest` first runs those tests, copies their generated world, opens it through Quick Play, hides its SDL window from the render thread, performs the client checks, and exits. `verifyMigration` combines the build and both suites. No computer-use tools, desktop input, existing saves, or real player accounts are needed. Client verification requires a graphics device and a desktop session.

The test mod lives in `neoforge/src/gameTest/` and is excluded from release JARs. Disposable worlds and logs live under `neoforge/runs/gametest/` and `neoforge/runs/client-test/`; do not store personal worlds there. GameTest replaces its own generated world on each run.

Reports:

- `neoforge/build/reports/gametest/results.xml`: 51 server behavior regression cases.
- `neoforge/build/reports/gametest/client-results.json`: model, render, effect, and network checks from the real client.
- `neoforge/runs/client-test/logs/latest.log`: client resource loading and runtime diagnostics.
- `neoforge/build/libs/olympus-neoforge-26.3-1.0.0.jar`: release artifact.

Server cases cover registered content, normal and elite harpy attributes and AI, Curios slot rules, cup charges and persistence, lethal damage protection and priority, Hades targeting, stun movement, Hermes jump limits, repairs and cooldowns, animal breeding, lightning chains, Ares charging/landing/throwing, Poseidon impact/splash/throwing, Artemis arrows and pet healing, crop growth, climbing roses, clouds, chest unlocking, trial waves and rewards, structure resources, and all payload codecs. The client resolves every item in GUI, first person, and third person contexts, cycles held items, and exercises entity renderers, Curios equipment, custom particles, post effects, and network handlers.

These checks validate the listed behavior and runtime paths. They do not prove exhaustive equivalence for every possible world, random AI decision, shader pack, multiplayer setup, or future NeoForge version, and do not replace a visual comparison against the original release.

## GitHub Actions

The saved personal CI template is adapted for this Java/NeoForge project. It uses Java 25 and the checked-in Gradle wrapper, with no Android SDK, APK signing, watch packages, or app update indexes.

- [CI](.github/workflows/ci.yml) runs on branch pushes, pull requests, and manual dispatch. Dependabot branch pushes are excluded because its PR receives the checks. Every ordinary push keeps its run; only an older run of the same PR is cancelled.
- The [shared build action](.github/actions/build-neoforge/action.yml) runs the offline distribution tests and `build :neoforge:verifyMigration`. Linux clients use Xvfb, Mesa software graphics, and a dummy audio driver. Gradle uses its official cache action with the basic GitHub cache provider and a cache directory outside the checkout. Only this repository's trusted `main` builds can write that cache.
- Build artifacts contain the production JAR, available source/Javadoc JARs, `SHA256SUMS`, and `build-info.json`. The primary JAR's embedded mod, Minecraft, NeoForge, and version metadata are verified; the test mod cannot enter the distribution. Artifacts and runtime diagnostics are retained for 14 days. Worlds are excluded.
- [Tag releases](.github/workflows/release.yml) require a `v*` tag matching `v{version}` in `gradle.properties` and a commit in `main` history. For the current version, that is `v1.0.0`. The same shared build produces the exact artifact downloaded by the publication job; only that job receives repository write permission. Assets are uploaded to a draft and downloaded again to verify their SHA-256 before publication. Published tags and conflicting draft assets are never overwritten. A failed publication job can resume identical partial draft assets. GitHub selects the latest stable release using its legacy version ordering.

The trusted repository is explicitly `badnng/Olympus-26.3`, which is itself a fork of the original Olympus project. Its identifier, rather than GitHub's general fork flag, controls distribution. Other forks and PR jobs receive no Telegram credentials. GitHub may require enabling Actions when first using these workflows in a fork. Commit and push the configuration before expecting remote runs; local checks do not mean a hosted workflow has run successfully.

### Optional Telegram delivery

Set these in the target repository's **Settings → Secrets and variables → Actions**:

| Type | Name | Purpose |
| --- | --- | --- |
| Variable | `TELEGRAM_ENABLED` | Set to `true` to enable delivery; otherwise it is skipped. |
| Secret | `TELEGRAM_BOT_TOKEN` | Required when delivery is enabled. |
| Secret | `TELEGRAM_CHAT_ID` | Required: numeric chat ID, `@public_channel`, or an ordinary public `t.me/channel` link. Invite links and display names are rejected. |
| Secret | `TELEGRAM_API_ID` | Optional application API ID. |
| Secret | `TELEGRAM_API_HASH` | Optional application API hash. |

Only non-PR `main` builds of the trusted repository notify Telegram. The notification job downloads the build job's original artifact; it never builds another copy. It sends the primary JAR with the saved HTML message layout and the two buttons **下载 Actions 产物** and **构建详情**.

If either application API credential is missing or blank, the job uses the ordinary Bot API and skips local server cache, compilation, startup, and logout. JARs up to **50,000,000 bytes** are attached; larger files produce a clear explanation and the Actions download link, which requires GitHub login. If both credentials are present and valid, the runner temporarily builds or restores the official pinned Local Bot API executable and can attach up to **2,000,000,000 bytes**. Only the executable is cached; sessions are temporary, logged out, and removed. Invalid complete credentials or failed sends remain errors; sends are not automatically retried after a timeout.

Notifications queue serially within this repository. GitHub concurrency groups do not coordinate different repositories: use a separate bot or arrange cross-repository coordination when sharing a bot. Switching from local back to the ordinary API may require Telegram's documented ten-minute cooldown.

### CI maintenance and offline checks

Action references use full commit SHAs. Dependabot checks weekly and groups their updates. The Gradle distribution has its official SHA-256 pinned. The optional Bot API server source is pinned separately in the CI workflow.

```powershell
python -B -m unittest discover -s .github/scripts -p 'test_*.py'
python -B .github/scripts/build_metadata.py check-tag --project-root . --tag v1.0.0
```

These Python tests use temporary files and mocked HTTP/GitHub responses; they do not send notifications or publish releases. The current actionlint 1.7.12 schema does not yet know `concurrency.queue`; if using that version, ignore only its unknown `queue` key diagnostic after checking that the value remains `max`. Other syntax and expression diagnostics must still fail validation.
