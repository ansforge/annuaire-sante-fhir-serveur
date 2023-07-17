/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import lombok.Getter;
import org.springframework.util.StringUtils;

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
     * To prefix search path with something (used in deep objects).
     * For example, if you want to search in a sub object sub : {"main": {"field1": "1234"}, "sub": {"fieldB": "1234"}}
     * You van set the prefix of the context to "sub".
     * If prefix is null or empty, will search on the root path.
     */
    private String prefix = "";


    public ExpressionContext() {
    }

    public ExpressionContext(String prefix) {
        this.setPrefix(prefix);
    }

    /**
     * The depth of the expression
     */
    public void increment() {
        this.depth++;
    }


    public void setPrefix(String prefix) {
        this.prefix = StringUtils.hasLength(prefix) ? prefix + "." : "";
    }
}
