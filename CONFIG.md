# Dans Plugin Manager Configuration

A `config.yml` is generated in `plugins/DansPluginManager/` on first run. The `usage-reporting` block also ships inside the jar, so a `config.yml` written by an older version that lacks it still reads the bundled values.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `version` | String | *(plugin version)* | Plugin version. Do not edit manually. |
| `debugMode` | Boolean | `false` | Enables verbose debug logging to the console. |
| `githubToken` | String | `""` | Personal access token for the GitHub API. When set, raises the rate limit from 60 to 5 000 requests per hour. Generate one at GitHub → Settings → Developer settings → Personal access tokens (no scopes required for public repos). |
| `experimentalReleaseTag` | String | `"dev"` | The GitHub release tag experimental (main-branch) builds are published under. DPM fetches `releases/tags/<this value>` for plugins on the experimental channel. Only change this if the plugin repositories publish their rolling build under a different tag. Applied on `/dpm reload`. |
| `discordWebhook` | String | `""` | Discord webhook URL. When set, DPM posts a summary to the channel after each `/dpm update` run and on any download failure from `/dpm get`. Leave empty to disable. Create one in your Discord server under channel settings → Integrations → Webhooks. |
| `usage-reporting.enabled` | Boolean | `true` | Whether the plugin reports usage events (see below). Set to `false` to turn it off. Takes effect on the next server restart. |
| `usage-reporting.endpoint` | String | `https://trace.danielstephenson.dev` | The trace server events are sent to. |
| `usage-reporting.key` | String | the plugin's key | Identifies this plugin to the trace server so reports are attributed to it. Not a secret: it ships in the bundled config and can only report as DansPluginManager. Empty means reporting is off regardless of `enabled`. |

## Usage reporting

When the plugin is enabled, and each time one of its commands is used, a small event is sent to the
author's [trace](https://github.com/Stephenson-Software/trace-client-java) server so it is known which
plugins are actually in use. An event carries the plugin's name, the event name (`startup` or
`command`), and either the plugin version or the command name — nothing about players, the world, or
the server. Sending happens off the main thread, never delays a tick, and is dropped silently if the
server cannot be reached. Set `usage-reporting.enabled` to `false` to turn it off; unlike the other
options, this one is read once at startup, so `/dpm reload` does not apply a change to it.
