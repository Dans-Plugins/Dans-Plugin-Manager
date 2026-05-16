# Dans Plugin Manager Commands

All commands use `/dpm` or `/danspluginmanager` as the base.

| Command | Description | Permission |
|---------|-------------|------------|
| `/dpm help` | View a list of commands. | `dpm.help` |
| `/dpm list` | List DPC plugins, showing which are installed (with version tag when known). | `dpm.list` |
| `/dpm get <plugin-name>` | Download a DPC plugin to the server. | `dpm.get` |
| `/dpm clean [--confirm]` | Preview duplicate plugin JARs, or delete them when `--confirm` is passed. | `dpm.clean` |
| `/dpm stats` | View plugin statistics. | `dpm.stats` |
| `/dpm update` | Update all installed managed plugins to their latest release. | `dpm.update` |
| `/dpm info <plugin-name>` | Show description, GitHub owner, repo, latest release tag, publish date, install status, and dependency status for a plugin. | `dpm.info` |
| `/dpm reload` | Reload `config.yml` and re-apply settings (e.g. `githubToken`). | `dpm.reload` |
| `/dpm remove <plugin-name> [--confirm]` | Preview removal of an installed plugin, or delete it when `--confirm` is passed. | `dpm.remove` |
