/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.*;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.util.Date;

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
    SearchConfig searchConfig;


    @Autowired
    public MongoDbExpressionFactory(SearchConfig searchConfig) {
        this.searchConfig = searchConfig;
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
        return new MongoDbQuantityExpression(this.searchConfig, fhirPath, value, operator);
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
        return new MongoDbStringExpression(searchConfig, fhirPath, value, operator);
    }

    /**
     * Create a new token expression
     *
     * @param fhirPath path on which the expression applies
     * @param system   the system of the token
     * @param value    the value of the token
     * @return the expression
     */
    @Override
    public TokenExpression<Bson> newTokenExpression(FhirSearchPath fhirPath, String system, String value) {
        return new MongoDbTokenExpression(searchConfig, fhirPath, system, value);
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
            return new MongoDbReferenceExpression(searchConfig, fhirPath, parts[0], parts[1]);
        } else {
            return new MongoDbReferenceExpression(searchConfig, fhirPath, null, reference);
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
        return new MongoDbIncludeExpression(searchConfig, type, name);
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
        return new MongoDbDateRangeExpression(searchConfig, path, value, precision, prefix);
    }

}
