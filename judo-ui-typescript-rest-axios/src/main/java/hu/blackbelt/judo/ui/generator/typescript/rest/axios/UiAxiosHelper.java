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
    public static final String SPLITTER = "::";
    public static final String TRANSFER_SKIP_SEGMENT = "_default_transferobjecttypes";

    public static String rootPathForApp(Application app) {
        return String.join("/", app.getActor().getPackageNameTokens())
                .concat("/")
                .concat(app.getActor().getSimpleName());
    }

    public static String restPath(ClassType classType, String first, String name, String second) {
        String suffix = first + (name == null ? "" : String.join("/", name.split(SPLITTER))) + (second == null ? "" : second);
        String effectiveSuffix = suffix == null ? "" : suffix;
        String packages = !classType.getPackageNameTokens().isEmpty() ? String.join("/", classType.getPackageNameTokens()) + "/" : "";
//        return "/" + stream(classType.getFQName().replaceFirst(application.getName() + "::", "").split(SPLITTER))
        return "/" + packages + stream(classType.getSimpleName().split(SPLITTER))
                .filter(i -> !i.contains(TRANSFER_SKIP_SEGMENT))
                .collect(Collectors.joining("/")) + effectiveSuffix;
    }

    public static String relationRestPath(RelationType relation, String suffix) {
        String effectiveSuffix = suffix == null ? "" : suffix;
        return String.join("/", relation.getOwnerPackageNameTokens())
                .concat("/")
                .concat(relation.getOwnerSimpleName())
                .concat("/")
                .concat(relation.getName())
                .concat(effectiveSuffix);
    }

    public static String classTypeRestPath(ClassType classType, String suffix) {
        String effectiveSuffix = suffix == null ? "" : suffix;
        return "/" + stream(classType.getName().split(SPLITTER))
                .filter(i -> !i.contains(TRANSFER_SKIP_SEGMENT))
                .collect(Collectors.joining("/")) + effectiveSuffix;
    }

    public static String operationRestPath(ClassType classType, OperationType operation, String suffix) {
        String effectiveSuffix = suffix == null ? "" : suffix;
        return classTypeRestPath(classType, "") + "/" + operation.getName() + effectiveSuffix;
    }

    public static String stringConcat(String... strings) {
        StringBuilder concatenatedString = new StringBuilder();
        for (String str : strings) {
            concatenatedString.append(str);
        }

        return concatenatedString.toString();
    }

    public static boolean hasFaults(OperationType operation) {
        return !operation.getFaults().isEmpty();
    }

    public static Collection<String> getWrittenImplementations(List<String> writtenFiles) {
        return writtenFiles.stream().filter(f -> f.endsWith("Impl.ts")).map(f -> f.substring(0, f.length() - 3)).collect(Collectors.toList());
    }

    public static String getDepPath(String importSource) {
        return "../" + importSource;
    }

}
