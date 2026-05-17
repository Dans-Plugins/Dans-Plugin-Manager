# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- `/dpm stats` now shows installed plugin count alongside the total registered count
- `/dpm reload` — reloads `config.yml` and re-applies live settings (e.g. `githubToken`) without a server restart
- `/dpm remove <plugin-name> [--confirm]` — previews the JAR to be deleted; pass `--confirm` to actually remove it and clear the stored version tag
- Tab-completion for `/dpm remove` offers installed plugin names at the first argument and `--confirm` at the second
- Tab-completion for `/dpm clean` offers `--confirm` at the first argument
- One-line descriptions added to all 28 registered plugins; shown by `/dpm info`
- Dependency declarations on `ProjectRecord` — hard (`depend`) and soft (`softdepend`) DPC-to-DPC relationships sourced from each plugin's `plugin.yml`
- `/dpm info` shows each dependency and whether it is currently installed
- `/dpm get` warns when a required hard dependency is not yet installed (download still proceeds)
- `/dpm get` accepts multiple plugin names: `/dpm get plugin1 plugin2 ...` downloads all named plugins sequentially with per-plugin result lines and a summary
- `/dpm list installed` shows only plugins whose JAR is present in the plugins folder
- `/dpm list available` shows only plugins that are not currently installed
- Tab-completion for `/dpm get` offers plugin names at every argument position
- Tab-completion for `/dpm list` offers `installed` and `available`

### Changed
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
