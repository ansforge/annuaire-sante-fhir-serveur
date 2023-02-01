/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.utils.ExpressionSerializationUtils;
import fr.ans.afas.utils.PatternMatching;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;


/**
 * Serialize expressions to string
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbExpressionSerializer implements ExpressionSerializer<Bson> {


    /**
     * The expression factory
     */
    ExpressionFactory<Bson> expressionFactory;
    /**
     * The search config
     */
    SearchConfig searchConfig;

    @Autowired
    public MongoDbExpressionSerializer(ExpressionFactory<Bson> expressionFactory, SearchConfig searchConfig) {
        this.expressionFactory = expressionFactory;
        this.searchConfig = searchConfig;
    }


    private static String encodeValue(String valueToEncode) {
        return URLEncoder.encode(valueToEncode, StandardCharsets.UTF_8);
    }

    @Override
    public String serialize(AndExpression<Bson> andExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(AndExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        var index = 0;
        for (var expression : andExpression.getExpressions()) {
            if (index++ != 0) {
                sb.append(URLEncoder.encode(Expression.SERIALIZE_SEPARATOR, StandardCharsets.UTF_8));
            }
            sb.append(URLEncoder.encode(expression.serialize(this), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Override
    public String serialize(DateRangeExpression<Bson> dateRangeExpression) {
        return ExpressionSerializationUtils.getCodeForClass(DateRangeExpression.class) +
                Expression.SERIALIZE_SEPARATOR +
                dateRangeExpression.getDate().getTime() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                dateRangeExpression.getFhirPath().getResource() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                dateRangeExpression.getFhirPath().getPath() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                dateRangeExpression.getPrecision().ordinal() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                dateRangeExpression.getPrefix().ordinal();
    }

    @Override
    public String serialize(IncludeExpression<Bson> andExpression) {
        throw new SerializationException("Include expression not supported");
    }

    @Override
    public String serialize(OrExpression<Bson> orExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(OrExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        var index = 0;
        for (var expression : orExpression.getExpressions()) {
            if (index++ != 0) {
                sb.append(URLEncoder.encode(Expression.SERIALIZE_SEPARATOR, StandardCharsets.UTF_8));

            }
            sb.append(URLEncoder.encode(expression.serialize(this), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Override
    public String serialize(QuantityExpression<Bson> quantityExpression) {
        return ExpressionSerializationUtils.getCodeForClass(QuantityExpression.class) +
                Expression.SERIALIZE_SEPARATOR +
                quantityExpression.getOperator().ordinal() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                quantityExpression.getValue().doubleValue() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                quantityExpression.getFhirPath().getResource() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                quantityExpression.getFhirPath().getPath();
    }

    @Override
    public String serialize(ReferenceExpression<Bson> referenceExpression) {
        return ExpressionSerializationUtils.getCodeForClass(ReferenceExpression.class) +
                Expression.SERIALIZE_SEPARATOR +
                referenceExpression.getType() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                referenceExpression.getId() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                referenceExpression.getFhirPath().getResource() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                referenceExpression.getFhirPath().getPath();
    }

    @Override
    public String serialize(SelectExpression<Bson> andExpression) {
        return ExpressionSerializationUtils.getCodeForClass(SelectExpression.class) +
                Expression.SERIALIZE_SEPARATOR +
                andExpression.getFhirResource() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                andExpression.getCount() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                andExpression.getOrder() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                URLEncoder.encode(andExpression.getExpression().serialize(this), StandardCharsets.UTF_8) +
                // include and rev includes:
                Expression.SERIALIZE_VALUE_SEPARATOR +
                URLEncoder.encode(encodeInclude(andExpression.getIncludes()), StandardCharsets.UTF_8) +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                URLEncoder.encode(encodeInclude(andExpression.getRevincludes()), StandardCharsets.UTF_8);

    }

    private String encodeInclude(Set<IncludeExpression<Bson>> includes) {
        var sb = new StringBuilder();
        for (var include : includes) {
            sb.append(include.getType());
            sb.append(Expression.SERIALIZE_SEPARATOR);
            sb.append(include.getName());
            sb.append(Expression.SERIALIZE_SEPARATOR);
        }
        return sb.toString();
    }

    @Override
    public String serialize(StringExpression<Bson> stringExpression) {
        return ExpressionSerializationUtils.getCodeForClass(StringExpression.class) +
                Expression.SERIALIZE_SEPARATOR +
                stringExpression.getOperator().ordinal() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                encodeValue(stringExpression.getValue()) +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                stringExpression.getFhirPath().getResource() +
                Expression.SERIALIZE_VALUE_SEPARATOR +
                stringExpression.getFhirPath().getPath();
    }

    @Override
    public String serialize(TokenExpression<Bson> tokenExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(TokenExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        if (StringUtils.hasLength(tokenExpression.getSystem())) {
            sb.append(encodeValue(tokenExpression.getSystem()));
        }
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        if (StringUtils.hasLength(tokenExpression.getValue())) {
            sb.append(encodeValue(tokenExpression.getValue()));
        }
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(tokenExpression.getFhirPath().getResource());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(tokenExpression.getFhirPath().getPath());
        return sb.toString();
    }


    @Override
    public Expression<Bson> deserialize(String val) {
        var typeAndValue = val.split("\\" + Expression.SERIALIZE_SEPARATOR, 2);
        var type = typeAndValue[0];
        String value = null;
        if (typeAndValue.length > 1) {
            value = URLDecoder.decode(typeAndValue[1], StandardCharsets.UTF_8);
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


    public String store(String serialized) {
        return serialized;
    }

    public String read(String storeId) {
        return storeId;
    }
}
