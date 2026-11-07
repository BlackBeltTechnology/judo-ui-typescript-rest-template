# judo-ui-typescript-rest-template — module agent doctrine

## Module purpose

`judo-ui-typescript-rest-template` generates the *data access half* of a JUDO
frontend: given a JUDO UI model instance (`judo-meta-ui` — the `Application` and
the `ClassType`, `EnumerationType`, `RelationType`, `OperationType` and security
elements reachable from it), it emits the TypeScript client through which the
generated React application reaches the running JUDO backend over REST. Nothing
here renders UI; the React templates in `judo-ui-react-template` import what
this module produces, and the two run as phase 1 (this module) and phase 2 (the
UI) of the same generation pipeline.

The output is layered on purpose, three directories with one responsibility
each:

- `data-api/` — the type layer. Per-class interfaces, enums, operation input and
  output types, the `JudoIdentifiable` / `JudoStored` identity contracts,
  `QueryCustomizer` filtering + `OrderingType` + `Seek` paging types,
  serializers and query-customizer serializers, mask and mask-builder types that
  express *which* attributes and relations a call fetches, plus error, file and
  security types.
- `data-service/` — the intent layer: `AccessService`, one CRUD service
  interface per class type, and one navigation service interface per relation
  type. Pure interfaces, no transport.
- `data-axios/` — the transport layer: Axios implementations of every service
  interface, together with `AxiosProvider` / `JudoAxiosProvider` /
  `JudoAxiosService` that own the HTTP client, headers and interceptor wiring.

Mechanically each generator module is a Handlebars template set plus
`public static` `@TemplateHelper` methods, bound together by a
`ui-typescript-rest.yaml` registry whose SpEL `pathExpression` computes each
output path and whose optional `factoryExpression` yields an iterable so one
template renders once per model element. Every generator module ships as an OSGi
bundle so `judo-ui-generator-maven-plugin` can load it at generation time by
`mvn:` URI.

**Repository:** BlackBeltTechnology/judo-ui-typescript-rest-template ·
**Artifact:** `hu.blackbelt.judo.generator:judo-ui-typescript-rest`
(packaging `pom`, version `${revision}` = `1.0.0-SNAPSHOT`) ·
**License:** EPL-2.0 · **Java:** 21 · **Build:** Maven 3.9.4 (no wrapper).

## Reactor map

The root `pom.xml` declares its `<modules>` inside the `modules` profile, active
unless `-DskipModules=true` is passed. Build order is the declared order —
`commons` first because the three template modules all call into it.

<modules>
  <module>judo-ui-typescript-rest-commons</module>
  <module>judo-ui-typescript-rest-api</module>
  <module>judo-ui-typescript-rest-service</module>
  <module>judo-ui-typescript-rest-axios</module>
  <module>judo-ui-typescript-rest-itest</module>
</modules>

| Module | Packaging | What it contributes |
|---|---|---|
| `judo-ui-typescript-rest-commons` | `bundle` | The naming law shared by every layer, and nothing else — no templates. `UiCommonsHelper` derives the identifiers all three generators must agree on (`classDataName`, `serviceClassName`, `serviceRelationName`, XMI-ID access, string shaping), which is what keeps a service interface, its Axios implementation and the type it returns referring to the same symbol. Consumed downstream too: `judo-ui-react-template` depends on it directly. |
| `judo-ui-typescript-rest-api` | `bundle` | Generates `data-api/`. Templates for per-class interfaces, enums and operation types (`model/`), the request/response, filter, mask, header and serialization machinery (`rest/`), and the hand-shared runtime types every generated app gets verbatim (`common/`, plus `common/errors`, `common/files`, `common/operations`, `common/security`). Helpers: `UiGeneralHelper`, `StoredVariableHelper`. |
| `judo-ui-typescript-rest-service` | `bundle` | Generates `data-service/`: `AccessService`, one CRUD service per `ClassType` (`factoryExpression: #getClassTypes(#application)`), one navigation service per `RelationType`. Interfaces only — this is the seam that lets the UI be written against intent rather than HTTP. Helper: `UiServiceHelper`. |
| `judo-ui-typescript-rest-axios` | `bundle` | Generates `data-axios/`: an Axios implementation for every service interface the `service` module declared, plus the provider/service base classes that configure the HTTP client. Swapping transport means replacing this module, not the layers above it. Helper: `UiAxiosHelper`. |
| `judo-ui-typescript-rest-itest` | `pom` | Aggregator for the executable proof. Its single child `ActionGroupTest` generates the full TypeScript client from a real `.model` file, per actor, then runs Vitest against the generated sources — so a template edit is validated by compiling and executing its output, not by inspection. |

