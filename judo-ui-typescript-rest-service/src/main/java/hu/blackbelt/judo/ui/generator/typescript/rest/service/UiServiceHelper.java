package hu.blackbelt.judo.ui.generator.typescript.rest.service;

/*-
 * #%L
 * JUDO UI Typescript Rest Generator Service
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

import static hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper.classDataName;

@Log
@TemplateHelper
public class UiServiceHelper extends StaticMethodValueResolver {

    public static Collection<RelationType> getNotAccessRelationsTypes(Application application) {
        return (List<RelationType>) application.getRelationTypes().stream()
                .filter(r -> !hasRelationTypeOwner(r)).collect(Collectors.toList());
    }

    public static Collection<RelationType> getAccessRelationsTypes(Application application) {
        return (List<RelationType>) application.getRelationTypes().stream()
                .filter(r -> hasRelationTypeOwner(r)).collect(Collectors.toList());
    }

    public static boolean hasRelationTypeOwner(Object relationType) {
        if(relationType instanceof RelationType) {
            if (( (RelationType) relationType).isIsAccess())
                return true;
            else
                return false;
        }
        return false;
    }

    public static String joinedTokensForApiImport(RelationType relation){
        HashSet<String> tokens = new HashSet<>();

        if (!relation.isIsAccess()) {
            tokens.add(classDataName((ClassType) relation.getOwner(), ""));
        }

        tokens.add(classDataName(relation.getTarget(), "QueryCustomizer"));
        tokens.add(classDataName(relation.getTarget(), "Stored"));
        tokens.add(classDataName(relation.getTarget(), ""));

        for (RelationType targetRelation : relation.getTarget().getRelations()) {
            tokens.add(classDataName(targetRelation.getTarget(), "QueryCustomizer"));
            tokens.add(classDataName(targetRelation.getTarget(), "Stored"));
            tokens.add(classDataName(targetRelation.getTarget(), ""));
        }

        for (OperationType operation: relation.getTarget().getOperations()) {
            if (operation.getIsInputRangeable()) {
                tokens.add(classDataName(operation.getInput().getTarget(), "QueryCustomizer"));
                tokens.add(classDataName(operation.getInput().getTarget(), "Stored"));
                tokens.add(classDataName(operation.getInput().getTarget(), ""));
            }
        }

        return String.join(", ", tokens);
    }

    public static String joinedTokensForApiImportForAccessRelationServiceImpl(RelationType relation){
        HashSet<String> tokens = new HashSet<>();

        if (!relation.isIsAccess()) {
            tokens.add(classDataName((ClassType) relation.getOwner(), ""));
        }

        tokens.add(classDataName(relation.getTarget(), "QueryCustomizer"));
        tokens.add(classDataName(relation.getTarget(), "Stored"));
        tokens.add(classDataName(relation.getTarget(), ""));

        return String.join(", ", tokens);
    }

    public static String joinedTokensForApiImportClassService(ClassType classType){
        HashSet<String> tokens = new HashSet<>();

        tokens.add(classDataName(classType, ""));
        tokens.add(classDataName(classType, "Stored"));

        if (classType.isIsMapped()) {
            tokens.add(classDataName(classType, "QueryCustomizer"));
        }

        for (RelationType relation: classType.getRelations()) {
            tokens.add(classDataName(relation.getTarget(), ""));
            tokens.add(classDataName(relation.getTarget(),"Stored"));
            tokens.add(classDataName(relation.getTarget(),"QueryCustomizer"));
        }

        for (OperationType operation: classType.getOperations()) {
            if (operation.getInput() != null) {
                tokens.add(classDataName(operation.getInput().getTarget(), ""));
            }

            if (operation.getOutput() != null) {
                tokens.add(classDataName(operation.getOutput().getTarget(), "Stored"));
            }

            if (operation.getIsInputRangeable()) {
                tokens.add(classDataName(operation.getInput().getTarget(), "QueryCustomizer"));
                tokens.add(classDataName(operation.getInput().getTarget(), "Stored"));
            }
        }

        return String.join(", ", tokens);
    }

    public static ClassType getRelationOwnerAsClassType(RelationType relationType){
        return ((ClassType) relationType.getOwner());
    }

    public static String serviceClassName(ClassType type) {
        return classDataName(type, "").concat("Service");
    }

    public static boolean classTypeIsMapped(ClassType classType) {
        return classType.isIsMapped();
    }

    public static boolean relationIsCollection(RelationType relationType) {
        return relationType.isIsCollection();
    }

}
