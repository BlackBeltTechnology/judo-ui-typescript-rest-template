package hu.blackbelt.judo.ui.generator.typescript.rest.api;

import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.data.ClassType;
import hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper;
import lombok.extern.java.Log;

import java.util.stream.Collectors;

import static hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper.getCamelCaseVersion;

@Log
@TemplateHelper
public class Helper extends StaticMethodValueResolver {

    public static String getClassName(ClassType type) {
        return type.getPackageNameTokens().stream()
                .map(UiCommonsHelper::getCamelCaseVersion)
                .collect(Collectors.joining())
                .concat(getCamelCaseVersion(type.getSimpleName()));
    }

}
