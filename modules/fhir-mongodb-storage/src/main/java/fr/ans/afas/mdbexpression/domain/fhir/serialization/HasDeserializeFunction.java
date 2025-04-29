/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.HasCondition;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;

/**
 * Deserialize a Has expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class HasDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfigService searchConfigService, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        //"$FhirResourceSub$reference_sub_path$1|2%7C0%24my%2Bname%2Bis%24FhirResourceSub%24string_sub_path"

        var parts = val.split("\\" + Expression.SERIALIZE_VALUE_SEPARATOR);
        var mongoDbOrExpression = new HasCondition<Bson>(FhirSearchPath.builder().resource(parts[1]).path(parts[2]).build());
        mongoDbOrExpression.addExpression(expressionDeserializer.deserialize(parts[3]));
        return mongoDbOrExpression;
    }
}