# Custom Operations

Generates service interfaces and Axios implementations for calling custom operations defined on ClassTypes and RelationTypes, including input validation, input templating, range queries for operation inputs, and typed fault handling.

## Perspectives

- **Template developer**: Authors template sections that iterate `classType.operationsOrderedByName` and `relation.target.operationsOrderedByName`, producing methods whose signatures vary based on input/output/mapping flags.
- **Consumer**: Calls typed operation methods (e.g., `approve(owner, input)`) with automatic serialization, fault typing, and optional input validation/templating.
- **Generator framework**: Operations are embedded within ClassService and OwnerServiceForRelation files, not in separate files.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-service/src/main/resources/data-service/classService.ts.hbs` (operations section) |
| Template | `judo-ui-typescript-rest-service/src/main/resources/data-service/relationService.ts.hbs` (operations sections) |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/classServiceImpl.ts.hbs` (operations section) |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/relationServiceImpl.ts.hbs` (operations sections) |
| Template | `judo-ui-typescript-rest-api/src/main/resources/model/operationType.ts.hbs` (fault containers) |
| Helper | `UiServiceHelper.java` — `isOperationInputValidateable()` |
| Helper | `UiAxiosHelper.java` — `operationRestPath()`, `hasFaults()` |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| Methods within `<Class>Service.ts` | Per operation on ClassType | Embedded |
| Methods within `<Owner>ServiceFor<Rel>.ts` | Per operation on target + target's relations | Embedded |
| `data-api/model/<OperationName>.ts` | One per operation with faults | Variable |

---

## Features

### Feature: Call operation

**Given** an `OperationType` on a ClassType or relation target
**When** the generator processes the operation
**Then** a call method is always generated (unconditional)

**Scenario: Method signature variants**

The method signature varies based on three flags:

| Flag | Effect on Signature |
|---|---|
| `operation.isMapped == true` (ClassService) or `operation.isStatic == false` (RelationService) | `owner` parameter is included |
| `operation.input != null` | `target` parameter is included (typed as input type) |
| `operation.output != null` | Return type is `<OutputType>Stored`; otherwise `void` |

This produces four possible signature shapes:
```typescript
// Mapped + input + output:
operationName(owner: OwnerStored, target: InputType): Promise<JudoRestResponse<OutputTypeStored>>;

// Mapped + no input + output:
operationName(owner: OwnerStored): Promise<JudoRestResponse<OutputTypeStored>>;

// Mapped + input + no output:
operationName(owner: OwnerStored, target: InputType): Promise<JudoRestResponse<void>>;

// Static + no input + no output:
operationName(): Promise<JudoRestResponse<void>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/<operationName>`
- Request body: serialized input (or `null` if no input)
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier` (only if mapped/non-static)
- Response: deserialized output (or raw data if void)

### Feature: Validate operation input

**Given** an operation where:
- `operation.input != null`
- `operation.input.behaviours` contains `VALIDATE_INPUT`

**When** the generator evaluates `isOperationInputValidateable(operation) == true`
**Then** a `validateOn<OperationName>()` method is generated

**Scenario: Service interface**
```typescript
validateOn<OperationName>(
  owner: OwnerStored,        // if mapped/non-static
  target: InputType
): Promise<JudoRestResponse<InputType>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/<operationName>/~validate`
- Request body: serialized input
- Response: deserialized input type (validation feedback)

**Given** an operation where `isOperationInputValidateable == false`
**Then** no validate method is generated

### Feature: Get template for operation input

**Given** an operation where:
- `operation.input != null`
- `operation.input.target.isTemplateable == true`

**Then** a `getTemplateOn<OperationName>()` method is generated

**Scenario: Axios implementation**
- HTTP method: `GET`
- Path: `<actorPath>/<InputType>/~template`
- Response: deserialized input type with default values

**Given** an operation where `operation.input == null` or `operation.input.target.isTemplateable == false`
**Then** no template method is generated

