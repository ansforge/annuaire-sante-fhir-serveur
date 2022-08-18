/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionDeserializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbIncludeExpression;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class SelectDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression process(SearchConfig searchConfig, ExpressionFactory expressionFactory, ExpressionDeserializer expressionDeserializer, String val) {
        var parts = val.split("\\$", -1);
        if (parts.length != 6) {
            throw new SerializationException("Error during the Select deserialization. 6 parameters wanted. " + parts.length + " found. Params:" + val);
        }
        var resourceType = parts[0];
        var count = parts[1];
        var order = parts[2];
        var expression = parts[3];
        var include = parts[4];
        var revinclude = parts[5];
        var se = new SelectExpression<Bson>(resourceType, expressionFactory, (ContainerExpression<Bson>) expressionDeserializer.deserialize(expression));
        se.setCount(Integer.parseInt(count));
        se.orderBy(order);
        se.getIncludes().addAll(deserializeInclude(searchConfig, include));
        se.getRevincludes().addAll(deserializeInclude(searchConfig, revinclude));
        return se;
    }

    private Set<IncludeExpression<Bson>> deserializeInclude(SearchConfig sc, String s) {
        if (!StringUtils.hasLength(s)) {
            return new HashSet<>();
        }
        var toRet = new HashSet<IncludeExpression<Bson>>();
        var parts = s.split(Pattern.quote(Expression.SERIALIZE_SEPARATOR));
        if (parts.length % 2 != 0) {
            throw new SerializationException("Error during the Select deserialization. (Rev)include not well formated.");
        }
        for (var i = 0; i < parts.length; i += 2) {
            toRet.add(new MongoDbIncludeExpression(sc, parts[i], parts[i + 1]));
        }
        return toRet;
    }
}
