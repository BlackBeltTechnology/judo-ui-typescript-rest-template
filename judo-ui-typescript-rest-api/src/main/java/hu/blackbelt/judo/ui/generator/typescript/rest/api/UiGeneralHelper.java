package hu.blackbelt.judo.ui.generator.typescript.rest.api;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.internal.lang3.StringUtils;
import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.Application;
import hu.blackbelt.judo.meta.ui.NavigationItem;
import hu.blackbelt.judo.meta.ui.Sort;
import hu.blackbelt.judo.meta.ui.data.*;
import lombok.extern.java.Log;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMIResource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static hu.blackbelt.judo.ui.generator.typescript.rest.api.Helper.*;

@Log
@TemplateHelper
public class UiGeneralHelper extends StaticMethodValueResolver {

    public static void debug(Object obj) {
        System.out.print(obj);
    }

    public static String plainName(String input) {
        return input.replaceAll("[^\\.A-Za-z0-9_]", "_").toLowerCase();
    }

    public static String cleanup(String string) {
        if (string == null) {
            return "";
        }
        return string.replaceAll("[\\n\\t ]", "");
    }

    public static String pathName(String fqName) {
        return fqName
                .replaceAll("\\.", "-")
                .replaceAll("::", "-")
                .replaceAll("_", "-")
                .replaceAll("#", "-")
                .replaceAll("/", "-")
                .replaceAll("([a-z])([A-Z]+)", "$1-$2")
                .toLowerCase();
    }

    public static String path(String fqName) {
        String fq = pathName(fqName);
        if (fq.lastIndexOf("-") > -1) {
            return fq.substring(fq.lastIndexOf("-") + 2);
        } else {
            return fq;
        }
    }

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

    public static String modelName(String fqName) {
        String[] splitted = fqName.split("::");
        return fqClass(stream(splitted)
                .map(StringUtils::capitalize)
                .findFirst().get());
    }

