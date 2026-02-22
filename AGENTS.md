# AGENTS.md

This file provides guidance to LLM model when working with code in this repository.

## What This Project Does

A code generator that produces a complete TypeScript REST client layer (models, services, Axios implementations) from a JUDO UI metamodel. Uses **Handlebars templates** (`.hbs`) + **Java helper classes** annotated with `@TemplateHelper`. The output is a typed data layer with models, query customizers, serializers, mask builders, service interfaces, and Axios-based HTTP implementations.

## Build Commands

```bash
# Full build (compile + test + install)
./mvnw clean install

# Build without submodules (parent POM only)
./mvnw clean install -DskipModules=true

# Build with CI version
./mvnw -B -Drevision=1.0.0.20260220_XXXXXX_hash_develop clean install
```

### Integration Tests Only

The itest module generates TypeScript from a test model, then runs Node.js-based tests (Vitest) and a TypeScript compilation check:

```bash
# Run from the itest submodule
cd judo-ui-typescript-rest-itest/ActionGroupTest/action_group_test__god
../../mvnw clean verify
```

The itest flow: Maven generates TypeScript via `judo-ui-generator-maven-plugin` → installs Node 18.14.2 + pnpm 7.27.1 via `frontend-maven-plugin` → runs `pnpm install` → `pnpm run format` → `pnpm test` → `pnpm run build:ci`.

## Architecture

### Module Dependency Chain

```
commons  ←  api      (model types, enums, query customizers, serializers, masks)
commons  ←  service  (service interfaces)
             axios   (Axios implementations of service interfaces)
```

All template modules are **OSGi bundles** (not plain JARs), loaded dynamically by the generator framework.

### Code Generation Flow

1. `judo-ui-generator-maven-plugin` reads a `.model` file (EMF/Ecore UI metamodel)
2. Loads template bundles via Maven URIs (`mvn:hu.blackbelt.judo.generator:judo-ui-typescript-rest-api:${revision}`)
3. Scans each bundle for `ui-typescript-rest.yaml` descriptors in `src/main/resources/`
4. For each template entry, evaluates `factoryExpression` (SpEL) to get iterable elements
5. For each element, evaluates `pathExpression` to determine output file path
6. Handlebars processes the `.hbs` template; Java `@TemplateHelper` static methods are available as helpers
7. Output written to destination directory

### Template Registration (YAML Descriptors)

Each module has a `ui-typescript-rest.yaml` in `src/main/resources/` with entries like:

- **`templateName`** — path to `.hbs` file
- **`factoryExpression`** — SpEL returning an iterable (e.g., `#getClassTypes(#application)` runs once per class type)
- **`pathExpression`** — SpEL determining output path (e.g., `'model/' + #classDataName(#self, '') + '.ts'`)
- **`applicationBased`** — if true, template receives the application context
- **`templateContext`** — additional variables injected into template scope

The `type` field in the generator plugin config must be `ui-typescript-rest` — this matches the YAML file names.

### Java Helpers (5 files total)

All are annotated `@TemplateHelper` and extend `StaticMethodValueResolver`. Their static methods become Handlebars helpers:

| Class | Module | Responsibility |
|-------|--------|---------------|
| `UiCommonsHelper` | commons | Core naming: `classDataName`, `serviceClassName`, `serviceRelationName`, `firstToUpper/Lower` |
| `UiGeneralHelper` | api | Main workhorse: type mapping, import tokens, serialization logic, model traversal, mask builders |
| `StoredVariableHelper` | api | ThreadLocal context accessor for template parameters (e.g., `debugPrint`) |
| `UiAxiosHelper` | axios | REST URL path generation: `restPath`, `relationRestPath`, `operationRestPath` |
| `UiServiceHelper` | service | Access vs non-access relations, service import tokens, operation capabilities |

### Key Conventions

- **TypeScript type mapping:** `NumericType`→`number`, `BooleanType`→`boolean`, `StringType`→`string`, `DateType/TimeType/TimestampType`→`Date`, `EnumerationType`→generated enum
- **Naming:** `classDataName(classType, suffix)` strips `::` separators and appends suffix (e.g., `"Stored"`, `"QueryCustomizer"`)
- **REST paths:** tilde-prefixed verbs (`~get`, `~list`, `~create`, `~update`, `~delete`, `~template`, `~range`, `~set`, `~unset`, `~add`, `~remove`, `~validate`)
- **Draft identifiers:** client-generated IDs start with `draft:` prefix, stripped during serialization, auto-generated on deserialization with `draftIdentifierPrefix + uuid`
- **Singleton serializers:** generated serializers use `_instance` + `getInstance()` to avoid circular reference issues
- **`TRANSFER_SKIP_SEGMENT`** (`_default_transferobjecttypes`) is filtered out when constructing REST paths
- **All templates** start with `{{> fragment.header.hbs }}` partial for generated-source header comments
- **SpEL `#` prefix** references helper methods and context variables in YAML expressions

