# CLAUDE.md — Dans Plugin Manager

## Build and test

```
mvn test          # run unit tests
mvn clean package # build the JAR (output in target/)
```

Tests must pass before merging. GitHub Actions runs `mvn clean package` (which includes tests) on every push to `main` and every PR targeting `main`. Run `mvn test` locally before pushing to catch failures early.

## Documentation — the four sources of truth

Every command is described in four places. All four must be updated together whenever a command is added, removed, or has its syntax changed:

| File | What to update |
|------|----------------|
| `src/main/java/dansplugins/dpm/commands/HelpCommand.java` | The line(s) printed by `/dpm help` |
| `COMMANDS.md` | The command reference table |
| `USER_GUIDE.md` | The Getting Started steps and permissions table |
| `CHANGELOG.md` | The `[Unreleased]` Added/Changed/Removed entry |

**Permissions table** (`USER_GUIDE.md`) must reflect the permission string passed to `super()` in each command's constructor — that string is the canonical source. Whenever a new command is added, grep the `super()` call for the permission node and add it to the table.

**Before closing any PR**, do a pass over all four files and verify:
- Command syntax strings match (flags, argument order, optional vs required)
- Permission nodes match the constructor `super()` calls
- CHANGELOG has an entry for every change in the PR

This project had multiple rounds of doc-fix commits because these drifted. Catching drift before merge is much cheaper than fixing it after.

## Writing GitHub issues

Before posting, verify every claim against the source code:
- Method names and call sites — grep to confirm they exist and behave as described
- "X is not done / X does not exist" claims — read the file to confirm the absence
- Example output — trace through the code to confirm the example is realistic

This project had an issue posted claiming `/dpm update` caused duplicate GitHub API requests per repo. The code showed each plugin maps to a unique repo, so there are no duplicates within a single update run. The issue was corrected before anyone acted on the wrong description.

## Test style

Tests use **JUnit 5** (`org.junit.jupiter`). No mocking framework — use anonymous subclasses to fake dependencies (see `DownloadServiceTest` for examples).

Filesystem tests use `@TempDir Path tempDir` and construct a `PluginFolderService(tempDir.toString())` with the temp path.

**Naming**: `methodName_condition_expectedOutcome` — e.g. `isInstalled_returnsFalseWhenOnlyVersionedJarPresent`.

**Structure**: group tests under a `// -------------------------------------------------------------------------` section comment per method under test (see `PluginFolderServiceTest`).

**Coverage expectations**:
- Every new `public` method on a service class needs at least happy-path and failure-path tests.
- Command classes depend on Bukkit and cannot be unit tested here. Cover the underlying services instead.
- When a bug is fixed, add a regression test that would have caught it.

## Code patterns

**Avoid O(N×M) directory scans** — `PluginFolderService.isInstalled()` scans the directory on every call. Never call it inside a loop over plugin records; use `filterInstalled(records)` instead, which does one scan for any number of records. `RemoveCommand` was fixed this way; `UpdateCommand` still uses the loop pattern and is tracked in issue #37.

**Async pattern** — commands that touch the network must not block the main thread:
```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // network/disk work here
    int result = downloadService.downloadLatest(record);
    Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(...));
});
```
Results are always dispatched back to the main thread via `runTask` before sending messages.

**Destructive commands** use `--confirm` to preview before acting (see `CleanCommand`, `RemoveCommand`). The no-arg or no-flag path always previews; `--confirm` performs the action. This pattern works identically in-game and from the console and requires no server-side state.

## Dependency declarations

Plugin-to-plugin dependencies are declared on `ProjectRecord` via the builder:
- `hardDependencies` — plugins that must be present for this plugin to function (sourced from `depend:` in `plugin.yml`)
- `softDependencies` — optional integrations (sourced from `softdepend:` in `plugin.yml`)

If new DPC plugins are added to `ProjectRecordInitializer`, check their `plugin.yml` on GitHub for `depend` and `softdepend` fields and declare them. The declared dependency graph powers `/dpm info` and `/dpm get` warnings.
