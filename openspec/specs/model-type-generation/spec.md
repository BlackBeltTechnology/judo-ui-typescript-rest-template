# Model Type Generation

Generates TypeScript interfaces, enumerations, and type aliases from the JUDO UI metamodel's `ClassType` and `EnumerationType` elements.

## Perspectives

- **Template developer**: Authors `.hbs` templates and Java helpers that produce TypeScript source from metamodel elements.
- **Consumer**: Uses the generated TypeScript interfaces and enums as the type foundation for the entire REST client layer.
- **Generator framework**: Evaluates YAML factory expressions to iterate over model elements and produce one file per element.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-api/src/main/resources/model/class.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/model/enum.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/model/index.ts.hbs` |
| Helper | `UiGeneralHelper.java` — type mapping, relation type resolution |
| Helper | `UiCommonsHelper.java` — naming (`classDataName`, `firstToUpper/Lower`) |
| YAML | `judo-ui-typescript-rest-api/src/main/resources/ui-typescript-rest.yaml` |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-api/model/<ClassName>.ts` | One per `ClassType` where `isActor == false` | N per application |
| `data-api/model/<EnumName>.ts` | One per `EnumerationType` | M per application |
| `data-api/model/index.ts` | Barrel re-export of all model files | 1 |

---

## Features

### Feature: ClassType interface generation

**Given** a `ClassType` element in the UI metamodel where `isActor == false`
**When** the generator processes the `class.ts.hbs` template
**Then** a TypeScript file is produced at `data-api/model/<classDataName>.ts` containing:
- An interface named `<classDataName>` extending `JudoTechnicalIdentifiable`
- An `__identifier?: string` field for client-side identity tracking
- One field per attribute on the ClassType
- One field per relation on the ClassType
- A type alias `<classDataName>Attributes` as a union of all attribute names (if attributes exist)
- A type alias `<classDataName>Relations` as a union of all relation names (if relations exist)
- A `<classDataName>Stored` interface extending both `JudoStored<classDataName>` and the base interface

### Feature: Actor ClassType exclusion

**Given** a `ClassType` element where `isActor == true`
**When** the generator evaluates the factory expression `#getClassTypes(#application)`
**Then** the ClassType is excluded and no file is generated for it

### Feature: Attribute type mapping

**Given** an `AttributeType` on a ClassType
**When** the template resolves the TypeScript type via `typescriptType(attribute.dataType)`
**Then** the mapping produces:

| Metamodel DataType | TypeScript Type |
|---|---|
| `StringType` | `string` |
| `NumericType` | `number` |
| `BooleanType` | `boolean` |
| `DateType` | `string` |
| `TimeType` | `string` |
| `TimestampType` | `string` |
| `EnumerationType` | Generated enum name (e.g., `MyEnum`) |
| `BinaryType` | `any` |
| `PasswordType` | `string` |

### Feature: Required vs optional attribute fields

**Given** an `AttributeType` on a ClassType

**Scenario: Required attribute**
**When** `attribute.isRequired == true`
**Then** the field is generated as `attributeName: <type>` (no `?`, no `null`)

**Scenario: Optional attribute**
**When** `attribute.isRequired == false`
**Then** the field is generated as `attributeName?: null | <type>`

### Feature: Relation field generation

**Given** a `RelationType` on a ClassType
**When** the template generates the relation field

**Scenario: Collection relation**
**When** `relation.isCollection == true`
**Then** the field type is `Array<<TargetName>Stored>` (or `Array<<TargetName>>` if `target.isMapped == false`)

**Scenario: Single required relation**
**When** `relation.isCollection == false` and `relation.isOptional == false`
**Then** the field type is `<TargetName>Stored` (no `?`, no `null`)

**Scenario: Single optional relation**
**When** `relation.isCollection == false` and `relation.isOptional == true`
**Then** the field type is `<TargetName>Stored` with `?: null |` prefix

**Scenario: Target not mapped**
**When** `relation.target.isMapped == false`
**Then** the `Stored` suffix is omitted; the base type name is used directly

### Feature: Stored interface generation

**Given** any ClassType interface `Foo`
**When** the template generates the Stored variant
**Then** `FooStored` is produced extending `JudoStored<Foo>` and `Foo`, inheriting metadata fields:
- `__signedIdentifier: string`
- `__entityType?: string`
- `__updateable?: boolean`
- `__deleteable?: boolean`
- `__selected?: boolean`
- `__version?: number`

### Feature: Enumeration generation

**Given** an `EnumerationType` in the application's `dataTypes`
**When** the generator processes `enum.ts.hbs`
**Then** a TypeScript file is produced at `data-api/model/<enumName>.ts` containing:
- A `const enum` (or `enum`) with one member per `EnumerationMember`, each valued as its string name (e.g., `MEMBER1 = 'MEMBER1'`)
- A private type alias `OrdinalOf<EnumName>` as a union of ordinal literal types (e.g., `0 | 1 | 2`)
- A function `get<EnumName>LiteralByOrdinal(ordinal)` converting ordinal to enum literal
- A function `get<EnumName>OrdinalByLiteral(literal)` converting enum literal to ordinal

### Feature: Attributes type alias

**Given** a ClassType with at least one attribute
**When** the template processes the ClassType
**Then** a type alias is generated: `type <ClassName>Attributes = 'attr1' | 'attr2' | ...`

**Given** a ClassType with no attributes
**When** the template processes the ClassType
**Then** the `Attributes` type alias is omitted

### Feature: Relations type alias

**Given** a ClassType with at least one relation
**When** the template processes the ClassType
**Then** a type alias is generated: `type <ClassName>Relations = 'rel1' | 'rel2' | ...`

**Given** a ClassType with no relations
**When** the template processes the ClassType
**Then** the `Relations` type alias is omitted

### Feature: Import generation

**Given** a ClassType with relations referencing other ClassTypes and enum-typed attributes
**When** the template generates imports
**Then** import statements are produced for:
- Each referenced `EnumerationType` (from `./EnumName`)
- Each referenced relation target `ClassType` (both base and Stored interfaces, from `./TargetName`)
- `JudoStored` and `JudoTechnicalIdentifiable` from `../common`

### Feature: Model barrel index

**Given** the full set of ClassTypes and EnumerationTypes in the application
**When** the generator processes `index.ts.hbs`
**Then** a single `data-api/model/index.ts` file is produced re-exporting all model files

### Feature: Naming convention

**Given** a ClassType with fully-qualified name `Package::SubPackage::TypeName`
**When** the `classDataName(classType, suffix)` helper resolves the name
**Then** the `::` separators are stripped and the suffix is appended (e.g., `TypeName`, `TypeNameStored`, `TypeNameQueryCustomizer`)
