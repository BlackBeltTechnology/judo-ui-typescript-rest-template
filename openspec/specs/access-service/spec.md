# Access Service

Generates a singleton `AccessService` interface and `AccessServiceImpl` that serves as the application entry point, providing principal retrieval, OIDC metadata, file upload/download, and instance finders for access relations.

## Perspectives

- **Template developer**: Authors `accessService.ts.hbs` and `accessServiceImpl.ts.hbs` templates that use `application.principal` and `getAccessRelationsTypes(application)` to conditionally emit methods.
- **Consumer**: Injects `AccessService` as the root entry point to authenticate, discover security configuration, manage file attachments, and locate specific instances via access relations.
- **Generator framework**: Produces exactly one service interface and one implementation file per application (singleton, not iterable).

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-service/src/main/resources/data-service/accessService.ts.hbs` |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/accessServiceImpl.ts.hbs` |
| Helper | `UiServiceHelper.java` — `getAccessRelationsTypes()` |
| YAML | Service and Axios module YAML descriptors (applicationBased: true) |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-service/AccessService.ts` | Singleton | 1 |
| `data-axios/AccessServiceImpl.ts` | Singleton | 1 |

---

## Features

### Feature: Get principal (authenticated user)

**Given** an application where `application.principal != null`
**When** the generator processes the access service templates
**Then** a `getPrincipal()` method is generated

**Scenario: Service interface**
```typescript
getPrincipal(): Promise<JudoRestResponse<PrincipalStored>>;
```

**Scenario: Axios implementation**
- HTTP method: `GET`
- Path: `<actorPath>/<ActorPackage>/<ActorName>/~principal`
- Response: deserialized principal instance
- Error codes: 401, 403

**Given** an application where `application.principal == null`
**Then** no `getPrincipal()` method is generated

### Feature: Get metadata (OIDC/OAuth2 configuration)

**Given** any application
**When** the generator processes the access service templates
**Then** a `getMetaData()` method is always generated (unconditional)

**Scenario: Service interface**
```typescript
getMetaData(): Promise<JudoRestResponse<JudoMetaData>>;
```

**Scenario: Axios implementation**
- HTTP method: `GET`
- Path: `<actorPath>/<ActorPackage>/<ActorName>/~meta`
- Response: `JudoMetaData` containing array of `JudoMetaDataSecurity` objects
- Error codes: 401, 403

### Feature: File upload (two-phase protocol)

**Given** any application
**When** the generator processes the access service templates
**Then** an `uploadFile()` method is always generated (unconditional)

**Scenario: Service interface**
```typescript
uploadFile(attributePath: string, file: File): Promise<string>;
```

**Scenario: Axios implementation — Phase 1: Obtain upload token**
- HTTP method: `POST`
- Path: `<actorPath>/<attributePath>/~upload-token`
- Response: `JudoToken` with `token` field
- Throws error if token response is falsy

**Scenario: Axios implementation — Phase 2: Upload file**
- HTTP method: `POST`
- Path: `<filePath>/upload` (uses `axiosProvider.getFilePath()`, not `getBasePath()`)
- Content-Type: `multipart/form-data`
- Headers: `X-Token: <token from phase 1>`
- Body: `FormData` with file appended using `file.name` as field key
- Returns full response on HTTP 200; throws on other status codes

### Feature: File download

**Given** any application
**When** the generator processes the access service templates
**Then** a `downloadFile()` method is always generated (unconditional)

**Scenario: Service interface**
```typescript
downloadFile(downloadToken: string, disposition: 'inline' | 'attachment'): Promise<any>;
```

**Scenario: Axios implementation**
- HTTP method: `GET`
- Path: `<filePath>/download?disposition=<disposition>` (uses `axiosProvider.getFilePath()`)
- Response type: `blob`
- Headers: `X-Token: <downloadToken>`
- The `disposition` parameter controls whether the browser displays inline or triggers a download

### Feature: Find instance by identifier

**Given** access relations obtained via `getAccessRelationsTypes(application)` (relations where `isAccess == true`)
**And** a relation where `isListable == true` and `isCollection == true`
**When** the generator processes the access service templates
**Then** a `findInstanceOf<RelationName>(identifier, mask?)` method is generated

**Scenario: Service interface**
```typescript
findInstanceOf<RelationName>(identifier: string, mask?: string): Promise<<TargetStored> | undefined>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<ActorPackage>/<ActorName>/<relationName>/~list`
- Request body:
  ```json
  {
    "_identifier": "<identifier>",
    "_mask": "<mask>",
    "_seek": { "limit": 1 }
  }
  ```
- Returns the first (and only) element if the response array has exactly 1 item
- Returns `undefined` if the array is empty or has more than 1 item
- Catches errors and returns `undefined` (non-throwing)

**Given** an access relation where `isListable == false` or `isCollection == false`
**Then** no `findInstanceOf` method is generated for that relation

### Feature: Access relation filtering

**Given** the application model
**When** `getAccessRelationsTypes(application)` is evaluated
**Then** only relations where `isAccess == true` are included
**And** the results are sorted by name

### Feature: Conditional generation matrix

| Method | Condition |
|---|---|
| `getPrincipal()` | `application.principal != null` |
| `getMetaData()` | Always generated |
| `uploadFile()` | Always generated |
| `downloadFile()` | Always generated |
| `findInstanceOf<Rel>()` | `relation.isAccess == true` AND `relation.isListable == true` AND `relation.isCollection == true` |
