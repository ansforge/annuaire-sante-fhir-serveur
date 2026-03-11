/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbTokenInExpression;
import org.bson.conversions.Bson;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


/**
 * Deserialize a Token in expression
 *
 * @author Atos
 * @since 1.0.0
 */
public class TokenInDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfigService searchConfigService, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        var parts = val.split("\\$");
        var system = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
        List<String> valueData = Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length - 2))
                .map(part -> URLDecoder.decode(part, StandardCharsets.UTF_8))
                .toList();
        var resource = parts[parts.length-2];
        var path = parts[parts.length-1];
        var fhirSearchPath = FhirSearchPath.builder().resource(resource).path(path).build();
        return new MongoDbTokenInExpression(searchConfigService, fhirSearchPath, system, valueData);
    }
}