### Feature: Range query for operation input

**Given** an operation where `operation.isInputRangeable == true`
**Then** a `getRangeOn<OperationName>(owner?, queryCustomizer?, headers?)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/<operationName>/~range`
- Request body: `{ owner: serialized_owner, queryCustomizer: serialized_qc }`
- Headers: `X-Judo-MarkSelectedRangeItems: 'true'`
- Response: array of deserialized stored instances

**Given** an operation where `isInputRangeable == false`
**Then** no range method is generated

### Feature: Range query for operation input relations

**Given** an operation where `operation.input != null`
**And** the input type has relations
**Then** for each relation on `operation.input.target.relationsOrderedByName`, a range method is generated:

```typescript
getRangeOn<OperationName>For<InputRelationName>(
  owner?: InputTypeStored,
  queryCustomizer?: InputRelationTargetQueryCustomizer,
  headers?: Record<string, string>
): Promise<JudoRestResponse<Array<InputRelationTargetStored>>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<InputType>/<inputRelationName>/~range`
- Headers: `X-Judo-MarkSelectedRangeItems: 'true'`

These methods are always generated (no additional condition beyond `operation.input != null`).

### Feature: Typed fault containers

**Given** an operation where `operation.getFaults()` is non-empty
**When** the generator processes `operationType.ts.hbs`
**Then** a fault container file is generated at `data-api/model/<OperationName>.ts` containing:

```typescript
export interface <OperationName>FaultContainer {
  <faultKey1>?: null | FaultType1;
  <faultKey2>?: null | FaultType2;
  // ... one per fault type
}
```

**Scenario: Axios implementation JSDoc**
**When** `hasFaults(operation) == true`
**Then** the call method's JSDoc includes:
```
@throws {AxiosError} With data containing {@link <OperationName>FaultContainer} for status code 422
```

**Scenario: HTTP 422 response**
**When** a custom operation returns HTTP 422
**Then** the response data matches the `<OperationName>FaultContainer` interface (typed business errors)

### Feature: Operation scoping hierarchy

Operations appear at three levels in the generated services:

```
ClassService
  └── classType.operationsOrderedByName          ← Class-level operations
  └── relation.target.operationsOrderedByName     ← Operations on relation targets

OwnerServiceForRelation
  └── relation.target.operationsOrderedByName     ← Direct target operations
  └── targetRelation.target.operationsOrderedByName ← Nested target operations
```

**Scenario: ClassService — class-level operations**
- Iterate `classType.operationsOrderedByName`
- Method name: `<operationName>(...)`
- Owner condition: `operation.isMapped`

**Scenario: ClassService — relation target operations**
- Iterate each `relation.target.operationsOrderedByName`
- Method name: `<operationName>For<RelationName>(...)`
- Owner condition: `operation.isMapped`

**Scenario: OwnerServiceForRelation — direct target operations**
- Iterate `relation.target.operationsOrderedByName`
- Method name: `<operationName>(...)`
- Owner condition: `!operation.isStatic`

**Scenario: OwnerServiceForRelation — nested target operations**
- Iterate `targetRelation.target.operationsOrderedByName` (for each target relation)
- Method name: `<operationName>For<TargetRelationName>(...)`
- Owner condition: `!operation.isStatic`

### Feature: Conditional generation matrix

| Method | Condition |
|---|---|
| `<operation>()` | Always generated (unconditional for each operation) |
| `validateOn<Operation>()` | `operation.input != null` AND `VALIDATE_INPUT` in `operation.input.behaviours` |
| `getTemplateOn<Operation>()` | `operation.input != null` AND `operation.input.target.isTemplateable == true` |
| `getRangeOn<Operation>()` | `operation.isInputRangeable == true` |
| `getRangeOn<Operation>For<InputRel>()` | `operation.input != null` (one per relation on input type, unconditional) |
| Fault container file | `operation.faults` is non-empty |
