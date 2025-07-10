/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbStringExpression;
import org.bson.conversions.Bson;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Deserialize a String expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class StringDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfigService searchConfigService, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        var parts = val.split("\\$");
        var operator = StringExpression.Operator.values()[Integer.parseInt(parts[0])];
        var valueData = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        var resource = parts[2];
        var path = parts[3];
        var fhirSearchPath = FhirSearchPath.builder().resource(resource).path(path).build();
        return new MongoDbStringExpression(searchConfigService, fhirSearchPath, valueData, operator);
    }
}
