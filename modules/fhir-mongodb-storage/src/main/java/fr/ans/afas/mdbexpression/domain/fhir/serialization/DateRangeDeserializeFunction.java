/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbDateRangeExpression;
import org.bson.conversions.Bson;

import java.util.Date;

/**
 * Deserialize a DateRange expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class DateRangeDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfig searchConfig, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        var parts = val.split("\\$");
        var date = new Date(Long.parseLong(parts[0]));
        var resource = parts[1];
        var path = parts[2];
        var precision = TemporalPrecisionEnum.values()[Integer.parseInt(parts[3])];
        var prefix = ParamPrefixEnum.values()[Integer.parseInt(parts[4])];
        var fhirSearchPath = FhirSearchPath.builder().resource(resource).path(path).build();
        return new MongoDbDateRangeExpression(searchConfig, fhirSearchPath, date, precision, prefix);
    }
}
