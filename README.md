# JUDO UI TypeScript REST Generator

A code generator that produces a complete TypeScript REST client layer — models, service interfaces, and Axios implementations — from a [JUDO UI metamodel](https://github.com/BlackBeltTechnology/judo-meta-ui). It uses **Handlebars templates** (`.hbs`) and **Java helper classes** annotated with `@TemplateHelper` to emit a fully typed data layer including query customizers, serializers, mask builders, and HTTP service implementations.

## Table of Contents

- [Overview](#overview)
- [Modules](#modules)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Build](#build)
  - [Integration Tests](#integration-tests)
- [Maven Plugin Usage](#maven-plugin-usage)
- [Architecture](#architecture)
  - [Module Dependency Chain](#module-dependency-chain)
  - [Code Generation Flow](#code-generation-flow)
  - [Generated Output Structure](#generated-output-structure)
- [Template System](#template-system)
  - [YAML Descriptors](#yaml-descriptors)
  - [Template Modules](#template-modules)
  - [Java Helpers](#java-helpers)
  - [Template Override Mechanism](#template-override-mechanism)
- [Key Conventions](#key-conventions)
  - [TypeScript Type Mapping](#typescript-type-mapping)
  - [Naming Conventions](#naming-conventions)
  - [REST Verb Convention](#rest-verb-convention)
- [License](#license)

## Overview

This project is part of the [JUDO](https://github.com/BlackBeltTechnology) platform. It reads an EMF/Ecore `.model` file describing a UI metamodel and generates a three-layer TypeScript REST client:

| Layer | Description |
|-------|-------------|
| **API** (`data-api/`) | TypeScript interfaces, enums, query customizers, serializers, mask builders |
| **Service** (`data-service/`) | Abstract service interfaces for CRUD and business operations |
| **Axios** (`data-axios/`) | Concrete Axios-based HTTP implementations of the service interfaces |

The Axios layer is optional — you can generate only the API and Service layers and provide your own HTTP implementation.

## Modules

| Module | Artifact ID | Description |
|--------|-------------|-------------|
| **Commons** | `judo-ui-typescript-rest-commons` | Shared Java helpers for naming (`classDataName`, `serviceClassName`, etc.) |
| **API** | `judo-ui-typescript-rest-api` | Templates for models, enums, query customizers, serializers, mask builders, and common utilities |
| **Service** | `judo-ui-typescript-rest-service` | Templates for service interfaces (`AccessService`, class services, relation services) |
| **Axios** | `judo-ui-typescript-rest-axios` | Templates for Axios-based service implementations |
| **ITest** | `judo-ui-typescript-rest-itest` | Integration tests — generates TypeScript from a test model, then runs Vitest and `tsc` |

All template modules are packaged as **OSGi bundles** (not plain JARs), loaded dynamically by the generator framework.

## Getting Started

### Prerequisites

- **Java 21+**
- **Maven 3.8+** (or use the included `./mvnw` wrapper)

For integration tests only:
- Node.js 18.14.2 and pnpm 7.27.1 are automatically installed by `frontend-maven-plugin`

### Build

```bash
# Full build (compile + test + install)
./mvnw clean install

# Build without submodules (parent POM only)
./mvnw clean install -DskipModules=true

# Build with CI-friendly version
./mvnw -B -Drevision=1.0.0.20260220_XXXXXX_hash_develop clean install
```

### Integration Tests

The itest module generates TypeScript from a test model, then validates it with Vitest and TypeScript compilation:

```bash
cd judo-ui-typescript-rest-itest/ActionGroupTest/action_group_test__god
../../mvnw clean verify
```

**ITest pipeline:** Maven generates TypeScript via `judo-ui-generator-maven-plugin` &rarr; installs Node 18.14.2 + pnpm 7.27.1 via `frontend-maven-plugin` &rarr; `pnpm install` &rarr; `pnpm run format` &rarr; `pnpm test` &rarr; `pnpm run build:ci`.

## Maven Plugin Usage

Add the `judo-ui-generator-maven-plugin` to your project to generate the TypeScript REST client:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <judo-generator-commons-version>1.0.0.20260219_192724_142f7c92_develop</judo-generator-commons-version>
        <judo-meta-ui-version>1.1.0.20260220_043758_d4abbddd_develop</judo-meta-ui-version>
        <judo-ui-typescript-rest-version><!-- template version --></judo-ui-typescript-rest-version>

        <model-name>MyModel</model-name>
        <actor-fq-name>MyModel::Admin</actor-fq-name>
        <model-file>${project.basedir}/model/${model-name}-ui.model</model-file>
        <generation-target>${project.basedir}/target/generated</generation-target>
    </properties>

    <build>
        <plugins>
            <plugin>
                <groupId>hu.blackbelt.judo.meta</groupId>
                <artifactId>judo-ui-generator-maven-plugin</artifactId>
                <version>${judo-meta-ui-version}</version>
                <executions>
                    <execution>
                        <id>generate-typescript-rest</id>
                        <phase>generate-resources</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <uris>
                                <uri>mvn:hu.blackbelt.judo.generator:judo-ui-typescript-rest-api:${judo-ui-typescript-rest-version}</uri>
                                <uri>mvn:hu.blackbelt.judo.generator:judo-ui-typescript-rest-service:${judo-ui-typescript-rest-version}</uri>
                                <uri>mvn:hu.blackbelt.judo.generator:judo-ui-typescript-rest-axios:${judo-ui-typescript-rest-version}</uri>
                            </uris>
                            <type>ui-typescript-rest</type>
                            <applications>${actor-fq-name}</applications>
                            <ui>${model-file}</ui>
                            <destination>${generation-target}/src/services</destination>
                        </configuration>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                        <groupId>hu.blackbelt.judo.meta</groupId>
                        <artifactId>hu.blackbelt.judo.meta.ui.model</artifactId>
                        <version>${judo-meta-ui-version}</version>
                    </dependency>
                    <dependency>
                        <groupId>hu.blackbelt.judo.generator</groupId>
                        <artifactId>judo-generator-commons</artifactId>
                        <version>${judo-generator-commons-version}</version>
                    </dependency>
                    <dependency>
                        <groupId>hu.blackbelt.judo.generator</groupId>
                        <artifactId>judo-ui-typescript-rest-commons</artifactId>
                        <version>${judo-ui-typescript-rest-version}</version>
                    </dependency>
                    <dependency>
                        <groupId>hu.blackbelt.judo.generator</groupId>
                        <artifactId>judo-ui-typescript-rest-api</artifactId>
                        <version>${judo-ui-typescript-rest-version}</version>
                    </dependency>
                    <dependency>
                        <groupId>hu.blackbelt.judo.generator</groupId>
                        <artifactId>judo-ui-typescript-rest-service</artifactId>
                        <version>${judo-ui-typescript-rest-version}</version>
                    </dependency>
                    <dependency>
                        <groupId>hu.blackbelt.judo.generator</groupId>
                        <artifactId>judo-ui-typescript-rest-axios</artifactId>
                        <version>${judo-ui-typescript-rest-version}</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

> **Tip:** The `axios` module can be omitted if you plan to provide your own implementation of the `api` and `service` interfaces.

The `judo-ui-generator-maven-plugin` documentation is available in the [plugin repository](https://github.com/BlackBeltTechnology/judo-meta-ui/tree/develop/generator-maven-plugin).

## Architecture

### Module Dependency Chain

```
commons  <--  api      (model types, enums, query customizers, serializers, masks)
commons  <--  service  (service interfaces)
               axios   (Axios implementations of service interfaces)
```

### Code Generation Flow

1. `judo-ui-generator-maven-plugin` reads a `.model` file (EMF/Ecore UI metamodel)
2. Loads template bundles via Maven URIs (`mvn:hu.blackbelt.judo.generator:judo-ui-typescript-rest-api:${revision}`)
3. Scans each bundle for `ui-typescript-rest.yaml` descriptors in `src/main/resources/`
4. For each template entry, evaluates `factoryExpression` (SpEL) to get iterable elements
5. For each element, evaluates `pathExpression` to determine the output file path
6. Handlebars processes the `.hbs` template with Java `@TemplateHelper` static methods available as helpers
7. Output is written to the destination directory

### Generated Output Structure

```
src/services/
  data-api/                                    # from api module
    common/
      index.ts                                 # barrel export
      JudoIdentifiable.ts                      # JudoIdentifiable<T> interface
      JudoStored.ts                            # JudoStored<T> with __entityType, __version, etc.
      OrderingType.ts                          # ASC/DESC enum
      QueryCustomizer.ts                       # base query customizer type
      Seek.ts                                  # pagination support
      Serializer.ts                            # base serializer interface
      utils.ts                                 # utility functions
      errors/                                  # FeedbackItem, FeedbackLevel
      files/                                   # JudoDownloadFile, JudoUploadFile, JudoUploadData
      operations/                              # filter operations (_BooleanOperation, _StringOperation, etc.)
      security/                                # JudoMetaData, JudoToken
    model/
      <ClassName>.ts                           # one per ClassType (interface + Stored + Attributes + Relations)
      <EnumName>.ts                            # one per EnumerationType
    rest/
      <ClassName>QueryCustomizer.ts            # typed query customizer per class
      <ClassName>QueryCustomizerSerializer.ts  # query customizer serialization
      <ClassName>Serializer.ts                 # data serializer with singleton pattern
      <ClassName>MaskBuilder.ts                # field mask builder per class
      FilterBy<DataType>.ts                    # filter types per filterable data type
      headers.ts                               # HTTP header constants
      requestResponse.ts                       # JudoRestResponse<T>
      serialization.ts                         # shared date/time serialization utils
      MaskBuilder.ts                           # base MaskBuilder class

  data-service/                                # from service module
    index.ts                                   # barrel export
    AccessService.ts                           # entry point: getPrincipal, getMetaData, file operations
    <ClassName>Service.ts                      # per-class service interface (CRUD, operations)
    <Owner>ServiceFor<Rel>.ts                  # per-relation service interface

  data-axios/                                  # from axios module
    AxiosProvider.ts                           # AxiosProvider interface + init data type
    JudoAxiosProvider.ts                       # singleton provider implementation
    JudoAxiosService.ts                        # base Axios service class
    AccessServiceImpl.ts                       # Axios implementation of AccessService
    <ClassName>ServiceImpl.ts                  # Axios implementation of class services
    <Owner>ServiceFor<Rel>Impl.ts             # Axios implementation of relation services
```

## Template System

### YAML Descriptors

Each module has a `ui-typescript-rest.yaml` in `src/main/resources/` that registers its templates. The `type` field in the generator plugin config must be `ui-typescript-rest` — this matches the YAML file names.

Each entry specifies:

| Field | Description |
|-------|-------------|
| `name` | Unique identifier (used for override matching) |
| `templateName` | Path to the `.hbs` template file |
| `factoryExpression` | SpEL expression returning an iterable — template runs once per element |
| `pathExpression` | SpEL expression determining the output file path |
| `conditionExpression` | SpEL expression returning boolean — skips template if false |
| `applicationBased` | If true, provides `#application` in template context |
| `actorTypeBased` | If true (default), iterates over actor types |
| `templateContext` | Additional named SpEL variables injected into scope |

**Example:**

```yaml
templates:
  - name: class-model
    templateName: model/class.ts.hbs
    pathExpression: "'data-api/model/' + #classDataName(#self, '') + '.ts'"
    factoryExpression: "#getClassTypes(#application)"
    applicationBased: true
    actorTypeBased: true
```

### Template Modules

| Module | Templates | Description |
|--------|-----------|-------------|
| **API** | 28 entries | Common types, model interfaces, enums, query customizers, serializers, mask builders, filter types |
| **Service** | 4 entries | AccessService, per-class services, per-relation services, barrel index |
| **Axios** | 6 entries | AxiosProvider, JudoAxiosService, AccessServiceImpl, per-class and per-relation implementations |

All templates start with `{{> fragment.header.hbs }}` to include a standard generated-source header comment.

### Java Helpers

Five Java helper classes provide the logic used by both Handlebars templates and SpEL expressions:

| Class | Module | Key Methods |
|-------|--------|-------------|
| `UiCommonsHelper` | commons | `classDataName`, `serviceClassName`, `serviceRelationName`, `restParamName`, `firstToUpper`, `firstToLower` |
| `UiGeneralHelper` | api | `getClassTypes`, `getEnumerationTypes`, `typescriptType`, `getImportTokens`, `serializePrimitive`, `deserializePrimitive`, `generateBuilderProps` |
| `StoredVariableHelper` | api | `isDebugPrint` (context accessor for template parameters via `ThreadLocalContextHolder`) |
| `UiAxiosHelper` | axios | `restPath`, `relationRestPath`, `operationRestPath`, `rootPathForApp`, `hasFaults` |
| `UiServiceHelper` | service | `getAccessRelationsTypes`, `getNotAccessRelationsTypes`, `joinedTokensForApiImport`, `operationsOrderedByName`, `isOperationInputValidateable` |

All helpers are annotated with `@TemplateHelper`, extend `StaticMethodValueResolver`, and expose `public static` methods. These methods are available as:
- **Handlebars helpers:** `{{methodName arg}}`
- **SpEL functions:** `#methodName(arg)`

### Template Override Mechanism

Downstream projects can customize generated output without modifying the original templates:

- **Template-level:** The `ChainedURLTemplateLoader` checks for `template.override.hbs` before loading `template.hbs`
- **YAML-level:** Override entries match by `name` field; use `exclude: true` to remove a template entirely

## Key Conventions

### TypeScript Type Mapping

| Metamodel Type | TypeScript Type |
|----------------|-----------------|
| `StringType` | `string` |
| `NumericType` | `number` |
| `BooleanType` | `boolean` |
| `DateType` | `string` |
| `TimeType` | `string` |
| `TimestampType` | `string` |
| `EnumerationType` | Generated enum |
| `BinaryType` | Binary handling |

### Naming Conventions

| Pattern | Example Input | Output |
|---------|---------------|--------|
| `classDataName(type, "")` | `MyModule::User` | `MyModuleUser` |
| `classDataName(type, "Stored")` | `MyModule::User` | `MyModuleUserStored` |
| `serviceClassName(type)` | `MyModule::User` | `MyModuleUserService` |
| `serviceRelationName(rel)` | Owner=`Order`, Rel=`items` | `OrderServiceForItems` |

### REST Verb Convention

All REST endpoints use tilde-prefixed verbs:

| Verb | HTTP Method | Purpose |
|------|-------------|---------|
| `~get` | POST | Fetch single entity by identifier |
| `~list` | POST | List/query a collection |
| `~create` | POST | Create a new instance |
| `~update` | POST | Update an existing instance |
| `~delete` | POST | Delete an instance |
| `~template` | GET | Get a blank template instance |
| `~range` | POST | Get selectable options for an association |
| `~set` | POST | Set association target(s) |
| `~unset` | POST | Clear an association |
| `~add` | POST | Add to a collection association |
| `~remove` | POST | Remove from a collection association |
| `~validate` | POST | Server-side validation |
| `~export` | POST | Export data (blob response) |
| `~principal` | GET | Get the authenticated user |
| `~meta` | GET | Get application metadata |
| `~upload-token` | POST | Obtain a file upload token |

### Other Conventions

- **Draft identifiers:** Client-generated IDs use a `draft:` prefix, stripped during serialization, auto-generated on deserialization with `draftIdentifierPrefix + uuid`
- **Singleton serializers:** Generated serializers use `_instance` + `getInstance()` to avoid circular reference issues
- **`TRANSFER_SKIP_SEGMENT`:** The segment `_default_transferobjecttypes` is filtered out when constructing REST paths
- **SpEL `#` prefix:** References helper methods and context variables in YAML expressions

## License

This project is licensed under the [Eclipse Public License 2.0](LICENSE.txt) with a secondary license option under GPL-2.0 with Classpath Exception.

Copyright 2018-2023 BlackBelt Technology.
