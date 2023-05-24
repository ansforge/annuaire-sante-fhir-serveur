/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.http;

import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;


/**
 * Parse fhir requests urls
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirRequestParser {


    private FhirRequestParser() {
    }

    public static String urlDecode(String string) {
        if (string == null) {
            return null;
        }
        return URLDecoder.decode(string, StandardCharsets.UTF_8);
    }


    public static <T> SelectExpression<T> parseSelectExpression(String url, ExpressionFactory<T> expressionFactory, SearchConfig searchConfig) throws BadSelectExpression {
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

            if (parsedParam.paramName.startsWith("_") && !parsedParam.paramName.equals("_id")) {
                handleSpecialParams(selectExpression, parsedParam);
                continue;
            }

            var path = FhirSearchPath.builder().resource(resourceType).path(parsedParam.paramName).build();
            var sc = searchConfig.getSearchConfigByPath(path).orElseThrow(() -> new BadSelectExpression("Parameter " + parsedParam.paramName + " not found for resource " + resourceType));


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
                default:
                    throw new BadSelectExpression("Search type not supported");
            }

        }
        return selectExpression;
    }

    private static <T> void parseString(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) {
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

    private static <T> void parseToken(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) {
        var tokenParam = new TokenAndListParam();
        var tokenOrListParam = new TokenOrListParam();
        for (var oneVal : parsedParam.getParamValues()) {
            var tp = new TokenParam();
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


    private static <T> void parseDate(SelectExpression<T> selectExpression, ParsedParam parsedParam, FhirSearchPath path) {
        var paramList = parsedParam.getParamValues().stream().map(dp -> {
            var p = new DateParam();
            p.setValueAsString(dp);
            return p;
        }).collect(Collectors.toList());
        selectExpression.orFromFhirParams(path, paramList);
    }

    /**
     * Parse a value that have prefix (date, quantity)
     *
     * @param value the value to parse
     * @return the parsed value
     */
    public static PrefixAndValue extractPrefix(String value) {
        var prefixAndValue = new PrefixAndValue();
        if (value.length() > 2) {
            var prefix = value.substring(0, 2);
            if (prefix.matches("[a-z]*")) {
                prefixAndValue.setPrefix(ParamPrefixEnum.forValue(prefix));
                prefixAndValue.setValue(value.substring(2));
            } else {
                prefixAndValue.setValue(value);
            }
        } else {
            prefixAndValue.setValue(value);
        }
        return prefixAndValue;
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


            // handle values:
            var tokenizerValues = new StringTokenizer(paramValue, ",");
            var parsedParamValues = new ArrayList<String>();
            while (tokenizerValues.hasMoreTokens()) {
                var next = tokenizerValues.nextToken();
                parsedParamValues.add(next);
            }


            params.add(ParsedParam.builder()
                    .paramName(parsedParamName)
                    .paramValues(parsedParamValues)
                    .modifier(parsedModifier)
                    .build());

        }

        return params;
    }

    public static void handleSpecialParams(SelectExpression<?> selectExpression, ParsedParam parsedParam) throws BadSelectExpression {
        var paramName = parsedParam.getParamName();

        if ("_count".equals(paramName)) {
            try {
                if (!parsedParam.getParamValues().isEmpty()) {
                    var count = Integer.parseInt(parsedParam.getParamValues().get(0));
                    selectExpression.setCount(count);
                }
            } catch (Exception e) {
                throw new BadSelectExpression("The count parameter must be in the FHIR format with an integer value : \"_count=30\"");
            }
        }
    }
}
