# Repository Guidelines

## Project Structure & Module Organization

Olympus is a Minecraft 26.1.2 mod targeting NeoForge.

- `common/src/main/`: shared entry point, platform API, and resources.
- `neoforge/src/main/java/dev/xylonity/olympus/`: gameplay in `common/`, rendering in `client/`, plus `registry/`, `network/`, `config/`, and `mixins/`.
- `neoforge/src/main/resources/`: assets under `assets/olympus/`, gameplay data under `data/olympus/`, and loader metadata under `META-INF/`.
- `fabric/`: loader scaffold, currently disabled in `settings.gradle`.
- `buildSrc/src/main/groovy/`: shared Gradle conventions. Dependency versions and mod metadata live in `gradle.properties`.

## Build, Test, and Development Commands

Use Java 25 and the checked-in Gradle 9.2.0 wrapper. Run from the repository root; on Unix, replace `.\gradlew.bat` with `./gradlew`.

| Command | Purpose |
| --- | --- |
| `.\gradlew.bat build` | Compile enabled modules, run configured checks, and package JARs under each module's `build/libs/`. |
| `.\gradlew.bat check` | Run configured verification tasks. |
| `.\gradlew.bat :neoforge:runClient` | Launch the development client. |
| `.\gradlew.bat :neoforge:runServer` | Launch the dedicated development server. |
| `.\gradlew.bat :neoforge:runData` | Generate resources into `neoforge/src/generated/resources/`; review changes before committing. |

## Coding Style & Naming Conventions

Use four-space indentation and same-line opening braces. Follow existing `dev.xylonity.olympus` packages, `PascalCase` types, `lowerCamelCase` methods/fields, and `UPPER_SNAKE_CASE` constants. Resource names use `lower_snake_case`, such as `harpy_spawn_egg`. Keep client-only code in `client/`. No formatter or linter is configured; match nearby code and honor `.gitattributes` line endings.

## Testing Guidelines

No automated test suite, test framework, or coverage threshold is configured. Run `build`, then exercise changed items, mobs, networking, and world generation in-game. Check dedicated-server startup for server-facing changes and inspect runtime logs. Use disposable worlds for world-generation checks. If introducing unit tests, configure their framework and place `*Test.java` under the relevant module's `src/test/java/`, mirroring production packages.

## Commit & Pull Request Guidelines

History favors short, emoji-prefixed English summaries describing one change, such as `🐛 Fix marble column alignment`; no formal Conventional Commits scheme is established. Keep commits focused. PRs should explain behavior changes, link relevant issues, list validation performed, and include screenshots or clips for visual changes. Exclude build outputs and development worlds.

## Licensing & Agent Instructions

Read `LICENSE` and `ASSETS_LICENSE`; preserve attribution and respect separate asset restrictions. When adding CI, adapt the saved [reuse-github-ci skill](C:/Users/wsj31/.codex/skills/reuse-github-ci/SKILL.md) to this Gradle project. Preserve Telegram fallback: missing App API ID or Hash uses Bot API and skips local-service builds; attach files up to 50 MB and provide download links above that limit.
