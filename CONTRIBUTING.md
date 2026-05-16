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

## Questions

Ask in the [Discord server](https://discord.gg/xXtuAQ2).
