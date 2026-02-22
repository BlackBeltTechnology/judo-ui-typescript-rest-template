# Relation Management

Generates service interfaces and Axios implementations for managing relations between ClassType instances: listing, creating, setting, unsetting, adding, removing, deleting, exporting, and range-querying related objects. Supports two levels of nesting (class → relation → relation's relations).

## Perspectives

- **Template developer**: Authors templates that iterate `classType.relationsOrderedByName` and `relation.target.relationsOrderedByName`, conditionally emitting methods based on relation capability flags.
- **Consumer**: Calls typed relation methods on `<Class>Service` (per-class scope) or `<Owner>ServiceFor<Relation>` (per-relation scope) to manage object graphs.
- **Generator framework**: Produces per-class and per-relation service files from two distinct factory expressions.

## Source Artifacts

| Artifact | Location |
|----------|----------|
| Template | `judo-ui-typescript-rest-service/src/main/resources/data-service/classService.ts.hbs` (relation sections) |
| Template | `judo-ui-typescript-rest-service/src/main/resources/data-service/relationService.ts.hbs` |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/classServiceImpl.ts.hbs` (relation sections) |
| Template | `judo-ui-typescript-rest-axios/src/main/resources/data-axios/relationServiceImpl.ts.hbs` |
| Helper | `UiServiceHelper.java` — `getAccessRelationsTypes()`, `getNonAccessRelationsTypes()` |
| Helper | `UiAxiosHelper.java` — `restPath()`, `relationRestPath()` |
| YAML | All three module YAML descriptors |

## Generated Output

| File Pattern | Generated From | Count |
|---|---|---|
| `data-service/<ClassName>Service.ts` | Per-class (relation methods section) | N |
| `data-service/<Owner>ServiceFor<Relation>.ts` | One per RelationType in the application | R |
| `data-axios/<ClassName>ServiceImpl.ts` | Per-class (relation impl section) | N |
| `data-axios/<Owner>ServiceFor<Relation>Impl.ts` | One per RelationType | R |

---

## Two Service Scopes

The generator produces relation methods in **two parallel scopes**:

```
┌──────────────────────────────────┐     ┌──────────────────────────────────────┐
│    ClassService (per-class)      │     │  OwnerServiceForRelation (per-rel)   │
│                                  │     │                                      │
│  listRelation1(target, qc)      │     │  list(owner, qc)                     │
│  getRelation2(target, qc)       │     │  refresh(owner, qc)                  │
│  getRangeForRelation1(owner, qc)│     │  getRange(owner, qc)                 │
│  createRelation1(owner, target) │     │  create(owner, target)               │
│  setRelation2(owner, selected)  │     │  set(owner, selected)                │
│  deleteRelation1(target)        │     │  delete(target)                      │
│  ...                            │     │  update(target, qc)                  │
│                                  │     │  validateUpdate(owner, target)       │
│  (1 level deep only)            │     │  list<TargetRel>(owner, qc)          │
│                                  │     │  ...                                 │
│                                  │     │  (2 levels deep: target's relations) │
└──────────────────────────────────┘     └──────────────────────────────────────┘
```

The **ClassService** exposes relation methods with relation-name-suffixed method names (e.g., `listOrders()`, `getRangeForOrders()`).

The **OwnerServiceForRelation** exposes the same operations with generic method names (e.g., `list()`, `getRange()`), plus nested relation operations on the target type (2 levels deep).

---

## Features

### Feature: List collection relation

**Given** a relation where `isRefreshable == true` and `isCollection == true`

**Scenario: ClassService scope**
**Then** a `list<RelationName>(target, queryCustomizer?, headers?)` method is generated
- Returns `Promise<JudoRestResponse<Array<<TargetStored>>>>`

**Scenario: OwnerServiceForRelation scope (requires `isListable == true`)**
**Then** a `list(owner?, queryCustomizer?, headers?)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/<relationName>/~list`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier` (for non-access relations)
- Request body: serialized QueryCustomizer
- Response: array of deserialized stored instances

### Feature: Get single relation

**Given** a relation where `isRefreshable == true` and `isCollection == false`

**Scenario: ClassService scope**
**Then** a `get<RelationName>(target, queryCustomizer?, headers?)` method is generated
- Returns `Promise<JudoRestResponse<<TargetStored> | null>>`

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/<relationName>/~get`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier`
- Empty string response (`typeof data === 'string' && !data.length`) is deserialized as `null`

### Feature: Range query (selection picker)

**Given** a relation

**Scenario: ClassService scope — always generated**
**Then** a `getRangeFor<RelationName>(owner?, queryCustomizer?, headers?)` method is generated unconditionally for every relation

**Scenario: OwnerServiceForRelation scope — conditional**
**When** `relation.isRangeable == true`
**Then** a `getRange(owner?, queryCustomizer?, headers?)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/<relationName>/~range`
- Request body: `{ owner: serialized_owner, queryCustomizer: serialized_qc }`
- Headers: `X-Judo-MarkSelectedRangeItems: 'true'` — marks already-attached items in the response as `__selected == true`
- Response: array of deserialized stored instances with `__selected` flag

### Feature: Create related item

**Given** a relation where `isCreatable == true`

**Scenario: Service interface**
```typescript
// ClassService scope:
create<RelationName>(owner: OwnerStored, target: TargetType, queryCustomizer?: CommandQueryCustomizer): Promise<JudoRestResponse<TargetStored>>;

// OwnerServiceForRelation scope:
create(owner?: OwnerStored, target: TargetType, queryCustomizer?: CommandQueryCustomizer): Promise<JudoRestResponse<TargetStored>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path (non-access): `<actorPath>/<OwnerType>/~update/<relationName>/~create`
- Path (access): `<actorPath>/<OwnerType>/<relationName>/~create`
- Headers: `X-Judo-SignedIdentifier` + `X-Judo-Mask` (default `'{}'`)
- Request body: serialized target instance

### Feature: Validate create (dry-run)

**Given** a relation where `isCreateValidatable == true`
**Then** a validate method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path (non-access): `<actorPath>/<OwnerType>/~update/<relationName>/~validate`
- Path (access): `<actorPath>/<OwnerType>/<relationName>/~validate`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier`
- Returns deserialized input type (not Stored)

### Feature: Set relation (replace association target)

**Given** a relation where `isSetable == true`

**Scenario: Service interface (collection)**
```typescript
set<RelationName>(owner: OwnerStored, selected: Array<TargetStored>): Promise<JudoRestResponse<void>>;
```

**Scenario: Service interface (single)**
```typescript
set<RelationName>(owner: OwnerStored, selected: TargetStored): Promise<JudoRestResponse<void>>;
```

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/~update/<relationName>/~set`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier`
- Request body: serialized selected array (or single object)

### Feature: Unset relation (clear association)

**Given** a relation where `isUnsetable == true`
**Then** an `unset<RelationName>(owner)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/~update/<relationName>/~unset`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier`
- Request body: `undefined`

### Feature: Add to collection

**Given** a relation where `isAddable == true`
**Then** an `add<RelationName>(owner, selected: Array<TargetStored>)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/~update/<relationName>/~add`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier`
- Request body: serialized array of selected items

### Feature: Remove from collection

**Given** a relation where `isRemovable == true`
**Then** a `remove<RelationName>(owner, selected: Array<TargetStored>)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/~update/<relationName>/~remove`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier`
- Request body: serialized array of selected items

### Feature: Delete related item

**Given** a relation where `isDeletable == true`
**Then** a `delete<RelationName>(target: TargetStored)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<TargetType>/~delete`
- Headers: `X-Judo-SignedIdentifier: target.__signedIdentifier`
- Note: uses the TARGET type path, not the owner path

### Feature: Export relation data

**Given** a relation where `isExportable == true`
**Then** an `export<RelationName>(owner?, queryCustomizer?)` method is generated

**Scenario: Axios implementation**
- HTTP method: `POST`
- Path: `<actorPath>/<OwnerType>/<relationName>/~export`
- Response type: `blob`
- Headers: `X-Judo-SignedIdentifier: owner.__signedIdentifier` (if owner provided)

### Feature: Get template for related type

**Given** a relation where `target.isTemplateable == true`
**Then** a `getTemplateFor<RelationName>()` method is generated

**Scenario: Axios implementation**
- HTTP method: `GET`
- Path: `<actorPath>/<TargetType>/~template`
- Response deserialized via base serializer (not Stored)

### Feature: Refresh in relation scope

**Given** a relation in the OwnerServiceForRelation scope where `isRefreshable == true`
**Then** a `refresh(owner?, queryCustomizer?, headers?)` method is generated

**Scenario: Access singleton relation refresh**
**When** `relation.isAccess == true` and `relation.isCollection == false`
**Then** an additional `refreshFor<RelationName>(queryCustomizer?, headers?)` method is generated (no owner parameter)

### Feature: Update in relation scope

**Given** a relation in the OwnerServiceForRelation scope where `isUpdatable == true`
**Then** an `update(target: Partial<TargetStored>, queryCustomizer?: CommandQueryCustomizer)` method is generated

### Feature: Validate update in relation scope

**Given** a relation in the OwnerServiceForRelation scope where `isUpdateValidatable == true`
**Then** a `validateUpdate(owner?, target: Partial<TargetStored>, queryCustomizer?)` method is generated

---

## Access vs Non-Access Relation Behavior

Relations where `isAccess == true` are entry points (top-level navigation). They behave differently from regular relations:

| Variation | `isAccess == true` | `isAccess == false` |
|---|---|---|
| Owner type | `JudoIdentifiable<any>` | Concrete `<Owner>Stored` |
| Owner in create/set/add/remove | No owner parameter | `owner: <Owner>Stored` required |
| Create path | `/<Owner>/<rel>/~create` | `/<Owner>/~update/<rel>/~create` |
| Validate create path | `/<Owner>/<rel>/~validate` | `/<Owner>/~update/<rel>/~validate` |
| Set/Add/Remove headers | No `X-Judo-SignedIdentifier` | `X-Judo-SignedIdentifier: owner.__signedIdentifier` |
| GetRange owner | No owner parameter | `owner: <Owner>Stored` optional |
| ValidateUpdate owner | No owner parameter | `owner: <Owner>Stored` optional |

---

## Nested Relations (2-Level Depth)

The `OwnerServiceForRelation` template additionally generates methods for the target type's own relations (one level deeper). This is produced by iterating `relation.target.relationsOrderedByName`.

### Feature: Nested relation combinatorics

**Given** a relation `Owner.orders → Order` where `Order` has a relation `Order.items → OrderItem`
**When** the generator processes `relationService.ts.hbs`
**Then** the `OwnerServiceForOrders` interface includes:
- Direct methods: `list()`, `create()`, `delete()`, etc. for Order
- Nested methods: `listItems(owner: OrderStored, qc?)`, `getRangeForItems(owner: OrderStored, qc?)`, `createItems(owner: OrderStored, target)`, etc. for OrderItem

The nested methods follow the same conditional generation rules as the primary relation methods, but applied to the target relation's flags:

| Nested Method | Condition |
|---|---|
| `list<TargetRel>(owner, qc)` | `targetRelation.isListable == true` AND `targetRelation.isCollection == true` |
| `get<TargetRel>(owner, qc)` | `targetRelation.isListable == true` AND `targetRelation.isCollection == false` |
| `export<TargetRel>(owner, qc)` | `targetRelation.isExportable == true` |
| `getRangeFor<TargetRel>(owner, qc)` | `targetRelation.isRangeable == true` |
| `getTemplateFor<TargetRel>()` | `targetRelation.target.isTemplateable == true` |
| `create<TargetRel>(owner, target)` | `targetRelation.isCreatable == true` |
| `validateCreate<TargetRel>(owner, target)` | `targetRelation.isCreateValidatable == true` |
| `delete<TargetRel>(target)` | `targetRelation.isDeletable == true` |
| `update<TargetRel>(owner, target)` | `targetRelation.isUpdatable == true` |
| `validateUpdate<TargetRel>(owner, target)` | `targetRelation.isUpdateValidatable == true` |
| `set<TargetRel>(owner, selected)` | `targetRelation.isSetable == true` |
| `unset<TargetRel>(owner)` | `targetRelation.isUnsetable == true` |
| `add<TargetRel>(owner, selected)` | `targetRelation.isAddable == true` |
| `remove<TargetRel>(owner, selected)` | `targetRelation.isRemovable == true` |

The depth is strictly 2 levels (class → relation → target's relations). No further nesting occurs.

---

## Conditional Generation Matrix (Complete)

| Method | Metamodel Flag | Derived From |
|---|---|---|
| `list` / `list<Rel>` | `isRefreshable` (ClassService) / `isListable` (RelationService) + `isCollection` | `RelationBehaviourType.REFRESH` / `LIST` in behaviours |
| `get` / `get<Rel>` | `isRefreshable` / `isListable` + NOT `isCollection` | Same |
| `getRange` / `getRangeFor<Rel>` | Always (ClassService) / `isRangeable` (RelationService) | `RelationBehaviourType.RANGE` in behaviours |
| `create` / `create<Rel>` | `isCreatable` | `RelationBehaviourType.CREATE` in behaviours |
| `validateCreate` / `validateCreate<Rel>` | `isCreateValidatable` | `RelationBehaviourType.VALIDATE_CREATE` in behaviours |
| `set` / `set<Rel>` | `isSetable` | `RelationBehaviourType.SET` in behaviours |
| `unset` / `unset<Rel>` | `isUnsetable` | `RelationBehaviourType.UNSET` in behaviours |
| `add` / `add<Rel>` | `isAddable` | `RelationBehaviourType.ADD` in behaviours |
| `remove` / `remove<Rel>` | `isRemovable` | `RelationBehaviourType.REMOVE` in behaviours |
| `delete` / `delete<Rel>` | `isDeletable` | `RelationBehaviourType.DELETE` in behaviours |
| `update` | `isUpdatable` | `RelationBehaviourType.UPDATE` in behaviours |
| `validateUpdate` | `isUpdateValidatable` | `RelationBehaviourType.VALIDATE_UPDATE` in behaviours |
| `export` / `export<Rel>` | `isExportable` | `RelationBehaviourType.EXPORT` in behaviours |
| `getTemplate` / `getTemplateFor<Rel>` | `target.isTemplateable` | `ClassBehaviourType.TEMPLATE` in target's behaviours |
| `refresh` | `isRefreshable` | `RelationBehaviourType.REFRESH` in behaviours |
