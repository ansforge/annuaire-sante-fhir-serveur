/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.emptyimpl;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.DateRangeExpression;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;

import java.util.Date;

public class EmptyDateExpression extends DateRangeExpression<String> {
    /**
     * Constructor
     *
     * @param fhirPath  The fhir path where to find
     * @param date      The date
     * @param precision The precision
     * @param prefix    The prefix
     */
    public EmptyDateExpression(FhirSearchPath fhirPath, Date date, TemporalPrecisionEnum precision, ParamPrefixEnum prefix) {
        super(fhirPath, date, precision, prefix);
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