## Build commands

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

### Maven profiles

| Profile | Purpose |
|---|---|
| `modules` | Active by default — includes all child modules. Deactivate with `-DskipModules=true`. |
| `sign-artifacts` | GPG-sign artifacts for Maven Central publication. |
| `release-dummy` | Deploy to local `/tmp/` directory for testing. |
| `release-judong` | Deploy to JudoNG Nexus (`nexus.judo.technology`). |
| `release-central` | Deploy to Maven Central via OSSRH with nexus-staging. |
| `generate-github-asciidoc-diagrams` | Generate PNG diagrams from AsciiDoc (PlantUML). |
| `update-source-code-license` | Update EPL-2.0 license headers in source files. |

## Technology stack

- **Eclipse EMF** (`ecore-xmi` 2.2.3) — metamodel framework for JUDO UI models
- **`judo-meta-ui-model`** 1.1.0.20260202 — the UI metamodel (`ClassType`, `RelationType`, `OperationType`, …)
- **`judo-generator-commons`** 1.0.0.20251205 — generation framework (`StaticMethodValueResolver`, `@TemplateHelper`)
- **Handlebars** 4.4.0 — template engine producing TypeScript
- **Spring Expression Language** 5.0.0 — path/factory expressions in the template registries
- **Lombok** 1.18.34 — boilerplate reduction in the Java helpers
- **Maven 3.9.4** + Flatten plugin — CI-friendly `${revision}` versioning
- **JaCoCo** 0.8.12 · **SonarQube** 3.9.1 · **Apache Felix Bundle Plugin** (OSGi packaging)
- **Vitest** — TypeScript test runner for the integration tests
- **`frontend-maven-plugin`** — auto-installs Node.js 18.14.2 + pnpm 7.27.1 for the itest build

## Architecture pointers

