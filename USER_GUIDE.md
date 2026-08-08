# Dans Plugin Manager User Guide

## What is Dans Plugin Manager?

Dans Plugin Manager (DPM) is a Spigot plugin that lets server operators browse and download DPC plugins in-game or from the server console.

## Installation

1. Download the latest `DansPluginManager-<version>.jar` from the [Releases](https://github.com/Dans-Plugins/Dans-Plugin-Manager/releases) page.
2. Place the JAR in your server's `plugins/` folder.
3. Restart the server.

## Getting Started

1. Run `/dpm list` to see all DPC plugins. Green entries are installed (with version tag when known); grey entries are not yet installed. Pass `installed` or `available` to filter the list.
2. Run `/dpm info <plugin-name>` to see a plugin's description, GitHub owner, repository, release channel, latest build on that channel, publish date, install status, and any required or optional dependencies.
3. Run `/dpm get <plugin-name>` to download a plugin to your server's `plugins/` folder. The name must match the one shown by `/dpm list` (e.g. `medievalfactions`). Multiple names are accepted: `/dpm get plugin1 plugin2`. If a plugin is already on the latest version, the download is skipped. Missing hard dependencies that are registered DPC plugins are automatically included in the download; dependencies that are not registered DPC plugins produce a warning. Add `--experimental` to install main-branch builds instead of published releases — see [Release channels](#release-channels).
4. Run `/dpm update` to check every installed managed plugin against the latest build on the channel it is set to, and download any that are out of date. Pass one or more plugin names to update only those: `/dpm update medievalfactions`.  
5. Restart the server to activate downloaded or updated plugins.
6. Run `/dpm search <keyword>` to find plugins by name or description (e.g. `/dpm search faction`). Green results are installed; grey are not.
7. Run `/dpm clean` to preview duplicate plugin JARs (e.g. versioned copies left over from manual installs). Add `--confirm` to delete them.
8. Run `/dpm remove <plugin-name>` to preview which JAR would be deleted. Add `--confirm` to remove it and clear its stored version tag and release channel.

## Release channels

Every managed plugin tracks one of two channels.

| Channel | What you get | How it is published |
|---------|--------------|---------------------|
| **stable** (default) | The plugin's latest published GitHub release — the versions the maintainers have tagged and announced. | Manually, when maintainers cut a release. |
| **experimental** | A build of the plugin's `main` branch, refreshed every time a change is merged. | Automatically by the plugin repository's CI, as a rolling `dev` prerelease. |

**Experimental builds are unreleased, unreviewed code.** They have not been through a release check, they can contain half-finished work, and a broken build can stop your server from starting. Do not run them on a server you care about without a backup and a way to roll back.

Switching a plugin to experimental builds:

```
/dpm get medievalfactions --experimental
```

The choice is remembered per plugin. From then on, plain `/dpm get medievalfactions` and `/dpm update` both keep that plugin on experimental builds — you do not need to repeat the flag. Switch it back with:

```
/dpm get medievalfactions --stable
```

Notes:

- **Dependencies keep their own channel.** If an experimental plugin pulls in a dependency automatically, that dependency is installed from whatever channel *it* is set to. Putting a dependency on experimental builds takes its own `/dpm get <dependency> --experimental`.
- **Version identity.** Experimental builds all carry the same `dev` tag, so DPM records them as `dev-<commit>` (e.g. `dev-abc1234`) to tell one build from the next. That is what `/dpm list` and `/dpm info` show.
- **`/dpm list installed` marks them.** Plugins on experimental builds are shown with a trailing `[experimental]`.
- **Switching channels always re-downloads**, because the two channels never share a version identity.
- **Not every plugin publishes experimental builds.** If a repository has no rolling build, `/dpm get <plugin> --experimental` reports that no experimental build is published and leaves the plugin on its current channel.
- **Removing a plugin resets it to stable**, so a later reinstall does not silently return to experimental builds.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dpm.help` | `true` | View the help menu. |
| `dpm.list` | `true` | Browse DPC plugins: list all/installed/available and search by keyword. |
| `dpm.stats` | `true` | View plugin statistics. |
| `dpm.get` | `op` | Download one or more plugins to the server, and switch them between the stable and experimental channels. |
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

`experimentalReleaseTag` controls which GitHub release tag experimental builds are read from. It defaults to `dev` and only needs changing if the plugin repositories publish their rolling build under a different tag. Run `/dpm reload` to apply a change.

To receive Discord notifications when `/dpm update` completes or when a download fails, set `discordWebhook` in `config.yml` to a Discord webhook URL and run `/dpm reload`. Create a webhook in your Discord server under channel settings → Integrations → Webhooks. Leave the value empty to disable notifications.

## Support

Ask questions in the [Discord server](https://discord.gg/xXtuAQ2) or open a [GitHub issue](https://github.com/Dans-Plugins/Dans-Plugin-Manager/issues).
