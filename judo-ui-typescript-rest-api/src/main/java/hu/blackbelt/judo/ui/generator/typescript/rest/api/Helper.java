package hu.blackbelt.judo.ui.generator.typescript.rest.api;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.data.ClassType;
import hu.blackbelt.judo.meta.ui.data.RelationType;
import lombok.extern.java.Log;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static hu.blackbelt.judo.ui.generator.typescript.rest.api.UiGeneralHelper.*;

@Log
@TemplateHelper
public class Helper extends StaticMethodValueResolver {

    protected static String getFileNameVersion(String token) {
        return token.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    protected static String getPackagePath(ClassType type) {
//        if (type.getPackageNameTokens().isEmpty()) {
            return "";
//        } else {
//            return type.getPackageNameTokens().stream()
//                    .map(UiGeneralHelper::getFileNameVersion)
//                    .collect(Collectors.joining("/"))
//                    .concat("/");
//        }
    }

    protected static String getPackagePath(RelationType relation) {
//        if (relation.getOwnerPackageNameTokens().isEmpty()) {
            return "";
//        } else {
//            return relation.getOwnerPackageNameTokens().stream()
//                    .map(UiGeneralHelper::getFileNameVersion)
//                    .collect(Collectors.joining("/"))
//                    .concat("/");
//        }
    }

    protected static String getTypeNamePath(ClassType type) {
        return getPackagePath(type).concat(getFileNameVersion(type.getSimpleName())).concat("/");
    }

    protected static String getTypeNamePath(RelationType relation) {
        return getPackagePath(relation).concat(getFileNameVersion(relation.getOwnerSimpleName())).concat("/");
    }

    protected static String getFeaturePath(RelationType relation) {
        return getTypeNamePath(relation).concat(getFileNameVersion(relation.getName())).concat("/");
    }

    protected static String getCamelCaseVersion(String token) {
        return StringUtils.capitalize(stream(token.split("_")).map(StringUtils::capitalize).collect(Collectors.joining()));
    }

    protected static String variable(String token) {
        String str = getCamelCaseVersion(token);
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    protected static String nameWithoutModel(String fqName) {
        return stream(fqName.replaceAll("#", "::")
                .replaceAll("\\.", "::")
                .replaceAll("/", "::")
                .replaceAll("_", "::")
                .split("::"))
                .skip(1)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }

    protected static String getClassName(ClassType type) {
        return type.getPackageNameTokens().stream().map(Helper::getCamelCaseVersion).collect(Collectors.joining())
                .concat(getCamelCaseVersion(type.getSimpleName()));
    }

    protected static String className(String fqName) {
        if (fqName == null) {
            return null;
        }
        String[] splitted = fqName.split("::");
        return nameWithoutModel(stream(splitted)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining()));
    }
}