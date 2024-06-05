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
import hu.blackbelt.judo.meta.ui.NamedElement;
import hu.blackbelt.judo.meta.ui.data.*;
import lombok.extern.java.Log;

import java.util.*;
import java.util.stream.Collectors;

import static hu.blackbelt.judo.ui.generator.typescript.rest.commons.UiCommonsHelper.classDataName;

@Log
@TemplateHelper
public class UiServiceHelper extends StaticMethodValueResolver {

    public static List<RelationType> getNotAccessRelationsTypes(Application application) {
        return (List<RelationType>) application.getRelationTypes().stream()
                .filter(r -> !((RelationType) r).isIsAccess())
                .sorted(Comparator.comparing(NamedElement::getFQName))
                .toList();
    }

    public static List<RelationType> getAccessRelationsTypes(Application application) {
        return (List<RelationType>) application.getRelationTypes().stream()
                .filter(r -> ((RelationType) r).isIsAccess())
                .sorted(Comparator.comparing(NamedElement::getFQName))
                .toList();
    }

    public static List<String> accessJoinedImportTokens(Application application) {
        HashSet<String> tokens = new LinkedHashSet<>();
        if (application.getPrincipal() != null) {
            tokens.add(classDataName(application.getPrincipal(), ""));
        }
        getAccessRelationsTypes(application)
                .forEach(relation -> tokens.add(classDataName(relation.getTarget(), "")));
        return tokens.stream().sorted().toList();
    }

    public static List<String> joinedTokensForApiImport(RelationType relation){
        HashSet<String> tokens = new LinkedHashSet<>();

        if (!relation.isIsAccess()) {
            tokens.add(classDataName((ClassType) relation.getOwner(), ""));
        }

        tokens.add(classDataName(relation.getTarget(), ""));

        for (RelationType targetRelation : relation.getTarget().getRelations()) {
            tokens.add(classDataName(targetRelation.getTarget(), ""));

            fillImportTokens(tokens, targetRelation);
        }

        fillImportTokens(tokens, relation);

        return tokens.stream().sorted().toList();
    }

    private static void fillImportTokens(HashSet<String> tokens, RelationType targetRelation) {
        for (OperationType operation: targetRelation.getTarget().getOperations()) {
            if (operation.getInput() != null) {
                tokens.add(classDataName(operation.getInput().getTarget(), ""));
            }
            if (operation.getOutput() != null) {
                tokens.add(classDataName(operation.getOutput().getTarget(), ""));
            }
            if (operation.getIsInputRangeable()) {
                tokens.add(classDataName(operation.getInput().getTarget(), ""));
            }
        }
    }

    public static String joinedTokensForApiImportForAccessRelationServiceImpl(RelationType relation){
        HashSet<String> tokens = new LinkedHashSet<>();

        if (!relation.isIsAccess()) {
            tokens.add(classDataName((ClassType) relation.getOwner(), ""));
        }

        for (OperationType operationType: relation.getTarget().getOperations()) {
            tokens.add(classDataName(operationType.getInput().getTarget(), ""));
        }

        tokens.add(classDataName(relation.getTarget(), ""));

        return String.join(", ", tokens);
    }

    public static List<String> joinedTokensForApiImportClassService(ClassType classType){
        HashSet<String> tokens = new LinkedHashSet<>();

        tokens.add(classDataName(classType, ""));

        for (RelationType relation: classType.getRelations()) {
            tokens.add(classDataName(relation.getTarget(), ""));

            for (OperationType operation: relation.getTarget().getOperations()) {
                fillOperationTokens(operation, tokens);
            }
        }

        for (OperationType operation: classType.getOperations()) {
            fillOperationTokens(operation, tokens);
        }

        return tokens.stream().sorted().toList();
    }

    private static void fillOperationTokens(OperationType operation, HashSet<String> tokens) {
        if (operation.getInput() != null) {
            tokens.add(classDataName(operation.getInput().getTarget(), ""));
            for (RelationType relationType: operation.getInput().getTarget().getRelations()) {
                tokens.add(classDataName(relationType.getTarget(), ""));
            }
        }

        if (operation.getOutput() != null) {
            tokens.add(classDataName(operation.getOutput().getTarget(), ""));
        }

        if (operation.getIsInputRangeable()) {
            tokens.add(classDataName(operation.getInput().getTarget(), ""));
        }
    }

    public static ClassType getRelationOwnerAsClassType(RelationType relationType){
        return ((ClassType) relationType.getOwner());
    }

    public static Collection<OperationType> operationsOrderedByName(ClassType classType) {
        return classType.getOperations().stream().sorted(Comparator.comparing(OperationType::getName))
                .collect(Collectors.toList());
    }

    public static Collection<RelationType> relationsOrderedByName(ClassType classType) {
        return classType.getRelations().stream().sorted(Comparator.comparing(RelationType::getName))
                .collect(Collectors.toList());
    }

}
