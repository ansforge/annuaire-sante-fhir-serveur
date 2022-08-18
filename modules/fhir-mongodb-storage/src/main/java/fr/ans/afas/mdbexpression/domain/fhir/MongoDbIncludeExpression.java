/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.IncludeExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionDeserializer;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;

import javax.validation.constraints.NotNull;

/**
 * Implementation of the include expression for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbIncludeExpression extends IncludeExpression<Bson> {

    /**
     * The search configuration
     */
    final SearchConfig searchConfig;

    /**
     * Create a new include expression
     *
     * @param searchConfig The search configuration
     * @param type         the fhir type
     * @param name         the name of the element to include
     */
    public MongoDbIncludeExpression(@NotNull SearchConfig searchConfig, @NotNull String type, @NotNull String name) {
        super(type, name);
        this.searchConfig = searchConfig;
    }


    /**
     * Create a mongodb expression based on the expression context
     *
     * @param expressionContext the expression context
     * @return the mongodb expression
     */
    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfig.getSearchConfigByResourceAndParamName(type, name);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + type + "." + name);
        }
        return null;
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