### Generated Output Structure

```
src/services/
  data-api/          ← from api module
    common/          (JudoIdentifiable, JudoStored, QueryCustomizer, errors/, files/, operations/, security/)
    model/           (one .ts per ClassType + EnumerationType)
    rest/            (QueryCustomizer, Serializer, MaskBuilder, FilterBy per type + shared utils)
  data-service/      ← from service module
    AccessService.ts, <Class>Service.ts, <Owner>ServiceFor<Rel>.ts
  data-axios/        ← from axios module
    JudoAxiosProvider.ts, JudoAxiosService.ts, AccessServiceImpl.ts, <Class>ServiceImpl.ts, <Owner>ServiceFor<Rel>Impl.ts
```

## UI Metamodel (judo-meta-ui)

The `.model` files consumed by this generator are instances of the JUDO UI metamodel (`hu.blackbelt.judo.meta.ui.model`). The metamodel is defined in Ecore and consists of three packages:

### `ui` (Root Package)

- **`Application`** — root element. Contains all data elements, actor types, pages, navigation, and theme. Key references:
  - `dataElements: ClassType[*]` — all transfer object types
  - `dataTypes: DataType[*]` — primitive types (String, Numeric, Boolean, Date, Time, Timestamp, Enumeration, Binary)
  - `enumerationTypes: EnumerationType[*]` — all enums
  - `classTypes: ClassType[*]` — derived from dataElements
  - `relationTypes: RelationType[*]` — all relations across all classes
  - `actorTypes: ActorType[*]` — actor/role types (used for per-actor generation)
  - `pages: PageDefinition[*]`, `navigations: NavigationItem[*]`, `theme: Theme`

### `ui.data` (Data Package) — the primary package for this template

- **`ClassType`** — represents a transfer object (generates a TypeScript interface). Key properties:
  - `attributes: AttributeType[*]` — typed fields (each has a `dataType` reference)
  - `relations: RelationType[*]` — navigation to other ClassTypes
  - `operations: OperationType[*]` — callable actions
  - `isTemplateable`, `isMapped` — capability flags
  - `fQName` (inherited from `NamedElement`) — fully-qualified `::` separated name

- **`AttributeType`** — a field on a ClassType:
  - `dataType: DataType` — the type reference
  - `isFilterable`, `isSortable`, `isRequired`, `isMemberTypeMapped`, `isReadOnly`
  - `memberType: AttributeType` — maps to the underlying PSM attribute

- **`RelationType`** — a reference from one ClassType to another:
  - `target: ClassType` — the target type
  - `isAccess` — true if this is a top-level access relation (entry point)
  - `isCollection`, `isRequired`, `isCreatable`, `isDeletable`, `isUpdatable`, `isRefreshable`
  - `isSetable`, `isUnsetable`, `isAddable`, `isRemovable`, `isListable`
  - `isMemberTypeMapped`, `isRangeable`, `isFilterable`, `isSortable`
  - `operations: OperationType[*]` — actions on this relation
  - `relations: RelationType[*]` — nested sub-relations

- **`OperationType`** — a callable action:
  - `input: ClassType`, `output: ClassType` — optional input/output types
  - `faults: ClassType[*]` — fault/error types
  - `isInputRangeable`, `isFilterable`, `isSortable`
  - `isMapped` — whether it maps to a backend operation

- **`DataType`** (abstract) — base for all primitive types. Concrete subtypes:
  - `StringType` (→ `string`), `NumericType` (→ `number`), `BooleanType` (→ `boolean`)
  - `DateType`, `TimeType`, `TimestampType` (→ `Date`)
  - `EnumerationType` (→ generated enum), `BinaryType` (→ binary handling)

- **`EnumerationType`** — has `members: EnumerationMember[*]`, each with `ordinal: int`

- **`NamedElement`** — base type providing `name`, `fQName` (fully-qualified name with `::` separator)

### `ui.data` — Visual/Layout Package (less relevant to this template)

- **`VisualType`** — base for visual components, extends `NamedElement`
- **`PageDefinition`** — UI page definitions with containers, links, actions
- **`PageContainer`**, **`Link`**, **`Table`**, **`Button`**, etc.
- **`ActionDefinition`** — UI action definitions (CRUD, custom operations)
- **`NavigationItem`** — menu/navigation entries
- **`Theme`** — color/styling definitions

### Key Model Traversal Patterns Used in Helpers

