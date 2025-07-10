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
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.utils.MongoDbUtils;
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

    public static final String INSENSITIVE_SUFFIX = "-i";

    /**
     * the search configuration
     */
    final SearchConfigService searchConfigService;

    /**
     * Build a sting expression
     *
     * @param searchConfigService The search configuration
     * @param fhirPath            the fhir path
     * @param value               the value
     * @param operator            the operator
     */
    public MongoDbStringExpression(@NotNull SearchConfigService searchConfigService, @NotNull FhirSearchPath fhirPath, @NotNull String value, @NotNull Operator operator) {
        super(fhirPath, value, operator);
        this.searchConfigService = searchConfigService;
    }

    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfigService.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }
        String normalizedInput = MongoDbUtils.removeAccentsAndLowerCase(value);

        Bson ret;
        switch (operator) {
            case EXACT:
                ret = Filters.eq(expressionContext.getPrefix() + config.get().getIndexName(), value);
                break;
            case EQUALS:
                ret = Filters.regex(expressionContext.getPrefix() + config.get().getIndexName() + INSENSITIVE_SUFFIX, "^" + Pattern.quote(normalizedInput));
                break;
            case CONTAINS:
            default:
                ret = Filters.regex(expressionContext.getPrefix() + config.get().getIndexName() + INSENSITIVE_SUFFIX, Pattern.quote(normalizedInput));
                break;
        }
        return ret;
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