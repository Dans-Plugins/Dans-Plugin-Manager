# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- `/dpm reload` — reloads `config.yml` and re-applies live settings (e.g. `githubToken`) without a server restart
- `/dpm remove <plugin-name> [--confirm]` — previews the JAR to be deleted; pass `--confirm` to actually remove it and clear the stored version tag

### Changed
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
