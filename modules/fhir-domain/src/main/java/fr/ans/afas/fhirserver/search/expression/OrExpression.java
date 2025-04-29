/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression;


import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/*
 * A base class for a logical "OR" expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public abstract class OrExpression<T> implements ContainerExpression<T> {
    protected final List<Expression<T>> expressions = new ArrayList<>();

    /**
     * Add an expression to the logical OR expression.
     * This method is similar to {@link OrExpression#addExpression(Expression)}.
     *
     * @param expression the expression to add
     * @return the current OR expression
     */
    public OrExpression<T> or(Expression<T> expression) {
        this.expressions.add(expression);
        return this;
    }

    @Override
    public ContainerExpression<T> addExpression(Expression<T> expression) {
        return this.or(expression);
    }

    @Override
    public String toString() {
        return toString(expressions, " OR ");
    }

}
