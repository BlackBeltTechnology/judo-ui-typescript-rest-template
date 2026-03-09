# commons Specification

## Purpose
Provides shared Java helper utilities used by all other generation modules. Handles naming conventions, string transformations, and EMF model access for converting JUDO UI metamodel elements into TypeScript-compatible identifiers.

## Architecture
Single class `UiCommonsHelper` extending `StaticMethodValueResolver`, annotated with `@TemplateHelper`. All methods are `public static` and callable from Handlebars templates via SpEL. Uses `StringHelper` from `judo-generator-commons` for string operations and EMF `XMIResource` for model element ID extraction.

Key EMF types consumed: `ClassType`, `RelationType`, `DataType`, `EObject`.

## Requirements

### Requirement: Class data name generation
The `classDataName` method SHALL produce a TypeScript-compatible class name by joining namespace segments (split by `::`) and capitalizing the first character, with an optional suffix appended.

#### Scenario: Simple class name without suffix
- **GIVEN** a `ClassType` with name `"MyPackage::MyClass"`
- **WHEN** `classDataName(classType, "")` is called
- **THEN** the result is `"MypackageMyclass"` (segments joined, first char uppercased)

#### Scenario: Class name with suffix
- **GIVEN** a `ClassType` with name `"MyPackage::MyClass"`
- **WHEN** `classDataName(classType, "Stored")` is called
- **THEN** the result is `"MypackageMyclassStored"`

### Requirement: Service class name generation
The `serviceClassName` method SHALL produce a service class name by joining namespace segments, capitalizing the first character, and appending `"Service"`.

#### Scenario: Standard service name
- **GIVEN** a `ClassType` with name `"Order"`
- **WHEN** `serviceClassName(classType)` is called
- **THEN** the result is `"OrderService"`

### Requirement: Service relation name generation
The `serviceRelationName` method SHALL produce a relation service name by combining the owner name, `"ServiceFor"`, and the capitalized relation name.

#### Scenario: Relation service name
- **GIVEN** a `RelationType` with name `"items"` owned by a class named `"Order"`
- **WHEN** `serviceRelationName(relation)` is called
- **THEN** the result is `"OrderServiceForItems"`

### Requirement: REST parameter name generation
The `restParamName` method SHALL extract the last segment of a `DataType` name (split by `::`), then capitalize each dot-separated token and join them.

#### Scenario: Dotted data type name
- **GIVEN** a `DataType` with name `"Package::my.data.type"`
- **WHEN** `restParamName(dataType)` is called
- **THEN** the result is `"MyDataType"`

### Requirement: XMI ID extraction
The `getXMIID` method SHALL return the XMI resource ID for any EMF `EObject`.

#### Scenario: Valid EMF element
- **GIVEN** an `EObject` contained in an `XMIResource`
- **WHEN** `getXMIID(element)` is called
- **THEN** the XMI ID string is returned

### Requirement: String case transformation
The `firstToUpper` and `firstToLower` methods SHALL transform only the first character of the input string.

#### Scenario: Uppercase first character
- **WHEN** `firstToUpper("hello")` is called
- **THEN** the result is `"Hello"`

#### Scenario: Lowercase first character
- **WHEN** `firstToLower("Hello")` is called
- **THEN** the result is `"hello"`
