# api Specification

## Purpose
Generates the `data-api/` layer of TypeScript output: interfaces, enums, serializers, query customizers, mask builders, filter types, and common shared types. Provides the `UiGeneralHelper` and `StoredVariableHelper` Java classes plus ~40 Handlebars templates.

## Architecture
`UiGeneralHelper` extends `StaticMethodValueResolver` and depends on `UiCommonsHelper` from the commons module. It provides type extraction from `Application`, TypeScript type mapping, import token management, serialization/deserialization code generation, and mask builder name generation.

`StoredVariableHelper` uses `@ContextAccessor` annotation and `ThreadLocal` to expose template execution context to helper methods.

Templates are organized into subdirectories: `common/` (shared types), `model/` (per-class/enum), `rest/` (serializers, masks, queries), configured via `ui-typescript-rest.yaml`.

Key EMF types: `Application`, `ClassType`, `RelationType`, `AttributeType`, `OperationType`, `DataType` (and subtypes: `EnumerationType`, `NumericType`, `BooleanType`, `StringType`, `DateType`, `TimeType`, `TimestampType`).

## Requirements

### Requirement: Enumeration type extraction
The `getEnumerationTypes` method SHALL return all `EnumerationType` instances from an application's data types.

#### Scenario: Application with enumerations
- **GIVEN** an `Application` with data types including `EnumerationType` instances
- **WHEN** `getEnumerationTypes(application)` is called
- **THEN** only `EnumerationType` instances are returned

### Requirement: Class type extraction
The `getClassTypes` method SHALL return all non-actor `ClassType` instances from an application, sorted by fully qualified name.

#### Scenario: Application with mixed types
- **GIVEN** an `Application` with both actor and non-actor class types
- **WHEN** `getClassTypes(application)` is called
- **THEN** only non-actor `ClassType` instances are returned, sorted by FQName

### Requirement: TypeScript type mapping
The `typescriptType` method SHALL map JUDO `DataType` subtypes to TypeScript type names.

#### Scenario: Numeric type mapping
- **GIVEN** a `NumericType` data type
- **WHEN** `typescriptType(dataType)` is called
- **THEN** the result is `"number"`

#### Scenario: Boolean type mapping
- **GIVEN** a `BooleanType` data type
- **WHEN** `typescriptType(dataType)` is called
- **THEN** the result is `"boolean"`

#### Scenario: String type mapping
- **GIVEN** a `StringType` data type
- **WHEN** `typescriptType(dataType)` is called
- **THEN** the result is `"string"`

#### Scenario: Temporal type mapping
- **GIVEN** a `DateType`, `TimeType`, or `TimestampType` data type
- **WHEN** `typescriptType(dataType)` is called
- **THEN** the result is `"Date"`

#### Scenario: Enumeration type mapping
- **GIVEN** an `EnumerationType` data type
- **WHEN** `typescriptType(dataType)` is called
- **THEN** the result is the REST parameter name of the enumeration

#### Scenario: Unknown type fallback
- **GIVEN** a data type that matches no known subtype
- **WHEN** `typescriptType(dataType)` is called
- **THEN** the result is `"any"`

### Requirement: Import token generation
The `getImportTokens` method SHALL collect enumeration and relation import tokens for a given class type, returning a sorted map of token names to module paths.

#### Scenario: Class with enum attribute and relation
- **GIVEN** a `ClassType` with an `EnumerationType` attribute and a relation to another class
- **WHEN** `getImportTokens(actor, classType)` is called
- **THEN** the result contains both the enum REST param name and the related class data name (plus "Stored" variant)

### Requirement: Filterable data type extraction
The `getFilterableDataTypes` method SHALL return distinct data types used by filterable attributes across all class types, sorted by FQName and deduplicated by XMI ID.

#### Scenario: Multiple classes with filterable attributes
- **GIVEN** an `Application` where multiple classes have filterable attributes sharing the same `DataType`
- **WHEN** `getFilterableDataTypes(application)` is called
- **THEN** the data type appears only once in the result

### Requirement: Date/time serialization code generation
The `serializePrimitive` and `deserializePrimitive` methods SHALL generate appropriate serialization/deserialization expressions for temporal types, passing non-temporal types through unchanged.

#### Scenario: Serialize a date attribute
- **GIVEN** an `AttributeType` backed by a `DateType`
- **WHEN** `serializePrimitive(attr, "value")` is called
- **THEN** the result is `"serializerUtil.serializeDate(value)"`

#### Scenario: Serialize a non-temporal attribute
- **GIVEN** an `AttributeType` backed by a `StringType`
- **WHEN** `serializePrimitive(attr, "value")` is called
- **THEN** the result is `"value"` (passed through)

### Requirement: Relation type generation
The `getRelationType` method SHALL produce a TypeScript type string for a relation, wrapping collection relations in `Array<>` and appending `"Stored"` to mapped targets.

#### Scenario: Collection relation to mapped type
- **GIVEN** a collection `RelationType` targeting a mapped `ClassType`
- **WHEN** `getRelationType(relation)` is called
- **THEN** the result is `"Array<ClassNameStored>"`

#### Scenario: Single relation to unmapped type
- **GIVEN** a non-collection `RelationType` targeting an unmapped `ClassType`
- **WHEN** `getRelationType(relation)` is called
- **THEN** the result is the class data name without "Stored" suffix

### Requirement: Mask builder name generation
The `generateBuilderProps` method SHALL produce a TypeScript union type string combining attribute type aliases and relation mask builder names for a class type.

#### Scenario: Class with attributes and aggregated relations
- **GIVEN** a `ClassType` with attributes and non-ASSOCIATION relations
- **WHEN** `generateBuilderProps(classType)` is called
- **THEN** the result is a pipe-separated union like `"ClassAttributes | ClassRelMaskBuilder"`

#### Scenario: Class with no attributes or relations
- **GIVEN** a `ClassType` with no attributes and no aggregated relations
- **WHEN** `generateBuilderProps(classType)` is called
- **THEN** the result is `"any"`

### Requirement: Template context binding
The `StoredVariableHelper.bindContext` method SHALL store the template execution context in a `ThreadLocal` for access by other helpers during rendering.

#### Scenario: Debug print flag
- **GIVEN** a template context with `debugPrint=true`
- **WHEN** `bindContext(context)` is called followed by `isDebugPrint()`
- **THEN** `isDebugPrint()` returns `true`
