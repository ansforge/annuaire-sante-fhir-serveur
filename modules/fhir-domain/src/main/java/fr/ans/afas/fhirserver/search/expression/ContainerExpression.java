/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import java.util.List;

/**
 * An {@link Expression} that can contains other expressions
 *
 * @param <T> the output format when the expression is interpreted
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface ContainerExpression<T> extends Expression<T> {
    /**
     * Add an expression to the logical AND query
     *
     * @param expression the expression to add
     * @return the current expression.
     */
    ContainerExpression<T> addExpression(Expression<T> expression);

    /***
     * Get the list of expressions
     * @return
     */
    List<Expression<T>> getExpressions();


}
