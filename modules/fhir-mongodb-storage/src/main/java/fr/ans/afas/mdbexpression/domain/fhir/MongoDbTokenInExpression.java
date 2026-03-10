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
import fr.ans.afas.fhirserver.search.expression.TokenInExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MongoDbTokenInExpression extends TokenInExpression<Bson> {

    private final SearchConfigService searchConfigService;
    private final FhirSearchPath fhirPath;
    private final List<String> values;
    private final String fieldSuffix;

    public MongoDbTokenInExpression(@NotNull SearchConfigService searchConfigService,
                                    @NotNull FhirSearchPath fhirPath,
                                    String system,
                                    @NotNull List<String> values) {
        super(fhirPath, system, values);

        this.searchConfigService = searchConfigService;
        this.fhirPath = fhirPath;

        if (system != null && !system.isBlank()) {
            // cas avec system → utiliser sysval
            this.values = values.stream()
                    .map(v -> system + "|" + v)
                    .toList();
            this.fieldSuffix = MongoDbTokenExpression.TOKEN_DB_PATH_SUFFIX_SYSVAL;
        } else {
            // cas sans system → utiliser value seul
            this.values = values;
            this.fieldSuffix = MongoDbTokenExpression.TOKEN_DB_PATH_SUFFIX_VALUE;
        }
    }

    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfigService.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }
        String field = expressionContext.getPrefix() + config.get().getIndexName() + fieldSuffix;
        return Filters.in(field, values);
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