- Each generator module carries its own `src/main/resources/ui-typescript-rest.yaml` template registry: every entry names a template, an output `pathExpression`, an optional `factoryExpression`, and the `templateContext` variables the template sees. Adding a generated file starts there.
- `factoryExpression` produces an iterable and the template renders once per element; an entry without one renders once per application.
- Output path expressions call the helpers by SpEL, e.g. `#classDataName(#self, "Serializer")` — which is why helper renames are breaking changes for every registry that references them.
- All four generator modules are OSGi bundles loaded at generation time by `judo-ui-generator-maven-plugin` via `mvn:` coordinates.
- Test logging is driven by the repo-root `logback-test.xml`, shared across modules. CI lives in `.github/workflows/` — `build.yml` (build, test, deploy to Nexus, tags and releases) and `release.yml` (manual release trigger creating master and develop PRs).
- [README.md](README.md) — usage examples, architecture overview with Mermaid diagrams.
- [CONTRIBUTING.md](CONTRIBUTING.md) — development setup, issue reporting, PR process.
- [.github/CIFLOW.md](.github/CIFLOW.md) — CI/CD flow and branching strategy with diagrams.
- [judo-ui-generator-maven-plugin docs](https://github.com/BlackBeltTechnology/judo-meta-ui/tree/develop/generator-maven-plugin) — plugin configuration reference.

## Development environment

**Required:** Java 21 JDK · Maven 3.9.4+.
**Optional (auto-installed by the itest build):** Node.js 18.14.2 · pnpm 7.27.1.

## Git workflow

- **Main branch:** `develop`
- **Versioning:** `${revision}` in `pom.xml`, resolved by `flatten-maven-plugin`. Develop builds append `date_commitId_branchName`; releases use clean semver.
- **Branch naming:** `feature/JNG-<number>_<summary>`, `bugfix/JNG-<number>_<summary>`, `hotfix/JNG-<number>_<summary>`
- **Commit rule:** every commit must include a JIRA ticket number (`JNG-xxx`)
- **CI/CD:** GitFlow with automated build, deploy, merge and release via GitHub Actions (see [CIFLOW.md](.github/CIFLOW.md))

## Scope guard — invariants an edit must not break

1. **This is a code generator, not a runtime application.** Editing a helper or a `.hbs` template changes the TypeScript emitted into every downstream project — weigh the change at that blast radius.
2. **Helper methods must be `public static`.** Handlebars reaches them through the `StaticMethodValueResolver` base class; anything else is invisible to the templates.
3. **`commons` owns naming.** The three layers only line up because they all derive identifiers from the same helpers. Do not re-implement a name locally.
4. **Never speculate about code you have not opened.** Read the referenced file before answering; ground every claim in it.
5. **Keep changes minimal and DRY.** Smallest edit that works; a pattern repeated across modules belongs in a shared helper.
6. **Implement test-first.** Write or update the failing test/expectation, then the minimal code that satisfies it.
7. **Check in before a major change.** Explain the plan and get it verified before large or structural edits, and summarise what changed after each step.

<!-- dox-doctrine -->
## Documentation Update Protocol (WRITE discipline)

Per-directory `AGENTS.md` files form a tree. Each directory `AGENTS.md` is the
per-file record for the files in that directory. This module-root `AGENTS.md`
holds doctrine + architecture pointers only — never a per-file index.

**Keep the root lean.** This file loads into every agent turn — every byte costs
tokens on every turn. A verbose root file buries the rules the model must follow
(signal dilution) and measurably degrades adherence; a lean file keeps doctrine
salient. Default assumption: your update does NOT belong in the root — route it
by the table below.

**Route every doc update by kind:**

| Kind of update | Goes in |
|---|---|
| New file in a directory, or its per-file detail / change history | Nearest directory `AGENTS.md`. Add a `` | `<basename>` | <purpose> | `` row, path-alphabetical. |
| Data flow, protocol, architecture rationale | `docs/architecture.md` or a `docs/<topic>.md` |
| End-user / developer setup | `README.md` |
| Cross-cutting rule every agent needs every turn (rare) | this module-root `AGENTS.md` |

**Read before editing (chain walk).** Before editing a file, read the nearest
`AGENTS.md` chain root→leaf so you know the file's recorded purpose, contracts,
and change history. Do not edit blind.

**Update after editing (closeout pass).** After changing a file, update its row
in the nearest directory `AGENTS.md`: find the file's row, update its purpose in
place; if absent, add it in path-alphabetical order. New directory → scaffold
its `AGENTS.md`. One row per file. The purpose carries a one-line summary, key
exported symbols, contracts/invariants, and `See change: <id>` history.

**Row style (caveman).** Short declarative fragments. Drop articles. Subject →
verb → object, present tense. One fact per row. Prefer concrete tokens (paths,
symbols, env vars) over prose. Keep identifiers verbatim.

**Size rule — split an over-large directory `AGENTS.md` file-based.** pi
auto-injects a directory `AGENTS.md` on every turn when cwd sits at/below it, so
an over-large directory `AGENTS.md` is not supported. Split it file-based: a row
exceeding the length threshold promotes to a per-file `<File>.AGENTS.md`
sidecar carrying that file's full detail (including every `See change:`). The
sidecar is pull-only — its name is not `AGENTS.md`, so pi never auto-injects it
— yet it stays search-indexed (`agents` doc_type). The directory `AGENTS.md`
keeps a one-line summary plus a `→ see `<File>.AGENTS.md`` pointer. Rows within
the threshold stay verbatim (lossless).

## Finding docs (READ discipline)

`kb_*` tools are faster and cheaper than raw search — they return a one-line
purpose + key exports per file, not raw bytes. **This fires on the ACTION, not
the intent** — before you `grep`/`rg` for a symbol, `cat`/read a file to learn
what it does, or chase an import, the kb call goes first. It fires **even
mid-task when you already know the file**; knowing the file does not exempt you.
When your reflex is the left column, run the right column instead:

| You're about to… | Do this FIRST instead |
|---|---|
| `grep -rn "SymbolName" src/` — find where a fn / type / const lives | `kb_search --doc-type agents "SymbolName"` — tree indexes key exports per file |
| `grep -rn "feature\|topic" src/` — how does X work / where's X handled | `kb_search "feature topic"` |
| `cat` / read a file just to learn its purpose before editing | `kb agents <path>` — one-line purpose + exports + change history |
| chase imports / callers across files | `kb_neighbors <path\|heading>` |
| read one doc section in full | `kb_get <path> <section>` |

**Fall-through (explicit):** if the kb call returns nothing relevant, `rg` /
source read is allowed — then add the missing directory `AGENTS.md` row per the
WRITE discipline. kb does NOT replace grep; it goes first.
