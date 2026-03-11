# JUDO UI TypeScript REST Generator - Project Documentation

## Project Overview


**Repository:** BlackBeltTechnology/judo-ui-typescript-rest-template
**License:** Eclipse Public License 2.0 (EPL-2.0)
**Java Version:** 21
**Build System:** Maven 3.9.4 with Handlebars templating, Spring SpEL, and Eclipse EMF

1. Generates TypeScript REST API client code from JUDO UI metamodels (EMF `.model` files)
2. Produces three layered outputs: data interfaces (`data-api/`), service abstractions (`data-service/`), and Axios HTTP implementations (`data-axios/`)
3. Each module contributes Java helper classes (callable via SpEL) and Handlebars templates (`.hbs`) configured through `ui-typescript-rest.yaml`
4. Integration tested by generating code from an `ActionGroupTest` model and running Vitest against the output
5. Part of the BlackBelt JUDO framework ecosystem (`hu.blackbelt.judo.generator` group)

## Code Instructions

1. First think through the problem, read the codebase for relevant files.
2. Before you make any major changes, check in with me and I will verify the plan.
3. Please every step of the way just give me a high level explanation of what changes you made.
4. Make every task and code change you do as simple as possible. We want to avoid making any massive or complex changes. Every change should impact as little code as possible. Everything is about simplicity.
5. Maintain a documentation file that describes how the architecture of the app works inside and out.
6. Never speculate about code you have not opened. If the user references a specific file, you MUST read the file before answering. Make sure to investigate and read relevant files BEFORE answering questions about the codebase. Never make any claims about code before investigating unless you are certain of the correct answer - give grounded and hallucination-free answers.
7. For implementation use TDD (Test-Driven Development): write or update tests first to define the expected behaviour, verify they fail, then write the minimal implementation to make them pass.
8. Use DRY (Don't Repeat Yourself): extract reusable logic into separate classes, utilities, or components. If the same pattern appears in multiple places, refactor it into a shared helper.

## Directory Structure

```
judo-ui-typescript-rest-template/
├── judo-ui-typescript-rest-commons/    # Shared Java helpers (naming, string utils, EMF access)
├── judo-ui-typescript-rest-api/        # data-api/ generation (interfaces, serializers, masks)
│   ├── src/main/java/.../             #   UiGeneralHelper, StoredVariableHelper
│   └── src/main/resources/            #   .hbs templates + ui-typescript-rest.yaml
├── judo-ui-typescript-rest-service/    # data-service/ generation (service interfaces)
│   ├── src/main/java/.../             #   UiServiceHelper
│   └── src/main/resources/            #   .hbs templates + ui-typescript-rest.yaml
├── judo-ui-typescript-rest-axios/      # data-axios/ generation (Axios implementations)
│   ├── src/main/java/.../             #   UiAxiosHelper
│   └── src/main/resources/            #   .hbs templates + ui-typescript-rest.yaml
├── judo-ui-typescript-rest-itest/      # Integration tests (ActionGroupTest model)
│   └── ActionGroupTest/               #   Per-actor test projects
├── .github/                            # CI/CD workflows (GitFlow-based)
├── pom.xml                             # Root POM (module aggregation, dependency management)
└── AGENTS.md                           # This file
```

## Core Modules

### Generation Modules

| Module | Type | Purpose |
|--------|------|---------|
| `judo-ui-typescript-rest-commons` | bundle | Shared utilities: naming conventions (`classDataName`, `serviceClassName`, `serviceRelationName`), EMF XMI ID access, string helpers |
| `judo-ui-typescript-rest-api` | bundle | Generates `data-api/`: TypeScript interfaces, enums, serializers, query customizers, mask builders, filter types, security types |
| `judo-ui-typescript-rest-service` | bundle | Generates `data-service/`: `AccessService`, per-class CRUD services, per-relation navigation services |
| `judo-ui-typescript-rest-axios` | bundle | Generates `data-axios/`: Axios HTTP implementations of all service interfaces, provider configuration |

### Test Module

| Module | Type | Purpose |
|--------|------|---------|
| `judo-ui-typescript-rest-itest` | pom | Integration tests: generates TypeScript from `ActionGroupTest` model, runs Vitest |

## Technology Stack

### Core Technologies
- **Eclipse EMF** (ecore-xmi 2.2.3) — metamodel framework for JUDO UI models
- **Handlebars** (4.4.0) — template engine for TypeScript code generation
- **Spring Expression Language** (5.0.0) — expression evaluation in template configurations
- **judo-generator-commons** (1.0.0.20251205) — base generation framework (`StaticMethodValueResolver`, `@TemplateHelper`)
- **judo-meta-ui-model** (1.1.0.20260202) — JUDO UI metamodel (ClassType, RelationType, OperationType, etc.)
- **Lombok** (1.18.34) — boilerplate reduction in Java helpers

### Build & Quality
- **Maven 3.9.4** with Flatten plugin for CI-friendly `${revision}` versioning
- **JaCoCo** (0.8.12) — code coverage
- **SonarQube** (3.9.1) — static analysis
- **Apache Felix Bundle Plugin** — OSGi bundle packaging
- **Vitest** — TypeScript test runner (integration tests)
- **frontend-maven-plugin** — auto-installs Node.js 18.14.2 + pnpm 7.27.1 for itest

## Build Commands

```bash
# Full build (all modules)
mvn clean install

# Run tests only
mvn clean test

# Build specific module
mvn clean install -pl judo-ui-typescript-rest-api

# Integration tests only (generates code + runs Vitest)
mvn clean test -pl judo-ui-typescript-rest-itest

# Skip module compilation (root only)
mvn clean install -DskipModules=true
```

> **Note:** No `mvnw` wrapper is included. Use system Maven.

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `modules` | Active by default — includes all child modules. Deactivate with `-DskipModules=true` |
| `sign-artifacts` | GPG-sign artifacts for Maven Central publication |
| `release-dummy` | Deploy to local `/tmp/` directory for testing |
| `release-judong` | Deploy to JudoNG Nexus (`nexus.judo.technology`) |
| `release-central` | Deploy to Maven Central via OSSRH with nexus-staging |
| `generate-github-asciidoc-diagrams` | Generate PNG diagrams from AsciiDoc (PlantUML) |
| `update-source-code-license` | Update EPL-2.0 license headers in source files |

## Key Configuration Files

| File | Purpose |
|------|---------|
| `pom.xml` | Root POM: module aggregation, shared dependencies, profiles |
| `*/src/main/resources/ui-typescript-rest.yaml` | Template registry: maps `.hbs` templates to output paths and SpEL expressions |
| `logback-test.xml` | Test logging configuration (shared across modules) |
| `.github/workflows/build.yml` | Main CI: build, test, deploy to Nexus, create tags and releases |
| `.github/workflows/release.yml` | Manual release trigger: creates PRs for master and develop |

## Development Environment

**Required:**
- Java 21 JDK
- Maven 3.9.4+

**Optional (auto-installed by itest build):**
- Node.js 18.14.2
- pnpm 7.27.1

## Git Workflow

- **Main Branch:** `develop`
- **Versioning:** `${revision}` in pom.xml, resolved by flatten-maven-plugin. Develop builds append `date_commitId_branchName`; releases use clean semver.
- **Branch naming:** `feature/JNG-<number>_<summary>`, `bugfix/JNG-<number>_<summary>`, `hotfix/JNG-<number>_<summary>`
- **Commit rule:** Every commit must include a JIRA ticket number (`JNG-xxx`)
- **CI/CD:** GitFlow with automated build, deploy, merge, and release via GitHub Actions (see [CIFLOW.md](.github/CIFLOW.md))

## Important Notes

1. **This is a code generator, not a runtime application.** Changes to Java helpers or `.hbs` templates affect the TypeScript output of downstream projects.
2. **Helper methods must be `public static`** to be callable from Handlebars templates via the `StaticMethodValueResolver` base class.
3. **Template output paths are SpEL expressions** — they use helper methods like `#classDataName(#self, "Serializer")` to compute file names dynamically.
4. **`ui-typescript-rest.yaml` is the template registry** — each entry defines a template name, output path expression, optional factory expression (for per-element generation), and template context variables.
5. **The `factoryExpression`** in yaml entries produces an iterable — the template is rendered once per element. Templates without `factoryExpression` are rendered once per application.
6. **All modules produce OSGi bundles** — they are loaded by the `judo-ui-generator-maven-plugin` at generation time via Maven coordinates (`mvn:` URIs).
7. **The `commons` module has no templates** — it only provides shared Java helper methods used by the other three modules.

## Related Documentation

- [README.md](README.md) — Usage examples, architecture overview with Mermaid diagrams
- [CONTRIBUTING.md](CONTRIBUTING.md) — Development setup, issue reporting, PR process
- [.github/CIFLOW.md](.github/CIFLOW.md) — Detailed CI/CD flow and branching strategy with diagrams
- [judo-ui-generator-maven-plugin docs](https://github.com/BlackBeltTechnology/judo-meta-ui/tree/develop/generator-maven-plugin) — Plugin configuration reference
