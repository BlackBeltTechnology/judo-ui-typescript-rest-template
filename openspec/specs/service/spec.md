# service Specification

## Purpose
Generates the `data-service/` layer of TypeScript output: abstract service interfaces for CRUD operations, relation navigation, and access control. Provides `UiServiceHelper` Java class and 4 Handlebars templates (`accessService`, `classService`, `relationService`, `serviceIndex`).

## Architecture
`UiServiceHelper` extends `StaticMethodValueResolver` and depends on `UiCommonsHelper.classDataName` from commons. It classifies relations into access and non-access types, generates import tokens for service files, and provides operation/relation ordering utilities.

Templates are configured via `ui-typescript-rest.yaml` with factory expressions iterating over class types and relation types.

Key EMF types: `Application`, `ClassType`, `RelationType`, `OperationType`, `OperationParameterType`, `OperationTargetBehaviourType`.

## Requirements

### Requirement: Access relation classification
The `getAccessRelationsTypes` method SHALL return only relations marked as access relations, sorted by FQName.

#### Scenario: Mixed access and non-access relations
- **GIVEN** an `Application` with both access and non-access relation types
- **WHEN** `getAccessRelationsTypes(application)` is called
- **THEN** only relations where `isIsAccess()` is true are returned, sorted by FQName

### Requirement: Non-access relation classification
The `getNotAccessRelationsTypes` method SHALL return only relations NOT marked as access, sorted by FQName.

#### Scenario: Filter out access relations
- **GIVEN** an `Application` with relation types
- **WHEN** `getNotAccessRelationsTypes(application)` is called
- **THEN** only non-access relations are returned

### Requirement: Access service import tokens
The `accessJoinedImportTokens` method SHALL collect class data names for the principal and all access relations, returning sorted unique tokens.

#### Scenario: Application with principal and access relations
- **GIVEN** an `Application` with a non-null principal and access relation targets
- **WHEN** `accessJoinedImportTokens(application)` is called
- **THEN** the result includes the principal class data name and all access relation target data names, sorted

### Requirement: Relation import token resolution
The `joinedTokensForApiImport` method SHALL recursively collect all class data names referenced by a relation: owner (if not access), target, target's relations' targets, and operation input/output targets.

#### Scenario: Relation with operations
- **GIVEN** a non-access `RelationType` whose target has operations with input and output types
- **WHEN** `joinedTokensForApiImport(relation)` is called
- **THEN** the result includes the owner, target, operation input target, and operation output target class data names

### Requirement: Class service import tokens
The `joinedTokensForApiImportClassService` method SHALL collect the class itself, all relation targets, and all operation input/output types (including nested relation targets of operation inputs).

#### Scenario: Class with relations and operations
- **GIVEN** a `ClassType` with relations and operations
- **WHEN** `joinedTokensForApiImportClassService(classType)` is called
- **THEN** all referenced class data names are included in the result

### Requirement: Operation ordering
The `operationsOrderedByName` method SHALL return a class type's operations sorted alphabetically by name.

#### Scenario: Unordered operations
- **GIVEN** a `ClassType` with operations named "delete", "create", "update"
- **WHEN** `operationsOrderedByName(classType)` is called
- **THEN** operations are returned in order: "create", "delete", "update"

### Requirement: Relation ordering
The `relationsOrderedByName` method SHALL return a class type's relations sorted alphabetically by name.

#### Scenario: Unordered relations
- **GIVEN** a `ClassType` with relations named "items", "address", "owner"
- **WHEN** `relationsOrderedByName(classType)` is called
- **THEN** relations are returned in order: "address", "items", "owner"

### Requirement: Operation input validation detection
The `isOperationInputValidateable` method SHALL return true only when an operation has a non-null input whose behaviours include `VALIDATE_INPUT`.

#### Scenario: Validatable operation input
- **GIVEN** an `OperationType` with input that includes `OperationTargetBehaviourType.VALIDATE_INPUT`
- **WHEN** `isOperationInputValidateable(operation)` is called
- **THEN** the result is `true`

#### Scenario: Non-validatable operation
- **GIVEN** an `OperationType` with null input
- **WHEN** `isOperationInputValidateable(operation)` is called
- **THEN** the result is `false`

### Requirement: Relation owner type casting
The `getRelationOwnerAsClassType` method SHALL cast the owner of a `RelationType` to `ClassType`.

#### Scenario: Standard relation
- **GIVEN** a `RelationType` whose owner is a `ClassType`
- **WHEN** `getRelationOwnerAsClassType(relation)` is called
- **THEN** the owner is returned as `ClassType`
