/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;


import fr.ans.afas.fhirserver.search.FhirSearchPath;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * A base of a string expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public abstract class StringExpression<T> implements ElementExpression<T> {

    /**
     * The value to search
     */
    protected final String value;
    /**
     * The operator
     */
    protected final Operator operator;
    /**
     * The fhir path where to find
     */
    protected FhirSearchPath fhirPath;

    /**
     * Constructor
     *
     * @param fhirPath The fhir path where to find
     * @param value    The value to search
     * @param operator The operator
     */
    protected StringExpression(@NotNull FhirSearchPath fhirPath, @NotNull String value, @NotNull Operator operator) {
        this.fhirPath = fhirPath;
        this.value = value;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "StringExpression{" +
                "fhirPath=" + fhirPath +
                ",value='" + value + '\'' +
                ",operator=" + operator +
                '}';
    }

    @Override
    public void setFhirPath(FhirSearchPath path) {
        this.fhirPath = path;
    }

    /**
     * Operator for String expressions
     */
    public enum Operator {
        /**
         * Equals
         */
        EQUALS,
        /**
         * Contains
         */
        CONTAINS,
        /**
         * Exact (terms match exactly: full match and case sensitive)
         */
        EXACT
    }
}