```java
// Get all class types from application
application.getClassTypes()  // or application.getDataElements() filtered

// Get relations for a class
classType.getRelations()

// Check if relation is an entry point
relationType.isIsAccess()

// Get operation faults
operationType.getFaults()

// Get enum members
enumerationType.getMembers()
```

## Generator Framework (judo-generator-commons)

The `judo-generator-commons` library provides the code generation infrastructure. Understanding this is essential for writing templates and helpers.

### Core Components

**`ModelGenerator`** — central orchestrator. Static methods drive the pipeline:
1. `createGeneratorContext()` — builds context from URIs, YAML descriptors, helpers
2. `generateToDirectory()` — executes generation, writes files with checksum tracking

**`ModelGeneratorContext`** — holds generation state:
- `createHandlebars()` — configures Handlebars with: UTF-8, `HighConcurrencyTemplateCache`, `ConditionalHelpers` (eq/neq/gt/gte/lt/lte/and/or/not), `StringHelpers`, custom `times` helper, all `@TemplateHelper` classes, pretty print, infinite loops allowed, string params enabled
- `createSpringEvaluationContext()` — registers all helper static methods as SpEL functions

### Writing Template Helpers

Helpers must follow these rules:
- Annotate class with `@TemplateHelper`
- Extend `StaticMethodValueResolver`
- Methods must be `public static` with 0 or 1 parameters
- Methods become available as both Handlebars helpers (`{{methodName arg}}`) and SpEL functions (`#methodName(arg)`)

### `@ContextAccessor` Mechanism

Exactly one class can be annotated with `@ContextAccessor` (enforced — throws if multiple found). It must implement `public static void bindContext(Map<String, ?> context)`. This bridges template parameters (like `debugPrint`) to static helper methods via `ThreadLocalContextHolder`:

```java
@TemplateHelper @ContextAccessor
public class StoredVariableHelper extends StaticMethodValueResolver {
    public static void bindContext(Map<String, ?> context) {
        ThreadLocalContextHolder.bindContext(context);
    }
    public static synchronized Boolean isDebugPrint() {
        return Boolean.parseBoolean((String) ThreadLocalContextHolder.getVariable("debugPrint"));
    }
}
```

### YAML Descriptor Full Schema

```yaml
templates:
  - name: uniqueId              # Required for override matching
    templateName: path/to.hbs   # Path to .hbs file (or use inline `template:`)
    pathExpression: "'out.ts'"   # SpEL → String (output path)
    factoryExpression: "#fn()"   # SpEL → Collection (iterate, each becomes #self)
    conditionExpression: "expr"  # SpEL → Boolean (skip if false)
    applicationBased: true       # Provides #application in context
    actorTypeBased: true         # Default true, iterates over actor types
    copy: false                  # Binary copy mode (no Handlebars processing)
    exclude: false               # Used in overrides to remove a template
    permission: rw-r--r--        # POSIX file permissions
    templateContext:             # Extra named SpEL variables
      - name: varName
        expression: "#self"
```

### SpEL Context Variables

Available in `factoryExpression`, `pathExpression`, `conditionExpression`, and `templateContext`:
- `#self` — current factory element (or model if no factoryExpression)
- `#model` — the root model object
- `#application` — when `applicationBased: true`
- `#actorType` — when `actorTypeBased: true`
- All `@TemplateHelper` static methods via `#methodName(args)`

### Template Override Mechanism

The `ChainedURLTemplateLoader` checks for `template.override.hbs` before loading `template.hbs`. Downstream projects can override any template without modifying the original. YAML-level overrides match by `name` field; use `exclude: true` to remove a template.

### Built-in Helpers from judo-generator-commons

| Helper | Methods |
|--------|---------|
| `StringHelper` | `lowerCase`, `upperCase`, `camelCaseToSnakeCase`, `decorateWithAsterisks`, `notEmpty`, `empty`, `isTrue`, `cleanup`, `firstToUpperCase`, `firstToLowerCase` |
| `ConditionalHelpers` | `eq`, `neq`, `gt`, `gte`, `lt`, `lte`, `and`, `or`, `not` (Handlebars block helpers) |
| `StringHelpers` | `capitalize`, `abbreviate`, etc. (Handlebars built-in) |
| `times` | Custom iteration helper for repeat loops |

### File Protection

- `.generator-ignore` — glob patterns for files that should never be overwritten
- `.generator-checksum-ignore` — glob patterns for files where checksum mismatches are suppressed (always overwritten)
- `.generated-files` — tracks checksums of generated files; stale files are deleted on regeneration
- Content normalization (enabled by default) prevents false checksum mismatches from formatters
