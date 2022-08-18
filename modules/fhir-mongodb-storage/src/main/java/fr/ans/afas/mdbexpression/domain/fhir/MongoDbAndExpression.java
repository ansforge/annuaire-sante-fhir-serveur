/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.expression.AndExpression;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionDeserializer;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of the And operation for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbAndExpression extends AndExpression<Bson> {


    /**
     * Filter.and on all expressions
     * If no expression are present, return null
     *
     * @param expressionContext the context of the expression
     * @return the request
     */
    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        List<Bson> expressionsBson = expressions.stream()
                .map(e -> e.interpreter(expressionContext))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!expressionsBson.isEmpty()) {
            if (expressionsBson.size() == 1) {
                return expressionsBson.get(0);
            } else {
                return Filters.and(expressionsBson);
            }
        } else {
            return null;
        }
    }


    @Override
    public String serialize(ExpressionSerializer expressionSerializer) {
        return expressionSerializer.serialize(this);
    }

    @Override
    public Expression<Bson> deserialize(ExpressionDeserializer expressionDeserializer) {
        return null;
    }


}
