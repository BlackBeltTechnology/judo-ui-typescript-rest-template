package hu.blackbelt.judo.ui.generator.typescript.rest.api;

/*-
 * #%L
 * JUDO UI Typescript Rest Generator API
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.Application;
import hu.blackbelt.judo.meta.ui.NamedElement;
import hu.blackbelt.judo.meta.ui.data.*;
import lombok.extern.java.Log;
import org.eclipse.emf.common.util.EList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper.classDataName;
import static hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper.firstToUpper;
import static hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper.getXMIID;
import static hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper.restParamName;

@Log
@TemplateHelper
public class UiGeneralHelper extends StaticMethodValueResolver {

    public static String packageName(String packageName) {
        List<String> nameTokens = stream(packageName
                .split("::"))
                .collect(Collectors.toList());
        if (nameTokens.size() > 2) {
            nameTokens.remove(0);
            nameTokens.remove(nameTokens.size() - 1);
            return nameTokens.stream()
                    .map(s -> StringUtils.capitalize(
                            stream(s.replaceAll("#", "::")
                                    .replaceAll("\\.", "::")
                                    .replaceAll("/", "::")
                                    .replaceAll("_", "::")
                                    .split("::"))
                                    .map(t -> StringUtils.capitalize(t))
                                    .collect(Collectors.joining())
                    ))
                    .collect(Collectors.joining());
        }
        return null;
    }

    public static String firstToLower(String input) {
        return StringUtils.uncapitalize(input);
    }

    public static Collection<EnumerationType> getEnumerationTypes(Application application) {
        return application.getDataTypes().stream()
                .filter(i -> i instanceof EnumerationType)
                .map(i -> (EnumerationType) i).collect(Collectors.toList());
    }

    public static Collection<ClassType> getClassTypes(Application application) {
        return application.getDataElements().stream()
                .filter(i -> i instanceof ClassType)
                .map(i -> (ClassType) i)
                .filter(i -> !i.isIsActor())
                .sorted(Comparator.comparing(NamedElement::getFQName))
                .collect(Collectors.toList());
    }

    public static Collection<OperationType> getOperationType(Application application) {
        return application.getDataElements().stream().filter(i -> i instanceof ClassType)
                .map(i -> (ClassType) i)
                .flatMap(i -> i.getOperations().stream())
                .filter(i -> i instanceof OperationType)
                .map(i -> (OperationType) i).collect(Collectors.toList());
    }

    public static Collection<ClassType> getQueryCustomizers(Application application) {
        return application.getDataElements().stream().filter(i -> i instanceof ClassType)
                .map(i -> (ClassType) i)
                .filter(i -> !i.isIsActor()).collect(Collectors.toList());
    }

    public static Collection<DataType> getFilterableDataTypes(Application app) {
        List<ClassType> classes = app.getClassTypes();
        ArrayList<AttributeType> attributeTypeList = new ArrayList();

        for (ClassType classType : classes) {
            attributeTypeList.addAll(
                    classType.getAttributes().stream()
                            .filter(a -> a.isIsFilterable())
                            .collect(Collectors.toList())
            );
        }

        return attributeTypeList.stream()
                .map(a -> a.getDataType())
                .filter(distinctByKey(dataType -> getXMIID(dataType))).collect(Collectors.toList());
    }

    private static <T extends Object> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final ConcurrentHashMap<Object, Boolean> seen = new ConcurrentHashMap<Object, Boolean>();
        final Predicate<T> _function = (T t) -> {
            Boolean _putIfAbsent = seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE);
            return (_putIfAbsent == null);
        };
        return _function;
    }

    public static String faultContainerName(OperationType operationType) {
        String packName = packageName(operationType.getName());
        return (packName == null ? "" : firstToUpper(packName)) + firstToUpper(className(operationType.getName())) + "FaultContainer";
    }


    public static TreeMap<String, String> getImportTokens(ClassType actor, ClassType classType) {
        TreeMap<String, String> tokens = new TreeMap<String, String>();

        for (AttributeType attr: classType.getAttributes()) {
            if (attr.getDataType() instanceof EnumerationType) {
                String token = restParamName(attr.getDataType());
                tokens.put(token, token);
            }
        }

        for (RelationType rel: classType.getRelations()) {
            String token = classDataName(rel.getTarget(), null);
            if (!rel.getTarget().equals(classType)) {
                tokens.put(token + "Stored", token);
            }
        }

        return tokens;
    }

    public static TreeSet<String> getImportTokensForQueries(ClassType classType) {
        TreeSet<String> tokens = new TreeSet<String>();

        for(AttributeType attr: classType.getAttributes()) {
            String token = restFilterName(attr.getDataType());
            if (attr.isIsFilterable() && !tokens.contains(token)) {
                tokens.add(token);
            }
        }

        return tokens;
    }


    private static boolean hasClassAttributes(ClassType classType) {
        return classType.getAttributes() != null && classType.getAttributes().size() > 0;
    }
    public static HashSet<String> modelImportTokens(ClassType classType) {
        var tokens = new HashSet<String>();

        tokens.addAll(classType.getRelations().stream().map(r -> classDataName(r.getTarget(), "Attributes")).collect(Collectors.toList()));

        if (hasClassAttributes(classType)) {
            tokens.add(classDataName(classType, "Attributes"));
        }

        return tokens;
    }

    public static String className(String fqName) {
        if(fqName == null) {
            return null;
        }

        if(!fqName.contains("::")) {
            return fqName;
        }

        String[] splittedfqName = fqName.split("::");
        return splittedfqName[splittedfqName.length - 1];
    }
    public static String unsafeVariable(String fqName) {
        return className(fqName);
    }

    public static String getRelationType(RelationType relation) {
        String typeName = classDataName(relation.getTarget(), "Stored");
        return relation.isIsCollection() ? ("Array<" + typeName + ">") : typeName;
    }

    public static String getClassTypeAttributes(ClassType classType) {
        return classType.getAttributes().stream().map(r -> unsafeVariable(r.getName())).collect(Collectors.joining("' | '"));
    }

    public static String getClassTypeRelations(ClassType classType) {
        return classType.getRelations().stream().map(r -> unsafeVariable(r.getName())).collect(Collectors.joining("' | '"));
    }

    public static String getFaultTargets(OperationParameterType operationParameterType) {
        return classDataName(operationParameterType.getTarget(), null).toString();
    }

    public static List<OperationParameterType> getOperationTypeFaults(OperationType operationType){
        return operationType.getFaults()
                .stream()
                .sorted(Comparator.comparing(NamedElement::getFQName))
                .collect(Collectors.toList());
    }

    public static String openApiDataType(String fqDataTypeName){
        if (fqDataTypeName == null) {
            return null;
        }

        String fqDataTypeNames[] = fqDataTypeName.split("::");
        return fqDataTypeNames[fqDataTypeNames.length - 1];
    }
    public static String restFilterName(DataType dataType) {
        return "FilterBy" + openApiDataType(dataType.getName());
    }

    public static Boolean attributeIsFilterable(AttributeType attribute) {
        return attribute.isIsFilterable();
    }

    public static Boolean isGreaterThan(int a, int b) {
        if (a > b) {
            return true;
        } else {
            return false;
        }
    }

    public static String joinModelImportTokens(HashSet<String> modelImportTokens) {
        return modelImportTokens.stream().collect(Collectors.joining(", "));
    }

    public static List<RelationType> getAggregatedRelations(ClassType classType) {
        return classType.getRelations().stream()
                .filter(r -> r.getRelationKind() != RelationKind.ASSOCIATION).collect(Collectors.toList());
    }

    public static int getAggregatedRelationsSize(ClassType classType) {
        return getAggregatedRelations(classType).size();
    }

    public static List<RelationType> getAggregatedTarget(RelationType relationType) {
        return relationType.getTarget().getRelations().stream()
                .filter(r -> r.getRelationKind() != RelationKind.ASSOCIATION).collect(Collectors.toList());
    }



    public static HashSet<RelationType> getUniqueRelations(ClassType classType) {
        HashSet<RelationType> uniqueRelations = new HashSet<RelationType>();

        for (RelationType relation: getAggregatedRelations(classType)) {
            if (!uniqueRelations.stream().map(r -> r.getTarget().getName()).collect(Collectors.toList()).contains(relation.getTarget().getName())) {
                uniqueRelations.add(relation);
            }
        }

        return uniqueRelations;
    }

    public static List<RelationType> getNotClassTypeRelations(HashSet<RelationType> uniqueRelations, ClassType classType) {
        return uniqueRelations
                .stream()
                .filter(a -> !a.getTarget().equals(classType))
                .sorted(Comparator.comparing(NamedElement::getFQName))
                .collect(Collectors.toList());
    }

    public static String relationBuilderName (RelationType relationType, ClassType classType, String postfix) {
        return classDataName(classType, firstToUpper(unsafeVariable(relationType.getName())) + postfix);
    }
    public static String getRelationBuilderNames(RelationType relation) {
        String relationBuilderName =  getAggregatedTarget(relation).stream()
                .map(r -> relationBuilderName(r, relation.getTarget(), "MaskBuilder"))
                .collect(Collectors.joining(", "));

        return relationBuilderName;
    }

    public static String getRelationBuilderNamesWithPipe(RelationType relation) {
        String relationBuilderName =  getAggregatedTarget(relation).stream()
                .map(r -> " | " + relationBuilderName(r, relation.getTarget(), "MaskBuilder"))
                .collect(Collectors.joining());

        return relationBuilderName;
    }

    public static String generateBuilderProps(ClassType classType) {
        String attrs = hasClassAttributes(classType) ? classDataName(classType, "Attributes") : "";
        String rels = getAggregatedRelationsSize(classType) > 0
                ? getAggregatedRelations(classType).stream()
                .map(r -> relationBuilderName(r, classType, "MaskBuilder"))
                .collect(Collectors.joining(" | ")) : "";

        String res = attrs + (attrs.length() > 0 && rels.length() > 0 ? " | " : "") + rels;

        return res.length() > 0 ? res : "any";
    }

    public static boolean isEnumType(DataType type) {
        return type instanceof EnumerationType;
    }

    public static String joinModelImports(DataType dataType) {
        ArrayList<String> tokens = new ArrayList<String>();

        tokens.add(typescriptType(dataType.getOperator()));

        if (isEnumType(dataType)) {
            tokens.add(restParamName(dataType));
        }

        return tokens.stream().collect(Collectors.joining(", "));
    }

    public static boolean classTypeRelationsIsEmpty(ClassType classType) {
        return classType.getRelations().isEmpty();
    }


    public static String typescriptType(DataType dataType) {
        if (dataType instanceof EnumerationType) {
            return restParamName((EnumerationType) dataType);
        } else if (dataType instanceof NumericType) {
            return "number";
        } else if (dataType instanceof BooleanType) {
            return "boolean";
        } else if (dataType instanceof TimeType) {
            return "string";
        } else if (dataType instanceof DateType) {
            return "string";
        } else if (dataType instanceof TimestampType) {
            return "Date";
        } else if (dataType instanceof StringType) {
            return "string";
        } else {
            return "any";
        }
    }

}
