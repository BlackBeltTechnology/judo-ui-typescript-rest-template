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
