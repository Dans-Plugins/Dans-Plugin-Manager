# Dans Plugin Manager Configuration

A `config.yml` is generated in `plugins/DansPluginManager/` on first run.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `version` | String | *(plugin version)* | Plugin version. Do not edit manually. |
| `debugMode` | Boolean | `false` | Enables verbose debug logging to the console. |
| `githubApiToken` | String | `""` | Personal access token for the GitHub API. When set, raises the rate limit from 60 to 5 000 requests per hour. Generate one at GitHub → Settings → Developer settings → Personal access tokens (no scopes required for public repos). |
