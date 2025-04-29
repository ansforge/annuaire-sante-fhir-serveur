/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import lombok.Getter;
import org.bson.conversions.Bson;

import javax.validation.constraints.NotNull;

/**
 * Implementation of the quantity expression for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class MongoDbQuantityExpression extends QuantityExpression<Bson> {


    final SearchConfigService searchConfigService;

    /**
     * Create a mongo db quantity expression
     *
     * @param searchConfigService the search config
     * @param fhirPath            the fhir path
     * @param value               the value to check
     * @param operator            the operator
     */
    public MongoDbQuantityExpression(@NotNull SearchConfigService searchConfigService, @NotNull FhirSearchPath fhirPath, @NotNull Number value, @NotNull Operator operator) {
        super(fhirPath, value, operator);
        this.searchConfigService = searchConfigService;
    }

    /**
     * Return the mongo db filter that match the expression
     *
     * @param expressionContext the expression context
     * @return the mongodb filter
     */
    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfigService.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }
        String path = expressionContext.getPrefix() + config.get().getIndexName();

        switch (operator) {
            case EQUALS:
                return Filters.eq(path, value);
            case GT:
                return Filters.gt(path, value);
            case LT:
                return Filters.lt(path, value);
            case LE:
                return Filters.lte(path, value);
            case GE:
                return Filters.gte(path, value);
            case NE:
                return Filters.ne(path, value);
            default:
                throw new BadConfigurationException("Unsupported operation");
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
