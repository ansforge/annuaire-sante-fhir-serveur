/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.OrExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;

import java.util.Objects;

/**
 * Implementation of the or expression for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbOrExpression extends OrExpression<Bson> {

    /**
     * Create a OR expression in mongodb
     *
     * @param expressionContext the context the expression context
     * @return the OR expression
     */
    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var expressionsBson = expressions.stream()
                .map(e -> e.interpreter(expressionContext))
                .filter(Objects::nonNull)
                .toList();

        if (!expressionsBson.isEmpty()) {
            if (expressionsBson.size() == 1) {
                return expressionsBson.get(0);
            } else {
                return Filters.or(expressionsBson);
            }
        } else {
            return null;
        }
    }


    @Override
    public String serialize(ExpressionSerializer<Bson> expressionSerializer) {
        return expressionSerializer.serialize(this);
    }

    @Override
    public Expression<Bson> deserialize(ExpressionSerializer<Bson> expressionDeserializer) {
        return null;
    }

}
