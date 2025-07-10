/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.emptyimpl;

import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.OrExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;

public class EmptyOrExpression extends OrExpression<String> {
    @Override
    public String interpreter(ExpressionContext expressionContext) {
        return "N/A";
    }

    @Override
    public String serialize(ExpressionSerializer<String> expressionSerializer) {
        return "N/A";
    }

    @Override
    public Expression<String> deserialize(ExpressionSerializer<String> expressionDeserializer) {
        return new EmptyOrExpression();
    }
}
