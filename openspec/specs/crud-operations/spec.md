# CRUD Operations

Generates service interfaces and Axios implementations for Create, Read (refresh), Update, Delete, and Validate operations on ClassType instances. Each operation is conditionally generated based on metamodel capability flags.

## Perspectives

- **Template developer**: Authors `classService.ts.hbs` and `classServiceImpl.ts.hbs` templates that conditionally emit CRUD method signatures and HTTP implementations based on ClassType behaviour flags.
- **Consumer**: Calls typed service methods (e.g., `refresh()`, `update()`, `delete()`) that handle serialization, header management, and response deserialization transparently.
- **Generator framework**: Produces one service interface and one implementation per ClassType, iterating via `#getClassTypes(#application)`.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-service/src/main/resources/data-service/classService.ts.hbs` |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/classServiceImpl.ts.hbs` |
| Helper | `UiCommonsHelper.java` — naming |
| Helper | `UiAxiosHelper.java` — REST path construction |
| YAML | `judo-ui-typescript-rest-service/src/main/resources/ui-typescript-rest.yaml` |
| YAML | `judo-ui-typescript-rest-axios/src/main/resources/ui-typescript-rest.yaml` |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-service/<ClassName>Service.ts` | One per ClassType (non-actor) | N |
| `data-axios/<ClassName>ServiceImpl.ts` | One per ClassType (non-actor) | N |

---

## Features

### Feature: Get template (blank defaults)

**Given** a ClassType where `isTemplateable == true`
**When** the generator processes the service templates
**Then** a `getTemplate()` method is generated

**Scenario: Service interface**
```typescript
getTemplate(): Promise<JudoRestResponse<ClassName>>;
```

**Scenario: Axios implementation**
- HTTP method: `GET`
- Path: `<actorPath>/<ClassType>/~template`
- Response is deserialized via `<ClassName>Serializer.deserialize(data)`
- Error codes documented: 401, 403

**Given** a ClassType where `isTemplateable == false`
**Then** no `getTemplate()` method is generated

### Feature: Refresh (re-read from backend)

**Given** a ClassType where `isMapped == true`
**When** the generator processes the service templates
**Then** a `refresh()` method is generated

**Scenario: Service interface**
```typescript
refresh(
  target: ClassNameStored,
  queryCustomizer?: ClassNameQueryCustomizer,
  headers?: Record<string, string>
): Promise<JudoRestResponse<ClassNameStored>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<ClassType>/~get`
- Request body: serialized QueryCustomizer
- Headers: `X-Judo-SignedIdentifier: target.__signedIdentifier` + any custom headers
- Response is deserialized via `<ClassName>StoredSerializer.deserialize(data)`
- Error codes documented: 401, 403

**Given** a ClassType where `isMapped == false`
**Then** no `refresh()` method is generated

### Feature: Delete

**Given** a ClassType where `isDeletable == true`
**When** the generator processes the service templates
**Then** a `delete()` method is generated

**Scenario: Service interface**
```typescript
delete(target: ClassNameStored): Promise<JudoRestResponse<void>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<ClassType>/~delete`
- Request body: `undefined`
- Headers: `X-Judo-SignedIdentifier: target.__signedIdentifier`
- No response body deserialization
- Error codes documented: 400, 401, 403

**Given** a ClassType where `isDeletable == false`
**Then** no `delete()` method is generated

### Feature: Update

**Given** a ClassType where `isUpdatable == true`
**When** the generator processes the service templates
**Then** an `update()` method is generated

**Scenario: Service interface**
```typescript
update(
  target: ClassNameStored,
  queryCustomizer?: CommandQueryCustomizer
): Promise<JudoRestResponse<ClassNameStored>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<ClassType>/~update`
- Request body: `StoredSerializer.serialize(target)` (full instance)
- Headers:
  - `X-Judo-SignedIdentifier: target.__signedIdentifier`
  - `X-Judo-Mask: queryCustomizer._mask ?? '{}'` (defaults to empty mask)
- Response is deserialized via `StoredSerializer.deserialize(data)`
- Error codes documented: 400, 401, 403

**Given** a ClassType where `isUpdatable == false`
**Then** no `update()` method is generated

### Feature: Validate update (dry-run)

**Given** a ClassType where `isUpdateValidatable == true`
**When** the generator processes the service templates
**Then** a `validateUpdate()` method is generated

**Scenario: Service interface**
```typescript
validateUpdate(
  target: ClassNameStored,
  queryCustomizer?: CommandQueryCustomizer
): Promise<JudoRestResponse<ClassNameStored>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<ClassType>/~validate`
- Request body: `StoredSerializer.serialize(target)`
- Headers: `X-Judo-SignedIdentifier: target.__signedIdentifier`
- Response is deserialized via `StoredSerializer.deserialize(data)`
- Error codes documented: 400, 401, 403

**Given** a ClassType where `isUpdateValidatable == false`
**Then** no `validateUpdate()` method is generated

### Feature: Conditional generation matrix

The following table summarizes which ClassType capability flags control which CRUD methods:

| Method | Metamodel Flag | Derived From |
|---|---|---|
| `getTemplate()` | `ClassType.isTemplateable` | `ClassBehaviourType.TEMPLATE` in `behaviours` |
| `refresh()` | `ClassType.isMapped` | `ClassType.isMapped` attribute (default: `true`) |
| `delete()` | `ClassType.isDeletable` | `ClassBehaviourType.DELETE` in `behaviours` |
| `update()` | `ClassType.isUpdatable` | `ClassBehaviourType.UPDATE` in `behaviours` |
| `validateUpdate()` | `ClassType.isUpdateValidatable` | `ClassBehaviourType.VALIDATE_UPDATE` in `behaviours` |

### Feature: Default command mask

**Given** an `update()` or `create()` call
**When** no `queryCustomizer._mask` is provided
**Then** the default mask `'{}'` is used in the `X-Judo-Mask` header
**And** the constant `DEFAULT_COMMAND_MASK = '{}'` is defined at the top of each impl file

### Feature: Signed identifier protocol

**Given** any CRUD operation targeting a stored instance
**When** the Axios implementation sends the request
**Then** the `X-Judo-SignedIdentifier` header is set to `target.__signedIdentifier`
**And** this identifier is opaque to the client (backend-signed, changes after mutations)
**And** no request body identifier field is used — identification is purely header-based

### Feature: Error response contract

**Given** any CRUD operation that fails
**When** the backend returns an error response

**Scenario: Validation error (400)**
**Then** `AxiosError.data` contains `Array<FeedbackItem>` with field-level error details

**Scenario: Authentication failure (401)**
**Then** `AxiosError.data` contains `Array<FeedbackItem>` indicating auth failure

**Scenario: Authorization failure (403)**
**Then** `AxiosError.data` contains `Array<FeedbackItem>` indicating insufficient permissions
