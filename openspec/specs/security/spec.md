# Security

Generates TypeScript types for OIDC/OAuth2 metadata discovery, authentication claims, file operation tokens, and the security infrastructure that enables frontend authentication integration.

## Perspectives

- **Template developer**: Authors security type templates in `common/security/` that produce static type definitions for the authentication contract.
- **Consumer**: Uses `JudoMetaData` (from `AccessService.getMetaData()`) to configure OIDC client libraries, `JudoToken` for file operations, and `Claim` types to understand user identity fields.
- **Generator framework**: Produces singleton security type files unconditionally (no metamodel conditions).

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/security/judoMetaData.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/security/judoMetaDataSecurity.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/security/judoToken.ts.hbs` |
| Template | `judo-ui-typescript-rest-api/src/main/resources/common/security/index.ts.hbs` |
| Metamodel | `Authentication` class with `realm` and `claims` |
| Metamodel | `Claim` class with `type: ClaimType` and `attributeType: AttributeType` |
| Metamodel | `ClaimType` enum: `UNDEFINED`, `EMAIL`, `USERNAME` |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-api/common/security/JudoMetaData.ts` | Singleton | 1 |
| `data-api/common/security/JudoMetaDataSecurity.ts` | Singleton | 1 |
| `data-api/common/security/JudoToken.ts` | Singleton | 1 |
| `data-api/common/security/index.ts` | Barrel export | 1 |

---

## Features

### Feature: OIDC/OAuth2 metadata type

**Given** the security templates
**When** the generator produces `JudoMetaData.ts`
**Then** the file contains:

```typescript
interface JudoMetaData {
  security: Array<JudoMetaDataSecurity>;
}
```

**And** `JudoMetaDataSecurity` contains the full OIDC discovery configuration:

| Field | Type | Purpose |
|---|---|---|
| `tokenEndpoint` | `string` | OAuth2 token endpoint URL |
| `clientId` | `string` | OIDC/OAuth2 client ID |
| `openIdConfigurationUrl` | `string` | OIDC discovery endpoint (`.well-known/openid-configuration`) |
| `authEndpoint` | `string` | Authorization endpoint URL |
| `logoutEndpoint` | `string` | Logout/revocation endpoint URL |
| `name` | `string` | Provider name (human-readable) |
| `defaultScopes` | `string` | Default OAuth2 scopes (space-separated) |
| `clientBaseUrl` | `string` | Client application base URL (for redirect URIs) |
| `issuer` | `string` | OIDC issuer identifier |

### Feature: File operation token type

**Given** the security templates
**When** the generator produces `JudoToken.ts`
**Then** the file contains:

```typescript
interface JudoToken {
  token: string;
}
```

**And** this type is used in:
- Upload flow: Phase 1 response from `~upload-token` returns `JudoToken`
- Download flow: `downloadToken` parameter to `downloadFile()` is the `token` value
- Upload Phase 2: `X-Token` header carries the token value

### Feature: Security metadata retrieval

**Given** an initialized `AccessService`
**When** the consumer calls `getMetaData()`
**Then** a `GET` request to `<actorPath>/~meta` returns `JudoMetaData`
**And** the `security` array may contain multiple providers (multi-realm support)
**And** each provider's fields enable a complete OIDC client configuration:
  - Discovery URL for automatic endpoint resolution
  - Direct endpoint URLs as fallback
  - Client credentials for authentication requests
  - Base URL for constructing redirect URIs

### Feature: Authentication realm (metamodel context)

**Given** the `Authentication` element on the `Application`
**When** `application.authentication != null`
**Then** the authentication realm name is available via `authentication.realm`
**And** claims define which user attributes map to identity fields

### Feature: Claim type mapping (metamodel context)

**Given** the `Authentication.claims` collection
**When** claims are defined
**Then** each `Claim` maps a `ClaimType` to an `AttributeType` on the principal:

| ClaimType | Meaning |
|---|---|
| `UNDEFINED` | Unspecified claim type |
| `EMAIL` | Maps to user's email attribute |
| `USERNAME` | Maps to user's username attribute |

### Feature: Token-based file security

**Given** the two-phase file upload protocol
**When** the consumer uploads a file

**Scenario: Token acquisition**
- POST to `<attributePath>/~upload-token` (authenticated)
- Response: `JudoToken` with time-limited, single-use token

**Scenario: Token usage**
- The token is sent as `X-Token` header in the upload/download request
- This decouples file storage authentication from the main API authentication
- File requests go to `axiosProvider.getFilePath()` (potentially a different server)

**Given** the file download protocol
**When** the consumer downloads a file
**Then** the `downloadToken` (obtained previously) is sent as `X-Token` header
**And** `disposition` controls browser behavior: `'inline'` for display, `'attachment'` for download

### Feature: Security barrel export

**Given** the security index template
**When** the generator produces `data-api/common/security/index.ts`
**Then** it re-exports all security types: `JudoMetaData`, `JudoMetaDataSecurity`, `JudoToken`
