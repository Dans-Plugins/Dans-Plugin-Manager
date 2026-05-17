# Dans Plugin Manager User Guide

## What is Dans Plugin Manager?

Dans Plugin Manager (DPM) is a Spigot plugin that lets server operators browse and download DPC plugins in-game or from the server console.

## Installation

1. Download the latest `DansPluginManager-<version>.jar` from the [Releases](https://github.com/Dans-Plugins/Dans-Plugin-Manager/releases) page.
2. Place the JAR in your server's `plugins/` folder.
3. Restart the server.

## Getting Started

1. Run `/dpm list` to see all DPC plugins. Green entries are installed (with version tag when known); grey entries are not yet installed. Pass `installed` or `available` to filter the list.
2. Run `/dpm info <plugin-name>` to see a plugin's description, GitHub owner, repository, latest release tag, publish date, install status, and any required or optional dependencies.
3. Run `/dpm get <plugin-name>` to download a plugin to your server's `plugins/` folder. The name must match the one shown by `/dpm list` (e.g. `medievalfactions`). Multiple names are accepted: `/dpm get plugin1 plugin2`. If a plugin is already on the latest version, the download is skipped. If required dependencies are not installed, a warning is shown before the download begins.
4. Run `/dpm update` to check every installed managed plugin against its latest GitHub release and download any that are out of date. Pass one or more plugin names to update only those: `/dpm update medievalfactions`.  
5. Restart the server to activate downloaded or updated plugins.
6. Run `/dpm clean` to preview duplicate plugin JARs (e.g. versioned copies left over from manual installs). Add `--confirm` to delete them.
7. Run `/dpm remove <plugin-name>` to preview which JAR would be deleted. Add `--confirm` to remove it and clear its stored version tag.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dpm.help` | `true` | View the help menu. |
| `dpm.list` | `true` | List DPC plugins, optionally filtered by `installed` or `available`. |
| `dpm.stats` | `true` | View plugin statistics. |
| `dpm.get` | `op` | Download one or more plugins to the server. |
| `dpm.clean` | `op` | Preview or remove duplicate plugin JARs. |
| `dpm.update` | `op` | Update all installed managed plugins, or specific ones by name. |
| `dpm.info` | `true` | View description, release info, install status, and dependencies for a plugin. |
| `dpm.reload` | `op` | Reload the DPM config. |
| `dpm.remove` | `op` | Preview or remove an installed managed plugin. |

## Configuration

DPM generates a `config.yml` in `plugins/DansPluginManager/` on first run. See [CONFIG.md](CONFIG.md) for all options.

The most useful option for operators running frequent updates is `githubToken`. GitHub limits unauthenticated API requests to 60 per hour. Setting a personal access token raises this to 5 000 per hour:

1. Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Generate a new token with no scopes selected (public repo read access is granted by default)
3. Add it to `config.yml`:
   ```yaml
   githubToken: "ghp_your_token_here"
   ```
4. Run `/dpm reload` to apply the change without restarting the server.

## Support

Ask questions in the [Discord server](https://discord.gg/xXtuAQ2) or open a [GitHub issue](https://github.com/Dans-Plugins/Dans-Plugin-Manager/issues).
