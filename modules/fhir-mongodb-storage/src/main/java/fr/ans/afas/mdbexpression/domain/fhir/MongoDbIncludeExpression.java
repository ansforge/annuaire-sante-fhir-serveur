/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir;

import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.IncludeExpression;
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
    final SearchConfigService searchConfigService;

    /**
     * Create a new include expression
     *
     * @param searchConfigService The search configuration
     * @param type                the fhir type
     * @param name                the name of the element to include
     */
    public MongoDbIncludeExpression(@NotNull SearchConfigService searchConfigService, @NotNull String type, @NotNull String name) {
        super(type, name);
        this.searchConfigService = searchConfigService;
    }


    /**
     * Create a mongodb expression based on the expression context
     *
     * @param expressionContext the expression context
     * @return the mongodb expression
     */
    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfigService.getSearchConfigByResourceAndParamName(type, name);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + type + "." + name);
        }
        return null;
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
