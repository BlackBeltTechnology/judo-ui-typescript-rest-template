# Contributing to JUDO UI TypeScript REST Generator

## Development Environment

Make sure your environment has:

- **Java 21** JDK
- **Maven 3.9.4+**
- Node.js 18+ and pnpm 7+ (only needed if running integration tests manually — the Maven build installs these automatically)

For full environment requirements, see the parent project's [CONTRIBUTING guide](https://github.com/BlackBeltTechnology/judo-community/blob/develop/CONTRIBUTING.adoc).

## Code Structure

This project is a code generator composed of Maven modules. Each module contributes Java helper classes and Handlebars templates that together produce TypeScript REST client code. See [README.md](README.md) for the full architecture.

## Build Commands

```bash
# Run all tests
mvn clean test

# Full build (compile, test, package, install to local repo)
mvn clean install
```

## Submitting an Issue

Before filing, search the [issue tracker](https://github.com/BlackBeltTechnology/judo-ui-typescript-rest-template/issues) — your problem may already be reported.

To help us reproduce the issue, include:

- Output of `java -version` and `mvn -version`
- The relevant `pom.xml` or `.flattened-pom.xml`
- A minimal reproduction case that demonstrates the failure

File new issues via the [issue form](https://github.com/BlackBeltTechnology/judo-ui-typescript-rest-template/issues/new/choose).

## Submitting a Pull Request

This project follows [GitHub's standard forking model](https://guides.github.com/activities/forking/). Fork the repository and submit pull requests from your fork.
