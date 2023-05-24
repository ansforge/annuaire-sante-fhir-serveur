/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.emptyimpl;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;

public class EmptyStringExpression extends StringExpression<String> {
    /**
     * Constructor
     *
     * @param fhirPath The fhir path where to find
     * @param value    The value to search
     * @param operator The operator
     */
    public EmptyStringExpression(FhirSearchPath fhirPath, String value, Operator operator) {
        super(fhirPath, value, operator);
    }

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
        return new EmptyAndExpression();
    }
}
