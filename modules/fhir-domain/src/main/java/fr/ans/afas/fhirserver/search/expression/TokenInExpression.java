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
 * A base class for a logical "In" expression
 *
 * Atos
 * @since 1.0.0
 */
@Getter
public abstract class TokenInExpression<T> implements ContainerExpression<T> {

    /**
     * List of expression contained is this In expression
     */
    protected final List<Expression<T>> expressions = new ArrayList<>();

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
    protected TokenInExpression(@NotNull FhirSearchPath fhirPath, String system, List<String> values) {
        this.fhirPath = fhirPath;
        this.system = system;
        this.values = values;

    }

    /**
     * Add an expression with In
     *
     * @param expression the expression to add
     * @return the expression
     */
    protected TokenInExpression<T> in(Expression<T> expression) {
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
        return this.in(expression);
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
        sb.append("IN");
        return sb.toString().trim();
    }
}
