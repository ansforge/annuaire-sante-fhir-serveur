/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * Generate request expressions (string, token...).
 *
 * @param <T> the format of an expression (depends on the implementation)
 */
public interface ExpressionFactory<T> {

    /**
     * Create a new quantity expression
     *
     * @param fhirPath path on which the expression applies
     * @param value    the value for the expression
     * @param operator an operator
     * @return the expression
     */
    QuantityExpression<T> newQuantityExpression(@NotNull FhirSearchPath fhirPath, @NotNull Number value, @NotNull QuantityExpression.Operator operator);


    /**
     * Create a new string expression
     *
     * @param fhirPath path on which the expression applies
     * @param value    the value for the expression
     * @param operator an operator
     * @return the expression
     */
    StringExpression<T> newStringExpression(FhirSearchPath fhirPath, String value, StringExpression.Operator operator);


    /**
     * Create a new token expression
     *
     * @param fhirPath path on which the expression applies
     * @param system   the system of the token
     * @param value    the value of the token
     * @return the expression
     */
    TokenExpression<T> newTokenExpression(FhirSearchPath fhirPath, String system, String value);

    /**
     * Create a new reference expression
     *
     * @param fhirPath  path on which the expression applies
     * @param reference the reference
     * @return the expression
     */
    ReferenceExpression<T> newReferenceExpression(FhirSearchPath fhirPath, String reference);

    /**
     * Create a new _has expression (reverse chaining)
     *
     * @param linkPath  the path of the field used for the reverse chaining
     * @param paramPath the search path
     * @param values    values of the expression
     * @return the _has expression
     */
    HasCondition<T> newHasExpression(FhirSearchPath linkPath, FhirSearchPath paramPath, List<String> values);

    /**
     * Create a new logical OR expression
     *
     * @return the logical OR expression
     */
    OrExpression<T> newOrExpression();

    /**
     * Create a new logical AND expression
     *
     * @return the logical AND expression
     */
    AndExpression<T> newAndExpression();

    /**
     * Create a include expression
     *
     * @param type the type of the resource
     * @param name the name of the "_include" field
     * @return the expression
     */
    IncludeExpression<T> newIncludeExpression(String type, String name);

    /**
     * Create a date expression
     *
     * @param path           path on which the expression applies
     * @param value          the value for the expression
     * @param precision      the precision of the date
     * @param queryQualifier a modifier for the expression
     * @return the expression
     */
    DateRangeExpression<T> newDateRangeExpression(FhirSearchPath path, Date value, TemporalPrecisionEnum precision, ParamPrefixEnum queryQualifier);


}
