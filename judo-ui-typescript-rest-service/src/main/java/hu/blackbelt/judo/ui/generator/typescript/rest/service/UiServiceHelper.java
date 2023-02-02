package hu.blackbelt.judo.ui.generator.typescript.rest.service;

import com.github.jknack.handlebars.internal.lang3.StringUtils;
import hu.blackbelt.judo.generator.commons.StaticMethodValueResolver;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import hu.blackbelt.judo.meta.ui.Application;
import hu.blackbelt.judo.meta.ui.NavigationItem;
import hu.blackbelt.judo.meta.ui.Sort;
import hu.blackbelt.judo.meta.ui.data.*;
import lombok.extern.java.Log;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.springframework.cglib.proxy.Mixin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static hu.blackbelt.judo.ui.generator.typescript.rest.api.Helper.*;
import static java.util.Arrays.stream;

@Log
@TemplateHelper
public class UiServiceHelper extends StaticMethodValueResolver {

    public static String classDataName(ClassType classType, String suffix) {
        String className = classType.getName();
        String base = nameWithoutModel(className);

        return base += suffix != null ? suffix : "";
    }
    public static Collection<RelationType> getRelationsTypesWithoutAccess(Application application) {
        return (List<RelationType>) application.getRelationTypes().stream()
                .filter(r -> !hasRelationTypeOwner(r)).collect(Collectors.toList());
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

    /*public static String classDataName(ClassType classType, String filler) {
        String safeFiller = filler == null ? "" : filler;
        String packName = packageName(classType.getName());
        return (packName == null ? "" : packName) + className(classType.getName()) + safeFiller;
    }*/

    public static boolean appPrincipalisNotNull(Application application) {
        return application.getPrincipal() != null;
    }

    public static String joinedTokensForApiImport(RelationType relation){
        HashSet<String> tokens = new HashSet<>();
        tokens.add(getClassName((ClassType) relation.getOwner()));
        tokens.add(classDataName(relation.getTarget(), "QueryCustomizer"));
        tokens.add(classDataName(relation.getTarget(), "Stored"));
        tokens.add(getClassName(relation.getTarget()));

        for (RelationType targetRelation : relation.getTarget().getRelations()) {
            tokens.add(classDataName(targetRelation.getTarget(), "QueryCustomizer"));
            tokens.add(classDataName(targetRelation.getTarget(), "Stored"));
            tokens.add(getClassName(targetRelation.getTarget()));
        }

        for (OperationType operation: relation.getTarget().getOperations()) {
            if (operation.getIsInputRangeable()) {
                tokens.add(classDataName(operation.getInput().getTarget(), "QueryCustomizer"));
                tokens.add(classDataName(operation.getInput().getTarget(), "Stored"));
                tokens.add(getClassName(operation.getInput().getTarget()));
            }
        }

        return tokens.stream().collect(Collectors.joining(", "));
    }

    /*public static String getDepPath(String importSource) {

        String scope_ = (params.npmScope !== null ? params.npmScope + "/" : "");

        return (params.mode == "npm" ? scope_ : "../") + importSource;
    }*/

    public static String serviceRelationName(RelationType relation) {
        return relation.getOwnerPackageNameTokens().stream()
                .map(t -> getCamelCaseVersion(t))
                .collect(Collectors.joining())
                .concat(getCamelCaseVersion(relation.getOwnerSimpleName()))
                .concat("ServiceFor")
                .concat(getCamelCaseVersion(relation.getName()));
    }

    public static ClassType getRealitionOwnerAsClassType(RelationType relationType){
        return (ClassType) relationType.getOwner();
    }

    public static boolean relationIsCollection(RelationType relationType) {
        return relationType.isIsCollection();
    }

    public static void debugHelper() {
        int a = 5;
    }
}