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

    static final String[] SPECIAL_PARAMS = new String[]{"_count", "_pretty", "_format", "_total", "_include", "_revinclude", "_elements"};


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
                    case "_pretty", "_format":
                    default:
                        break;

                }

            } else {
                // classic params
                var path = FhirSearchPath.builder().resource(resourceType).path(parsedParam.paramName).build();
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
        var referenceAndListParam = new ReferenceAndListParam();
        var referenceOrListParam = new ReferenceOrListParam();
        for (var oneVal : parsedParam.getParamValues()) {
            var referenceParam = new ReferenceParam();
            referenceParam.setValue(oneVal);
            referenceOrListParam.addOr(referenceParam);
        }
        referenceAndListParam.addAnd(referenceOrListParam);
        selectExpression.fromFhirParams(path, referenceAndListParam);
    }


    private static <T> void parseDate(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) {
        var paramList = parsedParam.getParamValues().stream().map(dp -> {
            var p = new DateParam();
            p.setValueAsString(dp);
            return p;
        }).toList();
        selectExpression.orFromFhirParams(path, paramList);
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
            String paramName;
            String paramValue = null;
            if (offsetEquals > 0) {
                paramName = urlDecode(token.substring(0, offsetEquals));
                paramValue = urlDecode(token.substring(offsetEquals + 1));
            } else {
                paramName = urlDecode(token);
            }

            if (paramName == null) {
                continue;
            }

            // handle modifiers:
            String parsedParamName;
            String parsedModifier = null;
            var offsetModifier = paramName.indexOf(':');
            if (offsetModifier > 0) {
                parsedParamName = paramName.substring(0, offsetModifier);
                parsedModifier = paramName.substring(offsetModifier + 1);
            } else {
                parsedParamName = paramName;
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
                    .paramName(parsedParamName)
                    .paramValues(parsedParamValues)
                    .modifier(parsedModifier)
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
