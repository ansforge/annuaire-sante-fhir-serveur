/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionDeserializer;
import fr.ans.afas.utils.ExpressionSerializationUtils;
import fr.ans.afas.utils.PatternMatching;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Deserialize expressions for mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Component
public class MongoDbExpressionDeserializer implements ExpressionDeserializer<Bson> {


    /**
     * The expression factory
     */
    ExpressionFactory<Bson> expressionFactory;
    /**
     * The search config
     */
    SearchConfig searchConfig;

    @Autowired
    public MongoDbExpressionDeserializer(ExpressionFactory<Bson> expressionFactory, SearchConfig searchConfig) {
        this.expressionFactory = expressionFactory;
        this.searchConfig = searchConfig;
    }

    @Override
    public Expression<Bson> deserialize(String val) {
        var typeAndValue = val.split("\\" + Expression.SERIALIZE_SEPARATOR, 2);
        var type = typeAndValue[0];
        String value = null;
        if (typeAndValue.length > 1) {
            try {
                value = URLDecoder.decode(typeAndValue[1], StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new SerializationException("Error encoding parameters during serialization");
            }
        } else {
            value = "";
        }
        final var valueFinal = value;
        var classFound = ExpressionSerializationUtils.getClassForCode(type);
        var deserializeChooser = PatternMatching
                .<Class<? extends Expression>, Expression>
                        when(SelectExpression.class::equals, x -> new SelectDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal))
                .orWhen(AndExpression.class::equals, x -> new AndDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal))
                .orWhen(OrExpression.class::equals, x -> new OrDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal))
                .orWhen(DateRangeExpression.class::equals, x -> new DateRangeDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal))
                .orWhen(QuantityExpression.class::equals, x -> new QuantityDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal))
                .orWhen(TokenExpression.class::equals, x -> new TokenDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal))
                .orWhen(ReferenceExpression.class::equals, x -> new ReferenceDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal))
                .orWhen(StringExpression.class::equals, x -> new StringDeserializeFunction().process(searchConfig, expressionFactory, this, valueFinal));

        return deserializeChooser.matches(classFound).orElseThrow();
    }


}
