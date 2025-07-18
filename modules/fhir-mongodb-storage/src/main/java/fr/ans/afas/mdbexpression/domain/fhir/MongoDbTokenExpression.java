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
import fr.ans.afas.fhirserver.search.expression.TokenExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;

/**
 * Implementation of the token expression for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbTokenExpression extends TokenExpression<Bson> {

    /**
     * Suffix of the value stored in database for the value index
     */
    public static final String TOKEN_DB_PATH_SUFFIX_VALUE = "-value";
    /**
     * Suffix of the value stored in database for the system index
     */
    public static final String TOKEN_DB_PATH_SUFFIX_SYSTEM = "-system";
    /**
     * Suffix of the value stored in database for the couple  system and value
     */
    public static final String TOKEN_DB_PATH_SUFFIX_SYSVAL = "-sysval";

    /**
     * The search configuration
     */
    final SearchConfigService searchConfigService;

    /**
     * Constructor
     *
     * @param searchConfigService The search configuration
     * @param fhirPath            the fhir path
     * @param system              the system
     * @param value               the value
     * @param operator            the operator
     */
    public MongoDbTokenExpression(@NotNull SearchConfigService searchConfigService, @NotNull FhirSearchPath fhirPath, String system, String value, @NotNull Operator operator) {
        super(fhirPath, system, value, operator);
        this.searchConfigService = searchConfigService;
    }

    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfigService.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }
        Bson ret = null;
        if (operator.equals(Operator.NOT)) {
            ret = Filters.not(Filters.eq(expressionContext.getPrefix() + config.get().getIndexName() + TOKEN_DB_PATH_SUFFIX_VALUE, value));
        } else if (StringUtils.hasText(this.system) && StringUtils.hasLength(this.value)) {
            ret = Filters.eq(expressionContext.getPrefix() + config.get().getIndexName() + TOKEN_DB_PATH_SUFFIX_SYSVAL, system + "|" + value);
        } else if (!StringUtils.hasLength(this.system)) {
            ret = Filters.eq(expressionContext.getPrefix() + config.get().getIndexName() + TOKEN_DB_PATH_SUFFIX_VALUE, value);
        } else if (!StringUtils.hasLength(this.value)) {
            ret = Filters.eq(expressionContext.getPrefix() + config.get().getIndexName() + TOKEN_DB_PATH_SUFFIX_SYSTEM, system);
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