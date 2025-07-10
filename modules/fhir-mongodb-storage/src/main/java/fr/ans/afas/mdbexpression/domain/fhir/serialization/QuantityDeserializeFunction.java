/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbQuantityExpression;
import org.bson.conversions.Bson;

/**
 * Deserialize a Quantity expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class QuantityDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfigService searchConfigService, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        var parts = val.split("\\$");
        var operator = QuantityExpression.Operator.values()[Integer.parseInt(parts[0])];
        var doubleValue = Double.parseDouble(parts[1]);
        var resource = parts[2];
        var path = parts[3];
        var fhirSearchPath = FhirSearchPath.builder().resource(resource).path(path).build();
        return new MongoDbQuantityExpression(searchConfigService, fhirSearchPath, doubleValue, operator);
    }
}
