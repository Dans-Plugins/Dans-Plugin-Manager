# Contributing

## Thank You

Thank you for your interest in contributing to Dans Plugin Manager! This guide will help you get started.

## Links

- [Website](https://dansplugins.com)
- [Discord](https://discord.gg/xXtuAQ2)

## Requirements

- A GitHub account
- Git
- Java 9+ and Maven

## Getting Started

1. [Sign up for GitHub](https://github.com/signup) if you don't have an account.
2. Fork the repository by clicking **Fork** at the top right of the repo page.
3. Clone your fork: `git clone https://github.com/<your-username>/Dans-Plugin-Manager.git`
4. Open the project in your IDE.
5. Build the plugin: `mvn clean package`

## Identifying What to Work On

Work items are tracked as [GitHub issues](https://github.com/Dans-Plugins/Dans-Plugin-Manager/issues).

## Making Changes

1. Make sure an issue exists for the work. If not, create one.
2. Create a branch off `main`: `git checkout -b <branch-name>`
3. Make and test your changes.
4. Commit: `git commit -m "Description of changes"`
5. Push: `git push origin <branch-name>`
6. Open a pull request against `main`, linking the related issue with `#<number>`.
7. Address review feedback.

## Testing

Run the unit test suite:

```
mvn test
```

Build and place the JAR into a local Spigot server's `plugins/` folder to test in-game:

```
mvn clean package
```

The built JAR is in `target/`.

### Local test server (via OMCSI)

A local Spigot test server is provided for manual testing of the plugin. It is a thin set of scripts that drive [OMCSI](https://github.com/dmccoystephenson/open-mc-server-infrastructure) — the same Minecraft server infrastructure the CI integration tests use — so the local dev environment matches CI exactly.

Prerequisites: Docker with Compose v2 (`docker compose`), Maven, Git, `curl`. Run `./test-integration.sh` to verify.

1. `cp sample.env .env` and edit if you want a custom operator UUID/name.
2. `./up.sh` — clones OMCSI to `../omcsi` (first run only; allow 10–15 min while Spigot is built from BuildTools), starts the stack, builds DPM, and hot-deploys it via OMCSI's plugin-deploy API.
3. `./dpm-cmd.sh "dpm list"` — sends a console command to the running server and prints the recent log lines. Lets you exercise `/dpm` commands without joining the server in a Minecraft client.
4. `./reload-plugin.sh` — rebuilds DPM and hot-redeploys to the running server.
5. `./down.sh` — stops the OMCSI stack.

You can also connect a Minecraft client to `localhost:25565` to test in-game. Set `OVERWRITE_EXISTING_SERVER=true` in `.env` to wipe the server's persistent data on the next `./up.sh`.

Anything OMCSI-specific (memory, MOTD, BlueMap, alerts, etc.) can be tuned in `../omcsi/.env` directly; see [OMCSI's `sample.env`](https://github.com/dmccoystephenson/open-mc-server-infrastructure/blob/main/sample.env) for the full list.

## Integration tests

End-to-end tests deploy DPM against a live Spigot server (via [OMCSI](https://github.com/dmccoystephenson/open-mc-server-infrastructure)) and assert real command output. See [`integration-test/README.md`](integration-test/README.md) for setup instructions and the list of required secrets for the CI workflow.

## Questions

Ask in the [Discord server](https://discord.gg/xXtuAQ2).
