/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.service.exception.ReferenceTypeNotFoundException;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbReferenceExpression;
import org.bson.conversions.Bson;

/**
 * Deserialize a Reference expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class ReferenceDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfigService searchConfigService, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        var parts = val.split("\\$");
        if (parts.length != 4) {
            throw new SerializationException("Error during the Reference deserialization. 4 parameters wanted. " + parts.length + " found. Params:" + val);
        }
        var id = parts[1];
        var resource = parts[2];
        var path = parts[3];
        var fhirSearchPath = FhirSearchPath.builder().resource(resource).path(path).build();
        var configList = searchConfigService.getAllByFhirResource(resource).stream().filter(conf ->
                conf.getSearchType().equals("reference") && conf.getUrlParameter().equals(path) && conf.getReferenceType() != null
        ).toList();

        if (!configList.isEmpty()) {
            var referenceType = configList.get(0).getReferenceType();
            return new MongoDbReferenceExpression(searchConfigService, fhirSearchPath, referenceType, id);
        } else {
            throw new ReferenceTypeNotFoundException("Parameter referenceType on SearchParamConfig is not defined for a type reference field");
        }
    }
}
