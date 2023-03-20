package hu.blackbelt.judo.ui.generator.typescript.rest.commons;

/*-
 * #%L
 * JUDO UI Typescript Rest Generator Commons
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
import hu.blackbelt.judo.meta.ui.data.ClassType;
import hu.blackbelt.judo.meta.ui.data.DataType;
import hu.blackbelt.judo.meta.ui.data.RelationType;
import lombok.extern.java.Log;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMIResource;

import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Log
@TemplateHelper
public class UiCommonsHelper extends StaticMethodValueResolver {
    public static String firstToUpper(String input) {
        return StringUtils.capitalize(input);
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

    public static String getCamelCaseVersion(String token) {
        return StringUtils.capitalize(stream(token.split("_")).map(StringUtils::capitalize).collect(Collectors.joining()));
    }

    public static String getXMIID(EObject element) {
        return ((XMIResource) element.eResource()).getID(element);
    }

    public static String classDataName(ClassType classType, String suffix) {
        String className = classType.getName();
        String base = nameWithoutModel(className);

        return base += suffix != null ? suffix : "";
    }

    public static String serviceRelationName(RelationType relation) {
        return relation.getOwnerPackageNameTokens().stream()
                .map(UiCommonsHelper::getCamelCaseVersion)
                .collect(Collectors.joining())
                .concat(getCamelCaseVersion(relation.getOwnerSimpleName()))
                .concat("ServiceFor")
                .concat(getCamelCaseVersion(relation.getName()));
    }

    public static String restParamName(DataType dataType) {
        String[] tokens = dataType.getName().split("::");
        String last = tokens[tokens.length - 1];
        return stream(last.split("\\.")).map(StringUtils::capitalize).collect(Collectors.joining(""));
    }
}
