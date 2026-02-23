# HTTP Infrastructure

Generates the Axios-based HTTP transport layer: provider pattern, base service class, path construction, header constants, and typed request/response wrappers that all generated service implementations depend on.

## Perspectives

- **Template developer**: Authors `AxiosProvider.ts.hbs`, `JudoAxiosProvider.ts.hbs`, `JudoAxiosService.ts.hbs`, `headers.ts.hbs`, and `requestResponse.ts.hbs` templates that form the HTTP foundation.
- **Consumer**: Initializes `JudoAxiosProvider` with an Axios instance and optional path factories, then injects the provider into service implementations. Uses typed `JudoRestResponse` for all API interactions.
- **Generator framework**: Produces singleton infrastructure files (not iterable), using the application context for path defaults.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/axiosProvider.ts.hbs` |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/judoAxiosProvider.ts.hbs` |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/judoAxiosService.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/headers.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/rest/requestResponse.ts.hbs` |
| Helper | `UiAxiosHelper.java` — `restPath()`, `relationRestPath()`, `operationRestPath()` |
| Helper | `UiCommonsHelper.java` — naming, path segment construction |
| YAML | API and Axios module YAML descriptors |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-axios/AxiosProvider.ts` | Singleton | 1 |
| `data-axios/JudoAxiosProvider.ts` | Singleton | 1 |
| `data-axios/JudoAxiosService.ts` | Singleton (abstract base) | 1 |
| `data-api/rest/headers.ts` | Singleton | 1 |
| `data-api/rest/requestResponse.ts` | Singleton | 1 |
| `data-axios/index.ts` | Barrel export | 1 |

---

## Features

### Feature: AxiosProvider interface

**Given** the Axios provider template
**When** the generator produces `data-axios/AxiosProvider.ts`
**Then** the file contains:

```typescript
interface JudoAxiosProviderInitData {
  axios: AxiosInstance;
  basePathFactory?: () => string;   // Custom base path override
  filePathFactory?: () => string;   // Custom file path override
}

interface AxiosProvider {
  init(data: JudoAxiosProviderInitData): void;
  getAxios(): AxiosInstance;
  getBasePath(suffix?: string): string;
  getFilePath(suffix?: string): string;
}
```

### Feature: JudoAxiosProvider implementation

**Given** the provider implementation template
**When** the generator produces `data-axios/JudoAxiosProvider.ts`
**Then** the file contains:
- A `JudoAxiosProvider` class implementing `AxiosProvider`
- `init()` stores the Axios instance and optional path factories, sets default `Content-Type: application/json` for POST requests
- `getAxios()` returns the Axios instance, throws if not initialized
- `getBasePath(suffix?)` returns the base path (from factory or default `/<ApplicationName>`) with trailing slashes removed, plus optional suffix
- `getFilePath(suffix?)` same as `getBasePath()` but using `filePathFactory`
- A singleton export: `const judoAxiosProvider: AxiosProvider = new JudoAxiosProvider()`

### Feature: Default base path

**Given** a `JudoAxiosProvider` initialized without a `basePathFactory`
**When** `getBasePath()` is called
**Then** the default path `/<ApplicationName>` is used (application name from the model)

**Given** a `JudoAxiosProvider` initialized with a custom `basePathFactory`
**When** `getBasePath()` is called
**Then** the factory function is invoked and its return value is used

### Feature: JudoAxiosService abstract base

**Given** the service base template
**When** the generator produces `data-axios/JudoAxiosService.ts`
**Then** the file contains an abstract class with:

```typescript
abstract class JudoAxiosService {
  protected readonly axiosProvider: AxiosProvider;

  constructor(axiosProvider: AxiosProvider);

  protected get axios(): AxiosInstance;  // Delegates to axiosProvider.getAxios()

  protected getBasePath(suffix?: string): string;  // Delegates to axiosProvider

  protected getPathForApp(path?: string): string;
  // Returns: <basePath>/<ModelName><path>

  protected getPathForActor(path?: string): string;
  // Returns: <basePath>/<ModelName>/<ActorPackage>/<ActorSimpleName><path>
}
```

### Feature: REST path construction

**Given** a generated service implementation
**When** constructing REST endpoint paths

**Scenario: Application-level path**
**Then** `getPathForApp(path)` produces: `<basePath>/<modelName>/<path>`

**Scenario: Actor-level path**
**Then** `getPathForActor(path)` produces: `<basePath>/<modelName>/<actorPackage>/<actorSimpleName>/<path>`

**Scenario: Path segment separator**
**When** the path does not start with `/`
**Then** a `/` is prepended before concatenation

