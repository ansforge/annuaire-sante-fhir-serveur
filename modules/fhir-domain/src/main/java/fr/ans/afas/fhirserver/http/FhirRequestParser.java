/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.http;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;


/**
 * Parse fhir requests urls
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirRequestParser {

    static final String[] SPECIAL_PARAMS = new String[]{"_count", "_pretty", "_format", "_total", "_include", "_revinclude", "_elements","_has"};


    private FhirRequestParser() {
    }

    public static String urlDecode(String string) {
        if (string == null) {
            return null;
        }
        return URLDecoder.decode(string, StandardCharsets.UTF_8);
    }


    public static <T> SelectExpression<T> parseSelectExpression(String url, ExpressionFactory<T> expressionFactory, SearchConfigService searchConfigService) throws BadSelectExpression, BadDataFormatException {
        var index = url.indexOf('?');

        String resourceType;
        List<ParsedParam> parsedParams;
        // we have a query
        if (index >= 0) {
            parsedParams = parseParameters(url.substring(index + 1));
            resourceType = url.substring(0, index);
        } else {
            resourceType = url;
            parsedParams = new ArrayList<>();
        }


        var selectExpression = new SelectExpression<>(resourceType, expressionFactory);
        for (var parsedParam : parsedParams) {

            if (ArrayUtils.indexOf(SPECIAL_PARAMS, parsedParam.getParamName()) >= 0) {
                switch (parsedParam.getParamName()) {
                    case "_count":
                        handleCountParam(selectExpression, parsedParam);
                        break;
                    case "_include":
                        handleIncludeParam(searchConfigService, selectExpression, resourceType, parsedParam);
                        break;
                    case "_revinclude":
                        handleRevIncludeParam(searchConfigService, selectExpression, resourceType, parsedParam);
                        break;
                    case "_total":
                        handleTotalParam(selectExpression, parsedParam);
                        break;
                    case "_elements":
                        handleElementsParam(selectExpression, parsedParam);
                        break;
                    case "_has":
                        handleHasParams(selectExpression, parsedParam);
                        break;
                    case "_pretty", "_format":
                    default:
                        break;

                }

            } else {
                // Détection du chaînage : si le paramètre contient un point, on le considère comme une recherche chaînée.
                FhirSearchPath path;
                if(parsedParam.getResourceTarget()!=null){
                    path = FhirSearchPath.builder().resource(resourceType).path(parsedParam.getResourceTarget()).build();

                } else {
                    path = FhirSearchPath.builder().resource(resourceType).path(parsedParam.getParamName()).build();
                }

                var sc = searchConfigService.getSearchConfigByPath(path).orElseThrow(() -> new BadSelectExpression("Parameter " + parsedParam.paramName + " not found for resource " + resourceType));
                switch (sc.getSearchType()) {

                    case "string":
                        parseString(selectExpression, parsedParam, path);
                        break;
                    case "token":
                        parseToken(selectExpression, parsedParam, path);
                        break;
                    case "date":
                        parseDate(selectExpression, parsedParam, path);
                        break;
                    case "reference":
                        parseReference(selectExpression, parsedParam, path);
                        break;
                    case "uri":
                        parseUri(selectExpression, parsedParam, path);
                        break;
                    default:
                        throw new BadSelectExpression("Search type not supported");
                }
            }
        }
        return selectExpression;
    }

    private static <T> void parseString(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) throws BadDataFormatException {
        var stringParam = new StringAndListParam();
        var stringOrListParam = new StringOrListParam();
        for (var oneVal : parsedParam.getParamValues()) {
            var sp = new StringParam();
            sp.setContains("contains".equals(parsedParam.getModifier()));
            sp.setExact("exact".equals(parsedParam.getModifier()));
            sp.setValue(oneVal);
            stringOrListParam.addOr(sp);
        }
        stringParam.addAnd(stringOrListParam);
        selectExpression.fromFhirParams(path, stringParam);
    }

    private static <T> void parseToken(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) throws BadDataFormatException {
        var tokenParam = new TokenAndListParam();
        var tokenOrListParam = new TokenOrListParam();
        for (var oneVal : parsedParam.getParamValues()) {
            var tp = new TokenParam();
            if ("not".equals(parsedParam.getModifier())) {
                tp.setModifier(TokenParamModifier.forValue(":not"));
            }
            var indexSplitToken = oneVal.indexOf('|');
            if (indexSplitToken >= 0) {
                var system = oneVal.substring(0, indexSplitToken);
                var value = oneVal.substring(indexSplitToken + 1);
                if (StringUtils.hasLength(value)) {
                    tp.setValue(value);
                }
                tp.setSystem(system);
            } else {
                tp.setValue(oneVal);
            }
            tokenOrListParam.addOr(tp);
        }
        tokenParam.addAnd(tokenOrListParam);
        selectExpression.fromFhirParams(path, tokenParam);
    }

    private static <T> void parseReference(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) throws BadDataFormatException {
        // Vérifier si le nom du paramètre contient un point (indiquant un chaining)
        if (parsedParam.getResourceTarget()!=null) {

            FhirSearchPath chainedPath = FhirSearchPath.builder()
                    .resource(path.getResource())
                    .path(parsedParam.getResourceTarget())
                    .chain(parsedParam.getParamName())
                    .build();
            // Créer les paramètres de référence pour la valeur reçue
            var referenceAndListParam = new ReferenceAndListParam();
            var referenceOrListParam = new ReferenceOrListParam();
            for (var oneVal : parsedParam.getParamValues()) {
                var referenceParam = new ReferenceParam();
                referenceParam.setValueAsQueryToken(null, null, null, oneVal);
                referenceParam.setChain(parsedParam.getResourceTarget());
                referenceOrListParam.addOr(referenceParam);
            }
            referenceAndListParam.addAnd(referenceOrListParam);
            // Intégrer le paramètre avec le chemin enrichi dans l'expression de sélection
            selectExpression.fromFhirParams(chainedPath, referenceAndListParam);
        } else {
            // Traitement classique si aucun chaining n'est présent
            var referenceAndListParam = new ReferenceAndListParam();
            var referenceOrListParam = new ReferenceOrListParam();
            for (var oneVal : parsedParam.getParamValues()) {
                var referenceParam = new ReferenceParam();
                referenceParam.setValueAsQueryToken(null, null, null, oneVal);
                referenceOrListParam.addOr(referenceParam);
            }
            referenceAndListParam.addAnd(referenceOrListParam);
            selectExpression.fromFhirParams(path, referenceAndListParam);
        }
    }



    private static <T> void parseDate(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) {
        var paramList = parsedParam.getParamValues().stream().map(dp -> {
            var p = new DateParam();
            p.setValueAsString(dp);
            return p;
        }).toList();
        selectExpression.orFromFhirParams(path, paramList);
    }

    private static <T> void parseUri(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) throws BadDataFormatException {
        var uriParam = new UriAndListParam();
        var uriOrListParam = new UriOrListParam();
        for (var oneVal : parsedParam.getParamValues()) {
            var sp = new UriParam();
            sp.setValue(oneVal);
            uriOrListParam.addOr(sp);
        }
        uriParam.addAnd(uriOrListParam);
        selectExpression.fromFhirParams(path, uriParam);
    }


    /**
     * Parse parameters from a query string
     *
     * @param queryString the query string to parse
     * @return list of parsed parameters
     */
    public static List<ParsedParam> parseParameters(String queryString) {
        var params = new ArrayList<ParsedParam>();
        if (queryString == null) {
            queryString = "";
        }
        var tokenizer = new StringTokenizer(queryString, "&");
        while (tokenizer.hasMoreTokens()) {

            // handle params:
            var token = tokenizer.nextToken();
            var offsetEquals = token.indexOf('=');
            String fullParamName;
            String paramValue = null;
            if (offsetEquals > 0) {
                fullParamName = urlDecode(token.substring(0, offsetEquals));
                paramValue = urlDecode(token.substring(offsetEquals + 1));
            } else {
                fullParamName = urlDecode(token);
            }

            if (fullParamName == null) {
                continue;
            }

            // Initialize variables for resource target, parameter name, and modifier
            String resourceTarget = null;
            String finalParamName;
            String modifier = null;

            // Split on '.' to separate resource target if present
            int dotIndex = fullParamName.indexOf('.');
            String paramNamePart;
            if (dotIndex > 0) {
                resourceTarget = fullParamName.substring(0, dotIndex);
                paramNamePart = fullParamName.substring(dotIndex + 1);
            } else {
                paramNamePart = fullParamName;
            }

            // Split on ':' to separate modifier if present
            int colonIndex = paramNamePart.indexOf(':');
            if (colonIndex > 0) {
                finalParamName = paramNamePart.substring(0, colonIndex);
                modifier = paramNamePart.substring(colonIndex + 1);
            } else {
                finalParamName = paramNamePart;
            }

            var parsedParamValues = new ArrayList<String>();

            // handle values:
            if (paramValue != null) {
                var tokenizerValues = new StringTokenizer(paramValue, ",");
                while (tokenizerValues.hasMoreTokens()) {
                    var next = tokenizerValues.nextToken();
                    parsedParamValues.add(next);
                }
            }

            params.add(ParsedParam.builder()
                    .resourceTarget(resourceTarget)  // now setting resourceTarget
                    .paramName(finalParamName)
                    .paramValues(parsedParamValues)
                    .modifier(modifier)
                    .build());
        }

        return params;
    }


    public static void handleCountParam(SelectExpression<?> selectExpression, ParsedParam parsedParam) throws BadSelectExpression {
        try {
            if (!parsedParam.getParamValues().isEmpty()) {
                var count = Integer.parseInt(parsedParam.getParamValues().get(0));
                selectExpression.setCount(count);
            }
        } catch (Exception e) {
            throw new BadSelectExpression("The _count parameter must be in the FHIR format with an integer value : \"_count=30\"");
        }
    }

    public static void handleTotalParam(SelectExpression<?> selectExpression, ParsedParam parsedParam) throws BadSelectExpression {
        if (!parsedParam.getParamValues().isEmpty()) {
            selectExpression.setTotalMode(parsedParam.getParamValues().get(0));
        }
    }

    public static void handleElementsParam(SelectExpression<?> selectExpression, ParsedParam parsedParam) {
        if (!parsedParam.getParamValues().isEmpty()) {
            selectExpression.setElements(new HashSet<>(parsedParam.getParamValues()));
        }
    }


    public static void handleIncludeParam(SearchConfigService searchConfigService, SelectExpression<?> selectExpression, String resourceType, ParsedParam parsedParam) throws BadDataFormatException, BadSelectExpression {
        var includes = new HashSet<Include>();
        for (var val : parsedParam.getParamValues()) {

            if ("*".equals(val)) {
                // add all references:
                var resource = selectExpression.getFhirResource();
                searchConfigService.getAllByFhirResource(resource).stream().filter(p -> "reference".equals(p.getSearchType())).forEach(p ->
                        includes.add(new Include(resource + ":" + p.getUrlParameter()))
                );
            } else {
                checkParametersInclude(searchConfigService, resourceType, val);
                includes.add(new Include(val));
            }
        }
        selectExpression.fromFhirParams(includes);
    }

    public static void handleRevIncludeParam(SearchConfigService searchConfigService, SelectExpression<?> selectExpression, String resourceType, ParsedParam parsedParam) throws BadDataFormatException, BadSelectExpression {
        var includes = new HashSet<Include>();
        for (var val : parsedParam.getParamValues()) {
            checkParametersRevInclude(searchConfigService, resourceType, val);
            includes.add(new Include(val));
        }
        selectExpression.fromFhirParamsRevInclude(includes);
    }

    private static <T> void handleHasParams(SelectExpression<T> selectExpression, ParsedParam parsedParam) throws BadSelectExpression {
        // Vérification et extraction du modificateur qui doit avoir le format : TargetResource:referenceField:searchParam
        String modifier = parsedParam.getModifier();
        if (modifier == null) {
            throw new BadSelectExpression("Le paramètre _has doit contenir un modificateur avec le format : _has:TargetResource:referenceField:searchParam");
        }
        String[] parts = modifier.split(":");
        if (parts.length != 3) {
            throw new BadSelectExpression("Format du modificateur _has incorrect. Attendu : _has:TargetResource:referenceField:searchParam");
        }
        String targetResource = parts[0];
        String referenceField = parts[1];
        String searchParamName = parts[2];

        // Création du paramètre HasAndListParam
        HasAndListParam hasAndListParam = new HasAndListParam();
        HasOrListParam hasOrListParam = new HasOrListParam();

        // Pour chaque valeur fournie, créer un HasParam
        for (String value : parsedParam.getParamValues()) {
            HasParam hasParam = new HasParam(targetResource, referenceField, searchParamName, value);
            hasOrListParam.addOr(hasParam);
        }
        hasAndListParam.addAnd(hasOrListParam);

        // Ajout de la condition _has à l'expression de sélection
        selectExpression.fromFhirParams(hasAndListParam);
    }

    private static void checkParametersInclude(SearchConfigService searchConfigService, String resourceType, String includeValue) throws BadSelectExpression {
        String resource = includeValue.split(":")[0];
        if (resourceType.equals(resource)) {
            checkParameters(searchConfigService, resourceType, includeValue);
        } else {
            throwBadSelectExpression(resourceType, includeValue);
        }
    }

    private static void checkParametersRevInclude(SearchConfigService searchConfigService, String resourceType, String includeValue) throws BadSelectExpression {
        checkParameters(searchConfigService, resourceType, includeValue);
    }

    private static void checkParameters(SearchConfigService searchConfigService, String resourceType, String includeValue) throws BadSelectExpression {
        String[] parts = includeValue.split(":");
        if (parts.length != 2 || searchConfigService.getResources().stream().noneMatch(res -> res.equals(parts[0]))
                || searchConfigService.getAllByFhirResource(parts[0]).stream().noneMatch(res -> res.getUrlParameter().equals(parts[1]))) {
            throwBadSelectExpression(resourceType, includeValue);
        }
    }

    private static void throwBadSelectExpression(String resourceType, String includeValue) throws BadSelectExpression {
        throw new BadSelectExpression(String.format("Parameter %s not found for resource %s", includeValue, resourceType));
    }
}
