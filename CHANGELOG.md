# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [0.7.0-SNAPSHOT-8-8-2026] – 2026-08-08

### Changed
- Dans-Plugin-Manager is now developed AI-first. Day-to-day feature work, grooming, review and maintenance run through AI agents working directly against this repository, with the maintainers setting direction and approving what lands. The version bump marks that change in how the project is built — it is not a break in behaviour, configuration or stored data, and existing installations can upgrade in place. Released as `0.7.0-SNAPSHOT-8-8-2026`: the AI-first line has not yet been verified in live operation, and the dated snapshot designation stays until it has.

### Added
- Local test server scripts (`up.sh`, `down.sh`, `reload-plugin.sh`, `dpm-cmd.sh`, `test-integration.sh`) that drive [OMCSI](https://github.com/dmccoystephenson/open-mc-server-infrastructure) for manual plugin testing during development — same Spigot stack and plugin-deploy API used by the CI integration tests
- `sample.env` documenting the env vars used by the local test server scripts

### Changed
- Reclassified `PluginFolderService` as `PluginFileRepository` and `GitHubReleaseService` as `GitHubReleaseRepository`, moving both into the `repositories/` package — both are data-access classes (filesystem scanning, GitHub API calls), not business logic, per the layering proposed in #94. No behavior change.
- Split `ConfigService` into `ConfigRepository` (`repositories/` package — plain config I/O) and `ConfigController` (`controllers/` package — option validation, `CommandSender` messaging, the `hasBeenAltered` flag), per the layering proposed in #94. `DansPluginManager` and `DiscordNotificationService` now depend on `ConfigRepository`. No behavior change.
- Introduced `GetController` (`controllers/` package), which now owns `/dpm get`'s dependency resolution and download-loop orchestration, returning plain per-plugin result data (`GetController.PluginResult`) instead of calling `sender.sendMessage()` directly. `GetCommand` is now a thin router that formats and sends the results; the async `runTaskAsynchronously`/`runTask` dispatch pattern is unchanged. This is the template for the remaining controllers proposed in #94. No user-visible behavior change.
- Introduced `UpdateController` (`controllers/` package), which now owns `/dpm update`'s installed-plugin scan, selective-update validation, download loop, and Discord notification, returning plain per-plugin result data (`UpdateController.PluginResult`) instead of calling `sender.sendMessage()` directly. `UpdateCommand` is now a thin router that formats and sends the results; the async dispatch pattern is unchanged. No user-visible behavior change (#108).
- Introduced `RemoveController` (`controllers/` package), which now owns `/dpm remove`'s dependent lookup, file deletion, and version-store cleanup, returning plain result data (`RemoveController.RemovalPreview`/`RemovalResult`) instead of calling `sender.sendMessage()` directly. `RemoveCommand` is now a thin router that formats and sends the results. No user-visible behavior change (#109).
- Introduced `StatsController` (`controllers/` package), which now owns `/dpm stats`'s registered/installed/available counting, returning plain result data (`StatsController.Stats`) instead of calling `sender.sendMessage()` directly. `StatsCommand` is now a thin router that formats and sends the results. No user-visible behavior change (#114).
- Introduced `SearchController` (`controllers/` package), which now owns `/dpm search`'s keyword filtering and installed/version lookup, returning plain result data (`SearchController.SearchResult`) instead of calling `sender.sendMessage()` directly. `SearchCommand` is now a thin router that formats and sends the results. No user-visible behavior change (#111).

## [0.6.0] - 2026-05-18

### Added
- `discordWebhook` config key — when set, DPM posts a summary notification to the specified Discord channel after each `/dpm update` run (with per-plugin version diffs) and on any download failure from `/dpm get` (#83)
- `DiscordNotificationService` — sends optional Discord webhook notifications; exceptions are silently swallowed so a failed webhook never affects the command result (#83)
- Single retry with a 2-second delay on transient network failures: `GitHubReleaseService.doFetch()` retries once on `IOException` or 5xx responses; `DownloadService.openNetworkStream()` retries once on `IOException`; 4xx responses are not retried (#85)
- `Logger.warn()` — always prints to the console regardless of `debugMode`, used for operator-visible error messages (#80)
- Server-console audit trail: `plugin.getLogger().info()` is emitted on success and `.warning()` on failure for `/dpm get`, `/dpm update`, `/dpm remove`, and `/dpm clean --confirm` (#81, #87)

### Fixed
- `VersionStore` now uses the injected `dansplugins.dpm.utils.Logger` instead of a static JUL logger; load and save failures emit a clearly prefixed warning that always appears in the server console (#82)
- GitHub rate-limit responses (HTTP 429 and HTTP 403 with `X-RateLimit-Remaining: 0`) now produce a specific warning advising to configure a `githubToken`; previously they were logged as generic API errors (#79)
- GitHub API authentication failures (HTTP 401) now produce a specific warning identifying the token as the cause; previously folded into the generic non-200 path (#88)
- Network errors, JAR download failures, and missing `.jar` releases now use `Logger.warn()` so they always appear in the server console, even when `debugMode` is off (#80)
- JAR download stream now sets a 10 s connect timeout and 30 s read timeout; previously `URL.openStream()` had no timeout, so a hung CDN would stall the async task indefinitely
- Conflicting JARs (e.g. `Plugin-1.0.0.jar`) are now removed only after a successful download; previously they were deleted before the network call, silently uninstalling the plugin on any download failure
- JAR write buffer increased from 1 KiB to 8 KiB, reducing I/O cycles for large plugin files

### Removed
- Multi-line Javadoc blocks from `HelpCommand`, `DansPluginManager`, `Logger`, and `TabCompleter` (CLAUDE.md violation)

### Changed
- `/dpm stats` now shows available plugin count (registered minus installed) as a third stat line
- `/dpm list available` now appends each plugin's description after its name, separated by an em-dash
- `/dpm remove <plugin-name> --confirm` now prints a reinstall hint (`/dpm get <name>`) after the removal success message
- `/dpm update` success message now shows the old and new version tag (e.g. `v4.5.0 → v4.6.3`) when a plugin was previously tagged; shows only the new tag if no prior tag was stored
- All "Plugin not found" errors across `/dpm get`, `/dpm update`, `/dpm info`, and `/dpm remove` now append `Use /dpm search <keyword> to find the right name.`
- `/dpm remove` preview and confirm paths now warn when other installed plugins declare a hard dependency on the plugin being removed
- Download failures now distinguish between network errors (GitHub unreachable) and file write errors (plugins folder not writable), surfacing a specific hint in each case

## [0.5.0] - 2026-05-17

### Added
- Docker-based integration test CI (`integration-test/test_dpm.py` + `.github/workflows/integration.yml`) that spins up an OMCSI Spigot stack, deploys DPM, and asserts `dpm list`, `dpm get`, and `dpm search` produce the expected output; runs on workflow dispatch and nightly schedule
- `/dpm get` now auto-downloads missing hard dependencies that are registered DPC plugins before downloading the requested plugin(s); transitive dependencies are resolved recursively and circular chains are handled safely. Dependencies that are not registered DPC plugins still produce a warning.
- `/dpm update [plugin-name ...]` — when plugin names are provided, only those plugins are updated; tab-completion offers installed plugin names at every argument position
- `/dpm search <keyword>` — searches registered plugin names and descriptions (case-insensitive substring match); results are colour-coded by install status
- `/dpm stats` now shows installed plugin count alongside the total registered count
- `/dpm reload` — reloads `config.yml` and re-applies live settings (e.g. `githubToken`) without a server restart
- `/dpm remove <plugin-name> [--confirm]` — previews the JAR to be deleted; pass `--confirm` to actually remove it and clear the stored version tag
- Tab-completion for `/dpm remove` offers installed plugin names at the first argument and `--confirm` at the second
- Tab-completion for `/dpm clean` offers `--confirm` at the first argument
- One-line descriptions added to all 28 registered plugins; shown by `/dpm info`
- Dependency declarations on `ProjectRecord` — hard (`depend`) and soft (`softdepend`) DPC-to-DPC relationships sourced from each plugin's `plugin.yml`
- `/dpm info` shows each dependency and whether it is currently installed
- `/dpm get` accepts multiple plugin names: `/dpm get plugin1 plugin2 ...` downloads all named plugins sequentially with per-plugin result lines and a summary
- `/dpm list installed` shows only plugins whose JAR is present in the plugins folder
- `/dpm list available` shows only plugins that are not currently installed
- Tab-completion for `/dpm get` offers plugin names at every argument position
- Tab-completion for `/dpm list` offers `installed` and `available`

### Changed
- `PluginFolderService`: added `findAllConflictingJars(List<ProjectRecord>)` that does a single directory scan for any number of records; `CleanCommand` (preview and `--confirm`) now uses this instead of calling `findConflictingJars()` in a loop, reducing N scans to 1
- `DownloadService`: added `downloadLatest(ProjectRecord, boolean physicallyInstalled)` overload; `UpdateCommand.runUpdates()` passes `true` since records are pre-confirmed installed via `filterInstalled()`, eliminating one `isInstalled()` scan per plugin per update run
- `GitHubReleaseService`: extracted `parseStringField` to eliminate duplicated JSON-parsing logic shared by `parseTagName` and `parsePublishedAt`
- Removed remaining multi-line Javadoc blocks from `GitHubReleaseService`, `DownloadService`, and `StatsCommand`; kept non-obvious algorithm notes as single `//` lines
- `UpdateCommand` selective path replaced per-plugin `isInstalled()` calls with a single `filterInstalled()` scan
- `USER_GUIDE.md` `dpm.list` permission description now covers `/dpm search` (both commands share this node)
- Removed multi-line Javadoc blocks from `PluginFolderService`, `VersionStore`, `CleanCommand`, and `DefaultCommand` per CLAUDE.md style rule
- `ListCommand` (show-all path) and `InfoCommand` (dependency display) replaced O(N×M) `isInstalled()` calls with a single `filterInstalled()` scan
- `DansPluginManager.onTabComplete` — extracted `allPluginNames()` helper to remove duplicated plugin-name list building for `get` and `info` tab-completion branches
- `UpdateCommand` replaced O(N×M) per-record `isInstalled()` loop with a single `filterInstalled()` call
- `/dpm update` now emits a per-plugin result line for every outcome including already-up-to-date and no-release, consistent with `/dpm get` batch mode
- `GitHubReleaseService` caches release responses for the session; repeated calls for the same repo make only one HTTP request. `/dpm reload` clears the cache
- `/dpm clean` now previews what would be deleted; pass `--confirm` to actually remove the files
- `/dpm clean --confirm` now distinguishes deletion failures (permission errors) from "no duplicates found"

## [0.4.0]

### Added
- `/dpm help`, `/dpm list`, `/dpm get`, `/dpm stats`, `/dpm clean`, `/dpm update`, `/dpm info` commands
- In-game browsing and downloading of DPC plugins
- Dynamic GitHub release retrieval — `/dpm get` fetches the latest release JAR automatically via the GitHub API instead of relying on hardcoded versioned URLs
- 9 additional public DPC plugin repos registered (Bluemap_MedievalFactions, Bookshelves-You-Can-Use, Dans-Set-Home, Democracy, FlyCommand, Herald, KDRTracker, Medieval-Cookery, MiniFactions)
- Informative "no published release yet" message when a plugin has no GitHub release
- Tab-completion for `/dpm` sub-commands and plugin names for `/dpm get` and `/dpm info`
- Version tracking — last-downloaded release tag is persisted in `dpm-versions.properties`; `/dpm get` and `/dpm update` skip re-download when already on the latest version
- `/dpm list` shows installed plugins in green (with version tag when known) and uninstalled plugins in grey
- `/dpm update` checks every installed managed plugin against the latest GitHub release and downloads any that are out of date
- `/dpm info <plugin-name>` shows GitHub owner, repository, latest release tag, publish date, and install/update status without downloading anything
- `githubToken` config option — when set, adds a `Bearer` token to all GitHub API requests, raising the unauthenticated rate limit (60 req/hr) to 5 000 req/hr

### Changed
- Download runs asynchronously so it no longer blocks the main server thread
- Conflicting JARs (e.g. manually installed versioned copies) are automatically removed before a new version is downloaded
- Conquest-Recipes switched from Spigot direct-link to GitHub release retrieval

### Removed
- ChatHub (repo no longer exists)
