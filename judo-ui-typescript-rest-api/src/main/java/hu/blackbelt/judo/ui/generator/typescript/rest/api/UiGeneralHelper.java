package hu.blackbelt.judo.ui.generator.typescript.rest.api;

import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.internal.lang3.StringUtils;
import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.Application;
import hu.blackbelt.judo.meta.ui.NavigationItem;
import hu.blackbelt.judo.meta.ui.Sort;
import hu.blackbelt.judo.meta.ui.data.*;
import lombok.extern.java.Log;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMIResource;

import java.util.*;
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

    public static String firstToUpper(String input) {
        return StringUtils.capitalize(input);
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

    public static String restParamName(ClassType classType, String filler) {
        String safeFiller = filler == null ? "" : filler;
        String packName = packageName(classType.getName());
        return (packName == null ? "" : packName) + className(classType.getName()) + safeFiller;
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
            String token = restParamName(rel.getTarget(), null) ;

            if (!rel.getTarget().equals(classType)) {
                tokens.put(token + "Stored", token);
            }
        }

        return tokens;
    }

}