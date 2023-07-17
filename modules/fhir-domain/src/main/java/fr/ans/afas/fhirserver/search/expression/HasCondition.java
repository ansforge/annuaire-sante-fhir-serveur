/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class HasCondition<T> implements Expression<T> {

    private final FhirSearchPath fhirPath;

    private final List<Expression<T>> expressions = new ArrayList<>();

    public HasCondition(FhirSearchPath fhirPath) {
        this.fhirPath = fhirPath;
    }

    public void addExpression(Expression<T> e) {
        this.expressions.add(e);
    }

    @Override
    public T interpreter(ExpressionContext expressionContext) {
        throw new UnsupportedOperationException("Interpreter not supported on has expression");
    }

    @Override
    public String serialize(ExpressionSerializer<T> expressionSerializer) {
        return expressionSerializer.serialize(this);
    }

    @Override
    public Expression<T> deserialize(ExpressionSerializer<T> expressionDeserializer) {
        return null;
    }
}