**Scenario: TRANSFER_SKIP_SEGMENT filtering**
**When** `restPath()`, `relationRestPath()`, or `operationRestPath()` constructs a path
**Then** the segment `_default_transferobjecttypes` is filtered out from package name tokens

### Feature: Tilde-prefixed REST verbs

**Given** all generated Axios implementations
**When** they construct endpoint paths
**Then** the following verb suffixes are used:

| Verb | Path Suffix | HTTP Method | Purpose |
|---|---|---|---|
| `~get` | `/<Type>/~get` | POST | Refresh/re-read a single instance |
| `~list` | `/<Type>/<rel>/~list` | POST | List collection relation items |
| `~create` | `/<Type>/~update/<rel>/~create` | POST | Create related instance |
| `~update` | `/<Type>/~update` | POST | Update existing instance |
| `~delete` | `/<Type>/~delete` | POST | Delete instance |
| `~template` | `/<Type>/~template` | GET | Get blank template with defaults |
| `~range` | `/<Type>/<rel>/~range` | POST | Range query for selection |
| `~set` | `/<Type>/~update/<rel>/~set` | POST | Set relation target(s) |
| `~unset` | `/<Type>/~update/<rel>/~unset` | POST | Clear relation |
| `~add` | `/<Type>/~update/<rel>/~add` | POST | Add items to collection |
| `~remove` | `/<Type>/~update/<rel>/~remove` | POST | Remove items from collection |
| `~validate` | `/<Type>/~validate` | POST | Validate without persisting |
| `~export` | `/<Type>/<rel>/~export` | POST | Export data as blob |
| `~upload-token` | `<attrPath>/~upload-token` | POST | Get file upload token |
| `~principal` | `<actor>/~principal` | GET | Get authenticated user |
| `~meta` | `<actor>/~meta` | GET | Get OIDC metadata |

### Feature: HTTP header constants

**Given** the headers template
**When** the generator produces `data-api/rest/headers.ts`
**Then** the file exports:

| Constant | Value | Direction | Purpose |
|---|---|---|---|
| `X_JUDO_SIGNED_IDENTIFIER` | `'X-Judo-SignedIdentifier'` | Request | Identifies the target instance (opaque, backend-signed) |
| `X_JUDO_COUNT_RECORDS` | `'X-Judo-CountRecords'` | Request | `'true'` to request total count in response header |
| `X_JUDO_MASK` | `'X-Judo-Mask'` | Request | Field selection mask string |
| `X_JUDO_MARK_SELECTED_RANGE_ITEMS` | `'X-Judo-MarkSelectedRangeItems'` | Request | `'true'` to mark already-attached items in range query |
| `X_JUDO_COUNT` | `'x-judo-count'` | Response | Total record count as string (parse with `parseInt()`) |

### Feature: Typed request/response wrappers

**Given** the request/response template
**When** the generator produces `data-api/rest/requestResponse.ts`
**Then** the file contains:

```typescript
type JudoRequestHeaders = Partial<Record<string, string> & {
  [X_JUDO_COUNT_RECORDS]?: 'true' | 'false';
}>;

interface JudoRestResponse<T = any> extends AxiosResponse {
  data: T;
  headers: (RawAxiosResponseHeaders | AxiosResponseHeaders) & {
    [X_JUDO_COUNT]?: string;  // Total count string
  };
  status: number;
}
```

### Feature: HTTP method convention

**Given** all generated Axios implementations
**When** they make HTTP requests
**Then** the following convention is followed:
- `GET` is used only for: `~template`, `~principal`, `~meta`, `download`
- `POST` is used for all other operations (CRUD, relation management, custom operations, range queries, export, upload)
- This is because query parameters and signed identifiers are passed in headers/body, not URL params

### Feature: Response deserialization pattern

**Given** any Axios implementation method that returns data
**When** the response is received
**Then** the implementation follows this pattern:
```typescript
const { data, ...rest } = await this.axios.post(path, body, config);
return {
  ...rest,
  data: serializer.deserialize(data),
};
```
**And** the original Axios response metadata (headers, status, etc.) is preserved alongside the deserialized data

### Feature: Array response deserialization

**Given** a list/range method returning an array
**When** the response is received
**Then** the implementation checks `Array.isArray(data)` before mapping deserialize
**And** returns empty array `[]` if the response is not an array

### Feature: Null response handling for single relations

**Given** a `get<Relation>` method returning a single item
**When** the response data is an empty string (`typeof data === 'string' && !data.length`)
**Then** the method returns `null`
**And** this handles the backend convention of returning empty string for absent single relations
