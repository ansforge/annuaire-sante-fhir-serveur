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
     * The fhir path where to find
     */
    protected final FhirSearchPath fhirPath;
    /**
     * The value to search
     */
    protected final String value;
    /**
     * The operator
     */
    protected final Operator operator;

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
