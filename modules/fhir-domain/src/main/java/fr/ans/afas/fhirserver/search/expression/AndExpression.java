/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import java.util.ArrayList;
import java.util.List;

/*
 * A base class for a logical "AND" expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public abstract class AndExpression<T> implements ContainerExpression<T> {

    /**
     * List of expression contained is this AND expression
     */
    protected final List<Expression<T>> expressions = new ArrayList<>();

    /**
     * Add an expression with AND
     *
     * @param expression the expression to add
     * @return the expression
     */
    protected AndExpression<T> and(Expression<T> expression) {
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
        return this.and(expression);
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
        var sb = new StringBuilder();
        var i = 0;
        sb.append("(");
        for (var e : this.expressions) {
            if (i++ > 0) {
                sb.append(" AND ");
            }
            sb.append(e);
        }
        sb.append(")");
        return sb.toString();
    }
}
