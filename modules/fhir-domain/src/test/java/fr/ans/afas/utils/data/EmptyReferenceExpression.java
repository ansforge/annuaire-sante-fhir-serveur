package fr.ans.afas.utils.data;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.ReferenceExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;

public class EmptyReferenceExpression extends ReferenceExpression<String> {
    /**
     * Constructor
     *
     * @param fhirPath The fhir path where to find
     * @param type     The fhir type
     * @param id       The id
     */
    public EmptyReferenceExpression(FhirSearchPath fhirPath, String type, String id) {
        super(fhirPath, type, id);
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
