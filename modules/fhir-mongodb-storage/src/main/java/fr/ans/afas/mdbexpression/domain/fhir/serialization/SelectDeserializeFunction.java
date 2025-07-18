/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbIncludeExpression;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class SelectDeserializeFunction implements DeserializeFunction<Bson> {
    @Override
    public Expression<Bson> process(SearchConfigService searchConfigService, ExpressionFactory<Bson> expressionFactory, ExpressionSerializer<Bson> expressionDeserializer, String val) {
        var parts = val.split("\\$", -1);
        if (parts.length != 6) {
            throw new SerializationException("Error during the Select deserialization. 6 parameters wanted. " + parts.length + " found. Params: " + val);
        }
        var resourceType = parts[0];
        var count = parts[1];
        var expression = parts[2];
        var include = parts[3];
        var revinclude = parts[4];
        var has = parts[5];
        var se = new SelectExpression<>(resourceType, expressionFactory, (ContainerExpression<Bson>) expressionDeserializer.deserialize(expression));
        se.setCount(Integer.parseInt(count));
        se.getIncludes().addAll(deserializeInclude(searchConfigService, include));
        se.getRevincludes().addAll(deserializeInclude(searchConfigService, revinclude));


        if (has.length() > 0) {
            se.addHasCondition((HasCondition<Bson>) expressionDeserializer.deserialize(URLDecoder.decode(has, StandardCharsets.UTF_8)));
        }

        return se;
    }

    private Set<IncludeExpression<Bson>> deserializeInclude(SearchConfigService sc, String s) {
        if (!StringUtils.hasLength(s)) {
            return new HashSet<>();
        }
        var toRet = new HashSet<IncludeExpression<Bson>>();
        var parts = s.split(Pattern.quote(Expression.SERIALIZE_SEPARATOR));
        if (parts.length % 2 != 0) {
            throw new SerializationException("Error during the Select deserialization. (Rev)include not well formatted.");
        }
        for (var i = 0; i < parts.length; i += 2) {
            toRet.add(new MongoDbIncludeExpression(sc, parts[i], parts[i + 1]));
        }
        return toRet;
    }
}
