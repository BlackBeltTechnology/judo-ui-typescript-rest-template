# Query Infrastructure

Generates typed query customizers, filter-by types, and query serialization logic for every ClassType, enabling type-safe filtering, sorting, pagination, and field selection.

## Perspectives

- **Template developer**: Authors templates that inspect `AttributeType.isFilterable` and `DataType` subtypes to produce per-type query interfaces and serializers.
- **Consumer**: Uses `<Type>QueryCustomizer` to compose queries with typed filter arrays, sort specifications, cursor-based pagination, and field masks.
- **Generator framework**: Evaluates `#getClassTypes(#application)` and `#getFilterableDataTypes(#application)` factory expressions to produce files.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/queryCustomizer.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/queryCustomizerSerializer.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/filterable.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/queryCustomizer.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/seek.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/orderingType.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/operations/*.ts.hbs` |
| Helper | `UiGeneralHelper.java` — `getFilterableDataTypes()`, `isEnumType()` |
| YAML | `judo-ui-typescript-rest-api/src/main/resources/ui-typescript-rest.yaml` |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-api/rest/<ClassName>QueryCustomizer.ts` | One per ClassType (non-actor) | N |
| `data-api/rest/<ClassName>QueryCustomizerSerializer.ts` | One per ClassType (non-actor) | N |
| `data-api/rest/FilterBy<DataTypeName>.ts` | One per distinct filterable DataType | Variable |
| `data-api/common/QueryCustomizer.ts` | Singleton | 1 |
| `data-api/common/Seek.ts` | Singleton | 1 |
| `data-api/common/OrderingType.ts` | Singleton | 1 |
| `data-api/common/operations/_StringOperation.ts` | Singleton | 1 |
| `data-api/common/operations/_NumericOperation.ts` | Singleton | 1 |
| `data-api/common/operations/_BooleanOperation.ts` | Singleton | 1 |
| `data-api/common/operations/_EnumerationOperation.ts` | Singleton | 1 |

---

## Features

### Feature: Base QueryCustomizer interface

**Given** the common infrastructure templates
**When** the generator produces `data-api/common/QueryCustomizer.ts`
**Then** the file contains:
- `QueryCustomizer<T>` interface with fields:
  - `_mask?: string` — field selection mask (e.g., `{field1,field2,rel{subField}}`)
  - `_seek?: Seek<Partial<JudoStored<T>>>` — cursor-based pagination
  - `_orderBy?: Array<OrderingType>` — sort specification
  - `_identifier?: string` — filter by client identifier
- `CommandQueryCustomizer` type alias: `Pick<QueryCustomizer<any>, '_mask'>`

### Feature: Seek-based pagination

**Given** the common infrastructure templates
**When** the generator produces `data-api/common/Seek.ts`
**Then** the file contains a `Seek<T>` interface with:
- `lastItem?: T` — cursor object from the last item of the previous page
- `limit?: number` — page size
- `reverse?: boolean` — pagination direction (true = reverse)

### Feature: OrderingType

**Given** the common infrastructure templates
**When** the generator produces `data-api/common/OrderingType.ts`
**Then** the file contains an `OrderingType` interface with:
- `attribute: string` — name of attribute to sort by
- `descending?: boolean` — true for descending, false/undefined for ascending

### Feature: Per-type QueryCustomizer generation

**Given** a ClassType with attributes
**When** the generator processes `queryCustomizer.ts.hbs`
**Then** a `<ClassName>QueryCustomizer` interface extending `QueryCustomizer<ClassName>` is produced with:
- One `<attrName>?: Array<FilterBy<DataType>>` field per attribute where `attribute.isFilterable == true`
- Attributes where `isFilterable == false` are excluded

### Feature: Filterable attribute inclusion

**Given** an `AttributeType` on a ClassType

**Scenario: Filterable attribute**
**When** `attribute.isFilterable == true`
**Then** a filter field appears in the QueryCustomizer interface typed as `Array<FilterBy<DataTypeName>>`

**Scenario: Non-filterable attribute**
**When** `attribute.isFilterable == false`
**Then** no filter field is generated for this attribute

### Feature: Filter operation enums per data type

**Given** the common operations templates
**When** the generator produces operation enum files
**Then** the following enums are generated:

| Enum | Members |
|---|---|
| `_StringOperation` | `lessThan`, `greaterThan`, `lessOrEqual`, `greaterOrEqual`, `equal`, `notEqual`, `matches`, `like`, `isNotEmpty`, `isEmpty` |
| `_NumericOperation` | `lessThan`, `greaterThan`, `lessOrEqual`, `greaterOrEqual`, `equal`, `notEqual`, `isNotEmpty`, `isEmpty` |
| `_BooleanOperation` | `equals`, `notEquals`, `isNotEmpty`, `isEmpty` |
| `_EnumerationOperation` | `equals`, `notEquals`, `isNotEmpty`, `isEmpty` |

### Feature: FilterBy type generation

**Given** a DataType that is the type of at least one filterable attribute across all ClassTypes
**When** the generator evaluates `#getFilterableDataTypes(#application)`
**Then** a `FilterBy<DataTypeName>.ts` file is produced containing:

**Scenario: Primitive data type (String, Numeric, Boolean, Date, Time, Timestamp)**
- `FilterBy<DataTypeName>` interface with `value: <tsType>` and `operator: _<Category>Operation`

**Scenario: Enumeration data type**
- `FilterBy<EnumName>` interface with `value: <EnumName>` and `operator: _EnumerationOperation`

| DataType Subclass | Filter Value Type | Operation Enum |
|---|---|---|
| `StringType` | `string` | `_StringOperation` |
| `NumericType` | `number` | `_NumericOperation` |
| `BooleanType` | `boolean` | `_BooleanOperation` |
| `DateType` | `string` | `_StringOperation` |
| `TimeType` | `string` | `_StringOperation` |
| `TimestampType` | `string` | `_StringOperation` |
| `EnumerationType` | Generated enum type | `_EnumerationOperation` |

### Feature: FilterBy deduplication

**Given** multiple ClassTypes with filterable attributes sharing the same DataType
**When** the generator evaluates `#getFilterableDataTypes(#application)`
**Then** only one `FilterBy` file is generated per distinct DataType (deduplicated by XMI ID)

### Feature: QueryCustomizer serialization

**Given** a ClassType with a generated QueryCustomizer
**When** the generator processes `queryCustomizerSerializer.ts.hbs`
**Then** a `serialize<ClassName>QueryCustomizer(qc)` function is produced that:
- Passes through `_mask`, `_orderBy`, `_identifier` unchanged
- Serializes each filter array's values using the appropriate serializer (date/time/timestamp formatting)
- Serializes the `_seek.lastItem` using the Stored serializer if present
- Returns `null` if the input is `null` or `undefined`

### Feature: Date filter value serialization

**Given** a filterable attribute of type `DateType`
**When** the QueryCustomizer serializer processes the filter array
**Then** each filter's `value` is serialized via `serializerUtil.serializeDate(value)` producing `yyyy-MM-dd` format

### Feature: Time filter value serialization

**Given** a filterable attribute of type `TimeType`
**When** the QueryCustomizer serializer processes the filter array
**Then** each filter's `value` is serialized via `serializerUtil.serializeTime(value)` producing `HH:mm:ss` format

### Feature: Timestamp filter value serialization

**Given** a filterable attribute of type `TimestampType`
**When** the QueryCustomizer serializer processes the filter array
**Then** each filter's `value` is serialized via `serializerUtil.serializeTimestamp(value)` producing ISO 8601 format

### Feature: Seek cursor serialization

**Given** a QueryCustomizer with `_seek.lastItem` populated
**When** the serializer processes the seek object
**Then** `lastItem` is serialized through the type's Stored serializer (handling date/time conversion and stored metadata)
**And** `limit` and `reverse` are passed through unchanged
