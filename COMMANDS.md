# Dans Plugin Manager Commands

All commands use `/dpm` or `/danspluginmanager` as the base.

| Command | Description | Permission |
|---------|-------------|------------|
| `/dpm help` | View a list of commands. | `dpm.help` |
| `/dpm list [installed\|available]` | List DPC plugins. Pass `installed` or `available` to filter. | `dpm.list` |
| `/dpm get <plugin-name> [plugin-name ...] [--experimental\|--stable]` | Download one or more DPC plugins to the server. `--experimental` switches the named plugins to main-branch builds and keeps them there; `--stable` switches them back to published releases. See [Release channels](USER_GUIDE.md#release-channels). | `dpm.get` |
| `/dpm clean [--confirm]` | Preview duplicate plugin JARs, or delete them when `--confirm` is passed. | `dpm.clean` |
| `/dpm stats` | View plugin statistics. | `dpm.stats` |
| `/dpm update [plugin-name ...]` | Update all installed managed plugins to the latest build on the channel each one is set to. Pass one or more names to update only those plugins. Takes no channel flags — use `/dpm get` to switch channels. | `dpm.update` |
| `/dpm info <plugin-name>` | Show description, GitHub owner, repo, release channel, latest build on that channel, publish date, install status, and dependency status for a plugin. | `dpm.info` |
| `/dpm reload` | Reload `config.yml` and re-apply settings (e.g. `githubToken`). | `dpm.reload` |
| `/dpm remove <plugin-name> [--confirm]` | Preview removal of an installed plugin, or delete it when `--confirm` is passed. | `dpm.remove` |
| `/dpm search <keyword>` | Search registered plugins by name or description. Results show install status and version. | `dpm.list` |
