# Field Mask Building

Generates typed mask builder classes that produce field selection strings for the `_mask` query parameter, enabling consumers to request only needed fields and traverse relation hierarchies.

## Perspectives

- **Template developer**: Authors `mask.ts.hbs` and `MaskBuilder.ts.hbs` templates, using `getAggregatedRelations()` to determine which relations qualify for nested mask builders.
- **Consumer**: Uses `<Type>MaskBuilder` with typed attribute names and relation mask builders to compose masks like `{name,age,address{street,city}}`.
- **Generator framework**: Produces one mask builder file per ClassType plus the shared base classes.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/mask.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/maskBuilder.ts.hbs` |
| Helper | `UiGeneralHelper.java` — `getAggregatedRelations()`, `getAggregatedRelationsSize()`, `isGreaterThan()` |
| YAML | `judo-ui-typescript-rest-api/src/main/resources/ui-typescript-rest.yaml` |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-api/rest/<ClassName>MaskBuilder.ts` | One per ClassType (non-actor) | N |
| `data-api/rest/MaskBuilder.ts` | Singleton base classes | 1 |

---

## Features

### Feature: MaskBuilder base class

**Given** the shared infrastructure template
**When** the generator produces `data-api/rest/MaskBuilder.ts`
**Then** the file contains:
- `MaskBuilder` class with constructor accepting `Array<string | MaskBuilder>`
- `build()` method that deduplicates string props, builds nested builder props, and produces `{field1,field2,nested{subfield}}` format
- `RelationMaskBuilder` class extending `MaskBuilder` with a `name` property
- `RelationMaskBuilder.build()` produces `relationName{field1,field2}`

### Feature: Per-type MaskBuilder class

**Given** a ClassType element
**When** the generator processes `mask.ts.hbs`
**Then** a `<ClassName>MaskBuilder` class is produced with constructor accepting `Array<>` of:
- `<ClassName>Attributes` string union (all attribute names)
- One `<ClassName><RelationName>MaskBuilder` type per aggregated relation

### Feature: Relation mask builder classes

**Given** a ClassType with relations

**Scenario: Aggregated relations (non-ASSOCIATION)**
**When** `relation.relationKind != ASSOCIATION` (i.e., COMPOSITION, AGGREGATION, or STATIC)
**Then** a `<ClassName><RelationName>MaskBuilder` class extending `RelationMaskBuilder` is generated
**And** its constructor accepts the target type's attribute names and nested relation mask builders

**Scenario: ASSOCIATION relations**
**When** `relation.relationKind == ASSOCIATION`
**Then** no mask builder class is generated for this relation

### Feature: Mask builder import gating

**Given** a ClassType
**When** `getAggregatedRelationsSize(classType) == 0` (no non-ASSOCIATION relations)
**Then** the `RelationMaskBuilder` import is omitted from the generated file

**When** `getAggregatedRelationsSize(classType) > 0`
**Then** `RelationMaskBuilder` is imported from `./MaskBuilder`

### Feature: Mask string format

**Given** a consumer constructs a mask builder:
```typescript
new PersonMaskBuilder([
  'name',
  'age',
  new PersonAddressMaskBuilder(['street', 'city'])
]).build()
```
**When** `build()` is called
**Then** the result is `{name,age,address{street,city}}`

### Feature: Deduplication

**Given** a mask builder with duplicate props (e.g., `['name', 'name', 'age']`)
**When** `build()` is called
**Then** duplicates are removed: `{name,age}`
