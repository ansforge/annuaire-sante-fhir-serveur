/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import lombok.Getter;

/**
 * A context used for the interpretation of expressions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class ExpressionContext {

    /**
     * Depth of the current expression
     */
    private int depth = 0;

    /**
     * The depth of the expression
     */
    public void increment() {
        this.depth++;
    }
}
