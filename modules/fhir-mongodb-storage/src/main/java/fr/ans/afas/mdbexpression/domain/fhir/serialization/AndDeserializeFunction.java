/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbAndExpression;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

/**
 * Deserialize a And expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AndDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfig searchConfig, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        var mongoDbAndExpression = new MongoDbAndExpression();
        if (StringUtils.hasLength(val)) {
            var parts = val.split("\\" + Expression.SERIALIZE_SEPARATOR);
            for (var i = 0; i < parts.length; i += 2) {
                mongoDbAndExpression.addExpression(expressionDeserializer.deserialize(parts[i] + Expression.SERIALIZE_SEPARATOR + parts[i + 1]));
            }
        }
        return mongoDbAndExpression;
    }
}
