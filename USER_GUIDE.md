# Dans Plugin Manager User Guide

## What is Dans Plugin Manager?

Dans Plugin Manager (DPM) is a Spigot plugin that lets server operators browse and download DPC plugins in-game or from the server console.

## Installation

1. Download the latest `DansPluginManager-<version>.jar` from the [Releases](https://github.com/Dans-Plugins/Dans-Plugin-Manager/releases) page.
2. Place the JAR in your server's `plugins/` folder.
3. Restart the server.

## Getting Started

1. Run `/dpm list` to see available plugins.
2. Run `/dpm get <plugin-name>` to download a plugin to your server's `plugins/` folder. The name must match the one shown by `/dpm list` (e.g. `medievalfactions`).
3. Restart the server to activate the downloaded plugin.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `dpm.help` | `true` | View the help menu. |
| `dpm.list` | `true` | List available plugins. |
| `dpm.stats` | `true` | View plugin statistics. |
| `dpm.get` | `op` | Download a plugin to the server. |

## Support

Ask questions in the [Discord server](https://discord.gg/xXtuAQ2) or open a [GitHub issue](https://github.com/Dans-Plugins/Dans-Plugin-Manager/issues).
