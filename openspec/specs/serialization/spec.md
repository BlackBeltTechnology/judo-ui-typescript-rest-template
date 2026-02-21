# Serialization

Generates per-type serializer classes that convert between TypeScript domain objects and JSON wire format, handling date/time formatting, relation traversal, stored metadata transfer, and draft identifier management.

## Perspectives

- **Template developer**: Authors `serializer.ts.hbs` and helper methods that inspect attribute types and relation shapes to produce serialize/deserialize logic.
- **Consumer**: Uses generated serializers implicitly (called by Axios implementations) or explicitly for custom serialization needs. Singleton pattern (`getInstance()`) prevents circular dependency issues.
- **Generator framework**: Produces one serializer file per ClassType, following the same `#getClassTypes(#application)` factory expression as model types.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/serializer.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/serialization.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/serializer.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/utils.ts.hbs` |
| Helper | `UiGeneralHelper.java` — `isAttributeDate()`, `isAttributeTime()`, `isAttributeTimestamp()`, `isEnumType()` |
| YAML | `judo-ui-typescript-rest-api/src/main/resources/ui-typescript-rest.yaml` |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-api/rest/<ClassName>Serializer.ts` | One per ClassType (non-actor) | N |
| `data-api/rest/serialization.ts` | Singleton | 1 |
| `data-api/common/Serializer.ts` | Singleton (interface) | 1 |
| `data-api/common/utils.ts` | Singleton (draft ID + stored member helpers) | 1 |

---

## Features

### Feature: Serializer interface

**Given** the common infrastructure templates
**When** the generator produces `data-api/common/Serializer.ts`
**Then** the file contains:
- `SerializerUtil` interface with methods: `serializeDate`, `serializeTime`, `serializeTimestamp`, `deserializeDate`, `deserializeTime`, `deserializeTimestamp`
- `Serializer<T>` interface with `serialize(instance: T): any` and `deserialize(data: any): T`

### Feature: Per-type serializer class generation

**Given** a ClassType element
**When** the generator processes `serializer.ts.hbs`
**Then** two classes are produced in a single file:
- `<ClassName>Serializer` implementing `Serializer<<ClassName>>` — handles base interface
- `<ClassName>StoredSerializer` implementing `Serializer<<ClassName>Stored>` — wraps the base serializer

### Feature: Singleton pattern

**Given** a generated serializer class
**When** the class is defined
**Then** it includes:
- A static `_instance` field
- A static `getInstance()` method returning the singleton
- This pattern avoids circular reference issues when serializers reference each other through relations

### Feature: Lazy relation serializer loading

**Given** a ClassType with relations to other ClassTypes
**When** the serializer references child serializers
**Then** child serializers are obtained via `<ChildType>Serializer.getInstance()` at call time (not constructor time)
**And** this prevents circular dependency failures during module initialization

### Feature: Attribute serialization

**Given** an `AttributeType` on a ClassType

**Scenario: Required attribute — serialize**
**When** `attribute.isRequired == true` and the value is defined
**Then** the serializer writes the value directly (applying type-specific formatting)

**Scenario: Optional attribute — serialize**
**When** `attribute.isRequired == false`
**Then** the serializer checks `instance.<attr> !== undefined` before writing
**And** if the value is `null`, it serializes as `null` (preserving explicit null)

**Scenario: Undefined attribute — serialize**
**When** `instance.<attr> === undefined`
**Then** the attribute is omitted from the serialized output

### Feature: Date/Time/Timestamp serialization

**Given** an attribute with a temporal DataType

**Scenario: DateType serialization**
**When** `isAttributeDate(attribute) == true`
**Then** `serializerUtil.serializeDate(value)` is called, producing `yyyy-MM-dd` string

**Scenario: TimeType serialization**
**When** `isAttributeTime(attribute) == true`
**Then** `serializerUtil.serializeTime(value)` is called, producing `HH:mm:ss` string (or `HH:mm:ss.SSS` for sub-second precision)

**Scenario: TimestampType serialization**
**When** `isAttributeTimestamp(attribute) == true`
**Then** `serializerUtil.serializeTimestamp(value)` is called, producing ISO 8601 string

### Feature: Date/Time/Timestamp deserialization

**Given** an attribute with a temporal DataType

**Scenario: DateType deserialization**
**When** `isAttributeDate(attribute) == true`
**Then** `serializerUtil.deserializeDate(value)` is called, parsing `yyyy-MM-dd` string to Date

**Scenario: TimeType deserialization**
**When** `isAttributeTime(attribute) == true`
**Then** `serializerUtil.deserializeTime(value)` is called, parsing `HH:mm:ss` or `HH:mm:ss.SSS` string to Date

**Scenario: TimestampType deserialization**
**When** `isAttributeTimestamp(attribute) == true`
**Then** `serializerUtil.deserializeTimestamp(value)` is called, parsing ISO 8601 string to Date

### Feature: Relation serialization

**Given** a `RelationType` on a ClassType

**Scenario: Collection relation — serialize**
**When** `relation.isCollection == true` and `Array.isArray(instance.<rel>)`
**Then** each array element is serialized via the target type's `StoredSerializer.getInstance().serialize(element)`

**Scenario: Single optional relation — serialize**
**When** `relation.isCollection == false` and `relation.isOptional == true`
**Then** if value is `null`, it serializes as `null`; otherwise delegates to target serializer

**Scenario: Single required relation — serialize**
**When** `relation.isCollection == false` and `relation.isOptional == false`
**Then** delegates directly to target serializer (no null check)

### Feature: Relation deserialization

**Given** a `RelationType` on a ClassType

**Scenario: Collection relation — deserialize**
**When** `relation.isCollection == true` and the JSON contains an array
**Then** each array element is deserialized via the target type's `StoredSerializer.getInstance().deserialize(element)`

**Scenario: Single optional relation — deserialize**
**When** `relation.isCollection == false` and `relation.isOptional == true`
**Then** if value is `null`, the result is `null`; otherwise delegates to target serializer

### Feature: Stored metadata transfer

**Given** any serialize or deserialize operation
**When** `applyStoredMembers(result, instance)` is called
**Then** the following fields are copied from source to result:
- `__deleteable`
- `__updateable`
- `__selected`
- `__version`
- `__entityType`
- `__signedIdentifier`
- `__identifier` (copied only if it does NOT start with `draft:` prefix during serialization)

### Feature: Draft identifier generation

**Given** a deserialization operation
**When** the deserialized result has no `__identifier` field (or it is falsy)
**Then** a new draft identifier is auto-generated: `draft:<uuid-v4>`
**And** the `draftIdentifierPrefix` constant is `'draft:'`

### Feature: Draft identifier stripping on serialization

**Given** a serialization operation
**When** `applyStoredMembers()` copies `__identifier`
**Then** if the identifier starts with `draft:`, it is NOT copied to the serialized output
**And** this prevents client-generated draft IDs from being sent to the backend

### Feature: Serialization utility functions

**Given** the `serialization.ts` singleton
**When** the generator produces the file
**Then** it contains:
- `uiDateToServiceDate(date)` — formats to `yyyy-MM-dd` using `date-fns/format`
- `serviceDateToUiDate(dateStr)` — parses string to `Date`
- `uiTimeToServiceTime(time)` — formats to `HH:mm:ss`
- `serviceTimeToUiTime(timeStr)` — parses string to `Date` (handles both `HH:mm:ss` and `HH:mm:ss.SSS`)
- `serializerUtil` object implementing `SerializerUtil` with timestamp handling via `toISOString()`
