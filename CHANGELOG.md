# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- Tab-completion for `/dpm` sub-commands (`help`, `list`, `get`, `clean`, `stats`, `update`, `info`) and plugin names for `/dpm get` and `/dpm info`
- Version tracking — last-downloaded release tag is persisted in `dpm-versions.properties`; `/dpm get` skips re-download when already on the latest version and reports the current tag
- `/dpm list` now shows installed plugins in green (with their version tag when known) and uninstalled plugins in grey
- `/dpm update` — checks every installed managed plugin against the latest GitHub release and downloads any that are out of date; prints a per-plugin result and a summary line
- `/dpm info <plugin-name>` — shows GitHub owner, repository, latest release tag, publish date, and install/update status for a single plugin without downloading anything
- `githubApiToken` config option — when set, adds a `Bearer` token to all GitHub API requests, raising the unauthenticated rate limit (60 req/hr) to 5 000 req/hr

## [0.4.0]

### Added
- `/dpm help`, `/dpm list`, `/dpm get`, `/dpm stats` commands
- In-game browsing and downloading of DPC plugins
- Dynamic GitHub release retrieval — `/dpm get` now fetches the latest release JAR automatically via the GitHub API instead of relying on hardcoded versioned URLs
- 9 additional public DPC plugin repos registered (Bluemap_MedievalFactions, Bookshelves-You-Can-Use, Dans-Set-Home, Democracy, FlyCommand, Herald, KDRTracker, Medieval-Cookery, MiniFactions)
- Informative "no published release yet" message when a plugin has no GitHub release
- `/dpm clean` command to remove duplicate plugin JARs from the plugins folder

### Changed
- Download runs asynchronously so it no longer blocks the main server thread
- Conflicting JARs (e.g. manually installed versioned copies) are automatically removed before a new version is downloaded
- Conquest-Recipes switched from Spigot direct-link to GitHub release retrieval

### Removed
- ChatHub (repo no longer exists)
