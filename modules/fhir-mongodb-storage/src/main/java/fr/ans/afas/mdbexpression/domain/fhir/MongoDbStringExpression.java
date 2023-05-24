/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import lombok.Getter;
import org.bson.conversions.Bson;

import javax.validation.constraints.NotNull;
import java.util.regex.Pattern;

/**
 * Implementation of the string expression for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class MongoDbStringExpression extends StringExpression<Bson> {

    /**
     * the search configuration
     */
    final SearchConfig searchConfig;

    /**
     * Build a sting expression
     *
     * @param searchConfig The search configuration
     * @param fhirPath     the fhir path
     * @param value        the value
     * @param operator     the operator
     */
    public MongoDbStringExpression(@NotNull SearchConfig searchConfig, @NotNull FhirSearchPath fhirPath, @NotNull String value, @NotNull Operator operator) {
        super(fhirPath, value, operator);
        this.searchConfig = searchConfig;
    }

    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfig.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }

        Bson ret;
        switch (operator) {
            case EXACT:
                ret = Filters.eq(config.get().getIndexName(), value);
                break;
            case EQUALS:
                ret = Filters.regex(config.get().getIndexName(), "^" + Pattern.quote(value), "i");
                break;
            case CONTAINS:
            default:
                ret = Filters.regex(config.get().getIndexName(), Pattern.quote(value), "i");
                break;
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