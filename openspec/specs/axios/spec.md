# axios Specification

## Purpose
Generates the `data-axios/` layer of TypeScript output: concrete Axios-based HTTP client implementations of the service interfaces. Provides `UiAxiosHelper` Java class and 6 Handlebars templates (`axiosProvider`, `judoAxiosProvider`, `judoAxiosService`, `accessServiceImpl`, `classServiceImpl`, `relationServiceImpl`).

## Architecture
`UiAxiosHelper` extends `StaticMethodValueResolver` and is self-contained (no dependency on other module helpers). It generates REST endpoint paths from model elements and provides string utilities for template rendering.

Uses `::` as namespace separator and filters out `_default_transferobjecttypes` path segments. Templates are configured via `ui-typescript-rest.yaml` with factory expressions iterating over class types and relation types.

Key EMF types: `Application`, `ClassType`, `RelationType`, `OperationType`.

## Requirements

### Requirement: Root API path generation
The `rootPathForApp` method SHALL produce the root REST path for an application by joining the actor's package name tokens with `/` and appending the actor's simple name.

#### Scenario: Actor with package tokens
- **GIVEN** an `Application` whose actor has package tokens `["com", "example"]` and simple name `"Admin"`
- **WHEN** `rootPathForApp(app)` is called
- **THEN** the result is `"com/example/Admin"`

### Requirement: Class type REST path generation
The `restPath` method SHALL produce a REST path for a class type by combining package tokens, simple name (filtering out transfer skip segments), and optional prefix/suffix strings.

#### Scenario: Class with package and name
- **GIVEN** a `ClassType` with package tokens and a simple name
- **WHEN** `restPath(classType, "/", "action", "")` is called
- **THEN** the result starts with `/`, includes the package path and simple name, followed by `/action`

#### Scenario: Class name with transfer skip segment
- **GIVEN** a `ClassType` whose simple name includes `_default_transferobjecttypes`
- **WHEN** `restPath(classType, "", null, null)` is called
- **THEN** the skip segment is filtered out of the path

### Requirement: Relation REST path generation
The `relationRestPath` method SHALL produce a path using the relation's owner package tokens, owner simple name, relation name, and optional suffix.

#### Scenario: Standard relation path
- **GIVEN** a `RelationType` with owner package `["pkg"]`, owner name `"Order"`, and relation name `"items"`
- **WHEN** `relationRestPath(relation, "/~list")` is called
- **THEN** the result is `"pkg/Order/items/~list"`

### Requirement: Class type simple REST path
The `classTypeRestPath` method SHALL produce a path from the full class name (split by `::`) with transfer skip segments filtered out.

#### Scenario: Namespaced class name
- **GIVEN** a `ClassType` with name `"MyPackage::MyClass"`
- **WHEN** `classTypeRestPath(classType, "/~update")` is called
- **THEN** the result is `"/MyPackage/MyClass/~update"`

### Requirement: Operation REST path generation
The `operationRestPath` method SHALL extend the class type path with the operation name and optional suffix.

#### Scenario: Operation endpoint
- **GIVEN** a `ClassType` and an `OperationType` named `"validate"`
- **WHEN** `operationRestPath(classType, operation, "")` is called
- **THEN** the result is the class type REST path followed by `/validate`

### Requirement: Fault detection
The `hasFaults` method SHALL return true when an operation has at least one fault parameter.

#### Scenario: Operation with faults
- **GIVEN** an `OperationType` with non-empty faults list
- **WHEN** `hasFaults(operation)` is called
- **THEN** the result is `true`

#### Scenario: Operation without faults
- **GIVEN** an `OperationType` with empty faults list
- **WHEN** `hasFaults(operation)` is called
- **THEN** the result is `false`

### Requirement: Written implementation file filtering
The `getWrittenImplementations` method SHALL filter a list of generated file paths to only those ending in `Impl.ts`, returning the names with the `.ts` extension stripped.

#### Scenario: Mixed generated files
- **GIVEN** a list of file paths `["FooService.ts", "FooServiceImpl.ts", "BarServiceImpl.ts"]`
- **WHEN** `getWrittenImplementations(files)` is called
- **THEN** the result contains `["FooServiceImpl", "BarServiceImpl"]`

### Requirement: Dependency path prefix
The `getDepPath` method SHALL prepend `"../"` to an import source path.

#### Scenario: Standard import path
- **WHEN** `getDepPath("data-api/model/Foo")` is called
- **THEN** the result is `"../data-api/model/Foo"`
