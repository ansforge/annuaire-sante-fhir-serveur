/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.*;
import org.bson.conversions.Bson;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * Implementation of expression factory for mongodb.
 * Create an instance of Mongodb specifics expressions.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbExpressionFactory implements ExpressionFactory<Bson> {

    /**
     * The search config
     */
    final SearchConfigService searchConfigService;


    @Inject
    public MongoDbExpressionFactory(SearchConfigService searchConfigService) {
        this.searchConfigService = searchConfigService;
    }

    /**
     * Create a new quantity expression
     *
     * @param fhirPath path on which the expression applies
     * @param value    the value for the expression
     * @param operator an operator
     * @return the expression
     */
    @Override
    public QuantityExpression<Bson> newQuantityExpression(@NotNull FhirSearchPath fhirPath, @NotNull Number value, @NotNull QuantityExpression.Operator operator) {
        return new MongoDbQuantityExpression(this.searchConfigService, fhirPath, value, operator);
    }

    /**
     * Create a new string expression
     *
     * @param fhirPath path on which the expression applies
     * @param value    the value for the expression
     * @param operator an operator
     * @return the expression
     */
    @Override
    public StringExpression<Bson> newStringExpression(FhirSearchPath fhirPath, String value, StringExpression.Operator operator) {
        return new MongoDbStringExpression(searchConfigService, fhirPath, value, operator);
    }

    /**
     * Create a new token expression
     *
     * @param fhirPath path on which the expression applies
     * @param system   the system of the token
     * @param value    the value of the token
     * @param operator the operator to use for the token expression
     * @return the expression
     */
    @Override
    public TokenExpression<Bson> newTokenExpression(FhirSearchPath fhirPath, String system, String value, TokenExpression.Operator operator) {
        return new MongoDbTokenExpression(searchConfigService, fhirPath, system, value, operator);
    }

    /**
     * Create a new reference expression
     *
     * @param fhirPath  path on which the expression applies
     * @param reference the reference
     * @return the expression
     */
    @Override
    public ReferenceExpression<Bson> newReferenceExpression(FhirSearchPath fhirPath, String reference) {
        Assert.hasLength(reference, "The reference must have a length");
        if (reference.contains("/")) {
            var parts = reference.split("/");
            if (parts.length != 2) {
                throw new BadConfigurationException("Bad reference format. The reference format must be <Type>/<id> or <id>");
            }
            return new MongoDbReferenceExpression(searchConfigService, fhirPath, parts[0], parts[1]);
        } else {
            return new MongoDbReferenceExpression(searchConfigService, fhirPath, null, reference);
        }
    }

    /**
     * Create a new Or expression
     *
     * @return the expression
     */
    @Override
    public OrExpression<Bson> newOrExpression() {
        return new MongoDbOrExpression();
    }

    /**
     * Create a new and expression
     *
     * @return the expression
     */
    @Override
    public AndExpression<Bson> newAndExpression() {
        return new MongoDbAndExpression();
    }

    /**
     * Create a new include expression
     *
     * @param type the type of the resource
     * @param name the name of the "_include" field
     * @return the expression
     */
    @Override
    public IncludeExpression<Bson> newIncludeExpression(String type, String name) {
        return new MongoDbIncludeExpression(searchConfigService, type, name);
    }

    /**
     * Create a new date range expression
     *
     * @param path      path on which the expression applies
     * @param value     the value for the expression
     * @param precision the precision of the date
     * @param prefix    the prefix of the expression
     * @return the expression
     */
    @Override
    public DateRangeExpression<Bson> newDateRangeExpression(FhirSearchPath path, Date value, TemporalPrecisionEnum precision, ParamPrefixEnum prefix) {
        return new MongoDbDateRangeExpression(searchConfigService, path, value, precision, prefix);
    }

    @Override
    public HasCondition<Bson> newHasExpression(FhirSearchPath linkPath, FhirSearchPath paramPath, List<String> values) {

        var config = searchConfigService.getSearchConfigByPath(paramPath).orElseThrow(() -> new BadConfigurationException("Chained param doesn't exist: " + paramPath.getResource() + "." + paramPath.getPath()));

        var hasCondition = new HasCondition<Bson>(linkPath);
        if ("string".equals(config.getSearchType())) {
            newStringHasCondition(paramPath, values, hasCondition);
        } else if ("token".equals(config.getSearchType())) {
            newTokenHasCondition(paramPath, values, hasCondition);
        } else {
            throw new BadConfigurationException("Reverse chained search (_has) is only supported on string and token params ");
        }


        return hasCondition;
    }

    private void newTokenHasCondition(FhirSearchPath paramPath, List<String> values, HasCondition<Bson> hasCondition) {
        if (!values.isEmpty()) {
            var or = new MongoDbOrExpression();
            for (var value : values) {
                var ex = parseTokenValue(paramPath, value);
                or.addExpression(ex);
            }
            hasCondition.addExpression(or);
        } else {
            throw new BadConfigurationException("Reverse chained search (_has) must be used with a non empty value");
        }
    }

    private TokenExpression<Bson> parseTokenValue(FhirSearchPath paramPath, String value) {
        String systemToUse = null;
        String valueToUse = null;
        var split = value.split("\\|");
        if (split.length > 1) {
            if (StringUtils.hasLength(split[0])) {
                systemToUse = split[0];
            }
            if (StringUtils.hasLength(split[1])) {
                valueToUse = split[1];
            }
        } else {
            valueToUse = value;
        }
        return this.newTokenExpression(paramPath, systemToUse, valueToUse, TokenExpression.Operator.EQUALS);
    }

    private void newStringHasCondition(FhirSearchPath paramPath, List<String> values, HasCondition<Bson> hasCondition) {
        var or = new MongoDbOrExpression();
        for (var value : values) {
            var ex = this.newStringExpression(paramPath, value, StringExpression.Operator.EQUALS);
            or.addExpression(ex);
        }
        hasCondition.addExpression(or);
    }
}
