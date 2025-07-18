/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.emptyimpl;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.TokenExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;

public class EmptyTokenExpression extends TokenExpression<String> {
    /**
     * Constructor
     *
     * @param fhirPath The fhir path where to find
     * @param system   The system
     * @param code     The code
     */
    public EmptyTokenExpression(FhirSearchPath fhirPath, String system, String code, Operator operator) {
        super(fhirPath, system, code,operator);
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
