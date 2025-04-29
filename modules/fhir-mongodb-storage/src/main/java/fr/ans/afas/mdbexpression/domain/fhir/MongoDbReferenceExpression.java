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
import fr.ans.afas.fhirserver.search.expression.ReferenceExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import lombok.Getter;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;

/**
 * Implementation of the reference expression for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class MongoDbReferenceExpression extends ReferenceExpression<Bson> {

    public static final String REFERENCE_DB_SUFFIX = "-reference";
    public static final String ID_DB_SUFFIX = "-id";

    /**
     * The search configuration
     */
    final SearchConfigService searchConfigService;

    /**
     * Constructor
     *
     * @param searchConfigService The search configuration
     * @param type                the type of the reference.
     * @param id                  the id of the reference.Must not be null.
     */
    public MongoDbReferenceExpression(@NotNull SearchConfigService searchConfigService, @NotNull FhirSearchPath fhirPath, String type, @NotNull String id) {
        super(fhirPath, type, id);
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
        if (StringUtils.hasLength(this.type)) {
            return Filters.eq(expressionContext.getPrefix() + config.get().getIndexName() + REFERENCE_DB_SUFFIX, this.type + "/" + this.id);
        } else {
            return Filters.eq(expressionContext.getPrefix() + config.get().getIndexName() + ID_DB_SUFFIX, this.id);
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
