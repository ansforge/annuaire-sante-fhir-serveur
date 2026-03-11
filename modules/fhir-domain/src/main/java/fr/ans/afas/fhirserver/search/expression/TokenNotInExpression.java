/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/*
 * A base class for a logical "NotIn" expression
 *
 * Atos
 * @since 1.0.0
 */
@Getter
public abstract class TokenNotInExpression<T> implements ContainerExpression<T> {


    /**
     * The system of the token
     */
    protected final String system;
    /**
     * The value of the token
     */
    protected final List<String> values;
    /**
     * The fhir path where to find
     */
    protected FhirSearchPath fhirPath;

    /**
     * Construct a token expression
     *
     * @param fhirPath the fhir path where to find
     * @param system   the system of the token
     * @param values    the value of the token
     */
    protected TokenNotInExpression(@NotNull FhirSearchPath fhirPath, String system, List<String> values) {
        this.fhirPath = fhirPath;
        this.system = system;
        this.values = values;

    }
    /**
     * List of expression contained is this Not in expression
     */
    protected final List<Expression<T>> expressions = new ArrayList<>();

    /**
     * Add an expression with NotIn
     *
     * @param expression the expression to add
     * @return the expression
     */
    protected TokenNotInExpression<T> notIn(Expression<T> expression) {
        this.expressions.add(expression);
        return this;
    }

    /**
     * Add an expression to this expression
     *
     * @param expression the expression to add
     * @return the expression
     */
    @Override
    public ContainerExpression<T> addExpression(Expression<T> expression) {
        return this.notIn(expression);
    }


    /**
     * Get the list of expressions
     *
     * @return the list of expressions
     */
    public List<Expression<T>> getExpressions() {
        return expressions;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Expression<T> expression : expressions) {
            sb.append(expression.toString()).append(" ");
        }
        sb.append("NIN");
        return sb.toString().trim();
    }
}
