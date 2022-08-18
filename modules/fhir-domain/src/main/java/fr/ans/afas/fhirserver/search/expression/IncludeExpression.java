/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
public abstract class IncludeExpression<T> implements OperatorExpression<T> {
    protected final String type;
    protected final String name;

    protected IncludeExpression(@NotNull String type, @NotNull String name) {
        this.type = type;
        this.name = name;
    }
}
