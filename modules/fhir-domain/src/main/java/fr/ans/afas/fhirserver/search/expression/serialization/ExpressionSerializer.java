/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.serialization;

import fr.ans.afas.fhirserver.search.expression.*;

/**
 * (De)Serialize an expression
 *
 * @param <T> the type of the expression (Bson for mongo etc...)
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface ExpressionSerializer<T> {

    /**
     * Serialize a And expression
     *
     * @param andExpression the end expression
     * @return a string representation of the expression
     */
    String serialize(AndExpression<T> andExpression);

    /**
     * Serialize a date range expression
     *
     * @param dateRangeExpression the date range expression
     * @return a string representation of the expression
     */
    String serialize(DateRangeExpression<T> dateRangeExpression);

    /**
     * Serialize an include expression
     *
     * @param andExpression the and expression
     * @return a string representation of the expression
     */
    String serialize(IncludeExpression<T> andExpression);

    /**
     * Serialize a or expression
     *
     * @param orExpression the or expression
     * @return a string representation of the expression
     */
    String serialize(OrExpression<T> orExpression);

    /**
     * Serialize a quantity expression
     *
     * @param quantityExpression
     * @return a string representation of the expression
     */
    String serialize(QuantityExpression<T> quantityExpression);

    /**
     * Serialize a reference expression
     *
     * @param referenceExpression the reference expression
     * @return a string representation of the expression
     */
    String serialize(ReferenceExpression<T> referenceExpression);


    /**
     * Serialize a select expression
     *
     * @param selectExpression the select expression
     * @return a string representation of the expression
     */
    String serialize(SelectExpression<T> selectExpression);

    /**
     * Serialize a string expression
     *
     * @param stringExpression the string expression
     * @return a string representation of the expression
     */
    String serialize(StringExpression<T> stringExpression);

    /**
     * Serialize a token expression
     *
     * @param tokenExpression the token expression
     * @return a string representation of the expression
     */
    String serialize(TokenExpression<T> tokenExpression);

    /**
     * Deserialize the expression
     *
     * @param val the string value of the expression to deserialize
     * @return the expression
     */
    Expression<T> deserialize(String val);

}
