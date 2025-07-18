/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression;


import fr.ans.afas.fhirserver.search.FhirSearchPath;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * A base of a quantity expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public abstract class QuantityExpression<T> implements ElementExpression<T> {

    /**
     * The value of the expression
     */
    protected final Number value;
    /**
     * An operator (equals, greater than...)
     */
    protected final Operator operator;
    /**
     * The path where to apply the filter
     */
    protected FhirSearchPath fhirPath;

    /**
     * Construct a Quantity expression
     *
     * @param fhirPath The path where to apply the filter
     * @param value    The value of the expression
     * @param operator An operator (equals, greater than...)
     */
    protected QuantityExpression(@NotNull FhirSearchPath fhirPath, @NotNull Number value, @NotNull QuantityExpression.Operator operator) {
        this.fhirPath = fhirPath;
        this.value = value;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "QuantityExpression{" +
                "fhirPath=" + fhirPath +
                ",value=" + value +
                ",operator=" + operator +
                '}';
    }

    @Override
    public void setFhirPath(FhirSearchPath path) {
        this.fhirPath = path;
    }

    /**
     * List of allowed operator for {@link QuantityExpression}.
     */
    public enum Operator {
        /**
         * Equals
         */
        EQUALS,
        /**
         * Greater than
         */
        GT,
        /**
         * Less than
         */
        LT,
        /**
         * Less than or equals
         */
        LE,
        /**
         * Greater than or equals
         */
        GE,
        /**
         * Not equals
         */
        NE
    }
}
