package hu.blackbelt.judo.ui.generator.typescript.rest.axios;

/*-
 * #%L
 * JUDO UI Typescript Rest Generator Axios
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

import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.Application;
import hu.blackbelt.judo.meta.ui.data.*;
import lombok.extern.java.Log;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Log
@TemplateHelper
public class UiAxiosHelper extends StaticMethodValueResolver {
    public static String getAppName(Application application) {
        return application.getName();
    }

    public static String rootPathForApp(Application app) {
        return app.getActor().getPackageNameTokens().stream()
                .collect(Collectors.joining("/")).concat("/")
                .concat(app.getActor().getSimpleName());
    }

    public static String getAppNameSplittedByColonFirstTag(Application application) {
        return application.getName().split("::")[0];
    }

    public static String restPath(ClassType classType, String first, String name, String second) {
        String suffix = first + name + second;
        String effectiveSuffix = suffix == null ? "" : suffix;
        return "/" + stream(classType.getName().split("::"))
                .skip(1)
                .filter(i -> i != "_default_transferobjecttypes")
                .collect(Collectors.joining("/")) + effectiveSuffix;
    }

    public static String relationRestPath(RelationType relation, String suffix) {
        String effectiveSuffix = suffix == null ? "" : suffix;
        return relation.getOwnerPackageNameTokens().stream()
                .collect(Collectors.joining("/"))
                .concat("/")
                .concat(relation.getOwnerSimpleName())
                .concat("/")
                .concat(relation.getName())
                .concat(effectiveSuffix);
    }

    public static String classTypeRestPath(ClassType classType, String suffix) {
        String effectiveSuffix = suffix == null ? "" : suffix;
        return "/" + stream(classType.getName().split("::"))
                .skip(1)
                .filter(i -> i != "_default_transferobjecttypes")
                .collect(Collectors.joining("/")) + effectiveSuffix;
    }

    public static String classTypeRestPathThreeParams(ClassType classType, String first, String second) {
        String suffix = first + second;
        String effectiveSuffix = suffix == null ? "" : suffix;
        return "/" + stream(classType.getName().split("::"))
                .skip(1)
                .filter(i -> i != "_default_transferobjecttypes")
                .collect(Collectors.joining("/")) + effectiveSuffix;
    }

    public static String stringConcat(String... strings) {
        String concatedString = "";
        for (String str : strings) {
            concatedString += str;
        }

        return concatedString;
    }

    public static boolean faultsLengthMoreThanZero(OperationType operation) {
        return operation.getFaults().size() > 0;
    }

    public static Collection<String> getWrittenImplementations(List<String> writtenFiles) {
        return writtenFiles.stream().filter(f -> f.endsWith("Impl.ts")).map(f -> f.substring(0, f.length() - 3)).collect(Collectors.toList());
    }


    public static String getDepPath(String importSource) {
        return "../" + importSource;
    }

    public static boolean classTypeIsIsMapped(ClassType classType) {
        return classType.isIsMapped();
    }

}
