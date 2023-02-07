package hu.blackbelt.judo.ui.generator.typescript.rest.api;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.data.ClassType;
import lombok.extern.java.Log;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Log
@TemplateHelper
public class Helper extends StaticMethodValueResolver {

    public static String getCamelCaseVersion(String token) {
        return StringUtils.capitalize(stream(token.split("_")).map(StringUtils::capitalize).collect(Collectors.joining()));
    }

    public static String nameWithoutModel(String fqName) {
        return stream(fqName.replaceAll("#", "::")
                .replaceAll("\\.", "::")
                .replaceAll("/", "::")
                .replaceAll("_", "::")
                .split("::"))
                .skip(1)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }

    public static String getClassName(ClassType type) {
        return type.getPackageNameTokens().stream()
                .map(Helper::getCamelCaseVersion)
                .collect(Collectors.joining())
                .concat(getCamelCaseVersion(type.getSimpleName()));
    }

}
