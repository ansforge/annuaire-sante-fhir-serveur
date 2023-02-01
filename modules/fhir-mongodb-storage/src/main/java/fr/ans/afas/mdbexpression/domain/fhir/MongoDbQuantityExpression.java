/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
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


    final SearchConfig searchConfig;

    /**
     * Create a mongo db quantity expression
     *
     * @param searchConfig the search config
     * @param fhirPath     the fhir path
     * @param value        the value to check
     * @param operator     the operator
     */
    public MongoDbQuantityExpression(@NotNull SearchConfig searchConfig, @NotNull FhirSearchPath fhirPath, @NotNull Number value, @NotNull Operator operator) {
        super(fhirPath, value, operator);
        this.searchConfig = searchConfig;
    }

    /**
     * Return the mongo db filter that match the expression
     *
     * @param expressionContext the expression context
     * @return the mongodb filter
     */
    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfig.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }
        String path = config.get().getIndexName();

        Bson ret = null;
        switch (operator) {
            case EQUALS:
                ret = Filters.eq(path, value);
                break;
            case GT:
                ret = Filters.gt(path, value);
                break;
            case LT:
                ret = Filters.lt(path, value);
                break;
            case LE:
                ret = Filters.lte(path, value);
                break;
            case GE:
                ret = Filters.gte(path, value);
                break;
            case NE:
                ret = Filters.ne(path, value);
                break;
            default:
                throw new BadConfigurationException("Unsupported operation");
        }

        return ret;
    }


    @Override
    public String serialize(ExpressionSerializer expressionSerializer) {
        return expressionSerializer.serialize(this);
    }

    @Override
    public Expression<Bson> deserialize(ExpressionSerializer expressionDeserializer) {
        return null;
    }


}
