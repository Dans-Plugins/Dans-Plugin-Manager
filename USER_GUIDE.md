# Dans Plugin Manager User Guide

## What is Dans Plugin Manager?

Dans Plugin Manager (DPM) is a Spigot plugin that lets server operators browse and download DPC plugins in-game or from the server console.

## Installation

1. Download the latest `DansPluginManager-<version>.jar` from the [Releases](https://github.com/Dans-Plugins/Dans-Plugin-Manager/releases) page.
2. Place the JAR in your server's `plugins/` folder.
3. Restart the server.

## Getting Started

1. Run `/dpm list` to see all DPC plugins. Green entries are installed (with version tag when known); grey entries are not yet installed.
2. Run `/dpm info <plugin-name>` to see the GitHub owner, repository, latest release tag, publish date, and install status for a specific plugin before downloading.
3. Run `/dpm get <plugin-name>` to download a plugin to your server's `plugins/` folder. The name must match the one shown by `/dpm list` (e.g. `medievalfactions`). If the plugin is already on the latest version, the download is skipped.
4. Run `/dpm update` to check every installed managed plugin against its latest GitHub release and download any that are out of date.
5. Restart the server to activate downloaded or updated plugins.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dpm.help` | `true` | View the help menu. |
| `dpm.list` | `true` | List DPC plugins with installed/version status. |
| `dpm.stats` | `true` | View plugin statistics. |
| `dpm.get` | `op` | Download a plugin to the server. |
| `dpm.clean` | `op` | Remove duplicate plugin JARs. |
| `dpm.update` | `op` | Update all installed managed plugins. |
| `dpm.info` | `true` | View release and install info for a plugin. |

## Configuration

DPM generates a `config.yml` in `plugins/DansPluginManager/` on first run. See [CONFIG.md](CONFIG.md) for all options.

The most useful option for operators running frequent updates is `githubToken`. GitHub limits unauthenticated API requests to 60 per hour. Setting a personal access token raises this to 5 000 per hour:

1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate a new token with no scopes selected (public repo read access is granted by default)
3. Add it to `config.yml`:
   ```yaml
   githubToken: "ghp_your_token_here"
   ```
4. Restart the server (or use `/dpm reload` once that command is available).

## Support

Ask questions in the [Discord server](https://discord.gg/xXtuAQ2) or open a [GitHub issue](https://github.com/Dans-Plugins/Dans-Plugin-Manager/issues).