    @Deprecated
    public static String fqClass(String fqName) {
        return stream(fqName.replaceAll("#", "::")
                .replaceAll("\\.", "::")
                .replaceAll("/", "::")
                .replaceAll("_", "::")
                .split("::"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }

    public static String fqClassWithoutModel(String fqName) {
        return stream(fqName.replaceAll("#", "::")
                .replaceAll("\\.", "::")
                .replaceAll("/", "::")
                .replaceAll("_", "::")
                .split("::"))
                .skip(1)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }

    public static Boolean isNavItemAGroup(NavigationItem navigationItem) {
        return navigationItem.getTarget() == null;
    }

    public static String iconClassNameToMuiIconName(String fontIconClassName) {
        return stream(fontIconClassName.split("[_\\-]"))
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }

    public static String variable(String fqName) {
        return StringUtils.uncapitalize(className(fqName));
    }

    public static String ucFirst(String name) {
        return StringUtils.capitalize(name);
    }

    public static String safeSort(Sort input) {
        if (input == null) {
            return Sort.ASC.toString().toLowerCase();
        }
        return input.equals(Sort.NONE) ? Sort.ASC.toString().toLowerCase() : input.toString().toLowerCase();
    }

    public static String getXMIID(EObject element) {
        return ((XMIResource) element.eResource()).getID(element);
    }

    public static String restParamName(EnumerationType enumerationType) {
        String[] tokens = enumerationType.getName().split("::");
        String last = tokens[tokens.length - 1];
        return stream(last.split("\\.")).map(e -> StringUtils.capitalize(e)).collect(Collectors.joining(""));
    }

    public static String dataTypeRestParamName(DataType dataType) {
        EnumerationType enumerationType = (EnumerationType) dataType;
        String[] tokens = enumerationType.getName().split("::");
        String last = tokens[tokens.length - 1];
        return stream(last.split("\\.")).map(e -> StringUtils.capitalize(e)).collect(Collectors.joining(""));
    }

    public static String firstToUpper(String input) {
        return StringUtils.capitalize(input);
    }

    public static String firstToLower(String input) {
        return StringUtils.uncapitalize(input);
    }

    public static boolean boolValue(Boolean original) {
        return original != null && original;
    }

    public static boolean hasElements(Collection<Object> items) {
        return items.size() > 0;
    }

    public static Set<Object> asUniqueSet(Collection<Object> input) {
        return new HashSet<>(input);
    }

    public static String actorTechnicalName(Application app) {
        return app.getActor().getName().replaceAll("::", "__").toLowerCase();
    }

    public static String emptyStringFallback(String input) {
        return input == null ? "" : input;
    }

    public static String attributePath(AttributeType attributeType) {
        return "/".concat(String.join("/", attributeType.getOwnerPackageNameTokens())
                .concat("/")
                .concat(attributeType.getOwnerSimpleName())
                .concat("/")).concat(attributeType.getName());
    }

    public static boolean isAttributeTypeBinary(AttributeType attributeType) {
        return attributeType.getDataType() instanceof BinaryType;
    }

    public static boolean hasDataElementOwner(DataElement dataElement) {
        if (dataElement instanceof RelationType) {
            return !((RelationType) dataElement).isIsAccess();
        }
        return false;
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

    //restParamName(ClassType classType, String filler)
    public static String classDataName(ClassType classType, String suffix) {
        String className = classType.getName();
        String base = nameWithoutModel(className);

        return base += suffix != null ? suffix : "";
    }

    private static <T extends Object> Predicate<T> distinctByKey(final Function<? super T, ?> keyExtractor) {
        final ConcurrentHashMap<Object, Boolean> seen = new ConcurrentHashMap<Object, Boolean>();
        final Predicate<T> _function = (T t) -> {
            Boolean _putIfAbsent = seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE);
            return (_putIfAbsent == null);
        };
        return _function;
    }





    /*public static String classDataName(ClassType classType, String filler) {
        String safeFiller = filler == null ? "" : filler;
        String packName = packageName(classType.getName());
        return (packName == null ? "" : packName) + className(classType.getName()) + safeFiller;
    }*/

    public static String FaultContainerName(OperationType operationType) {
        String packName = packageName(operationType.getName());
        return (packName == null ? "" : firstToUpper(packName)) + firstToUpper(className(operationType.getName())) + "FaultContainer";
    }


    public static HashMap<String, String> getImportTokens(ClassType actor, ClassType classType) {
        HashMap<String, String> tokens = new HashMap<String, String>();

        for (AttributeType attr: classType.getAttributes()) {
            if (attr.getDataType() instanceof EnumerationType) {
                String token = restParamName((EnumerationType) attr.getDataType());

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

    public static HashSet<String> getImportTokensForQueries(ClassType actor, ClassType classType) {
        HashSet<String> tokens = new HashSet<String>();

        for(AttributeType attr: classType.getAttributes()) {
            String token = restFilterName(actor, attr.getDataType());

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

    public static EList<OperationParameterType> getOperationTypeFaults(OperationType operationType){
        return operationType.getFaults();
    }

    public static String openApiDataType(String fqDataTypeName){
        if (fqDataTypeName == null) {
            return null;
        }

        String fqDataTypeNames[] = fqDataTypeName.split("\\.");
        return fqDataTypeNames[fqDataTypeNames.length - 1];
    }
    public static String restFilterName(ClassType actor, DataType dataType) {
        return "FilterBy" + openApiDataType(dataType.getName());
    }

    /*public static String getClassName(ClassType classType) {
        return classType.getPackageNameTokens().stream()
                .map(t -> getCamelCaseVersion(t))
                .collect(Collectors.joining())
                .concat(getCamelCaseVersion(classType.getSimpleName()));
    }*/

    public static Boolean AttributeIsIsFilterable(AttributeType attribute) {
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
        return classType.getRelations().stream()
                .filter(r -> r.getRelationKind() != RelationKind.ASSOCIATION).collect(Collectors.toList()).size();
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

    public static HashSet<RelationType> getNotClassTypeRelations(HashSet<RelationType> uniqueRelations, ClassType classType) {
        return uniqueRelations.stream().filter(a -> !a.getTarget().equals(classType)).collect(Collectors.toCollection(HashSet::new));
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

    public static ClassType getRelationTarget(RelationType relation) {
        return relation.getTarget();
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
            tokens.add(restParamName((EnumerationType) dataType));
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