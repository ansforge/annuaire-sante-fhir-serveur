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
    final SearchConfig searchConfig;

    /**
     * Constructor
     *
     * @param searchConfig The search configuration
     * @param fhirPath     the fhir path
     * @param system       the system
     * @param value        the value
     */
    public MongoDbTokenExpression(@NotNull SearchConfig searchConfig, @NotNull FhirSearchPath fhirPath, String system, String value) {
        super(fhirPath, system, value);
        this.searchConfig = searchConfig;
    }

    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfig.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }
        Bson ret = null;
        if (StringUtils.hasText(this.system) && StringUtils.hasLength(this.value)) {
            ret = Filters.eq(config.get().getIndexName() + TOKEN_DB_PATH_SUFFIX_SYSVAL, system + "|" + value);
        } else if (!StringUtils.hasLength(this.system)) {
            ret = Filters.eq(config.get().getIndexName() + TOKEN_DB_PATH_SUFFIX_VALUE, value);
        } else if (!StringUtils.hasLength(this.value)) {
            ret = Filters.eq(config.get().getIndexName() + TOKEN_DB_PATH_SUFFIX_SYSTEM, system);
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