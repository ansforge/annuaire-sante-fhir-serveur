/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.utils.ExpressionSerializationUtils;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
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

    private static String encodeValue(String valueToEncode) {
        return URLEncoder.encode(valueToEncode, StandardCharsets.UTF_8);
    }

    @Override
    public String serialize(AndExpression<Bson> andExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(AndExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        var index = 0;
        try {
            for (var expression : andExpression.getExpressions()) {
                if (index++ != 0) {
                    sb.append(URLEncoder.encode(Expression.SERIALIZE_SEPARATOR, StandardCharsets.UTF_8.toString()));
                }
                sb.append(URLEncoder.encode(expression.serialize(this), StandardCharsets.UTF_8.toString()));
            }
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException("Error serializing expression. Unsupported encoding");
        }
        return sb.toString();
    }

    @Override
    public String serialize(DateRangeExpression<Bson> dateRangeExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(DateRangeExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        sb.append(dateRangeExpression.getDate().getTime());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(dateRangeExpression.getFhirPath().getResource());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(dateRangeExpression.getFhirPath().getPath());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(dateRangeExpression.getPrecision().ordinal());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(dateRangeExpression.getPrefix().ordinal());
        return sb.toString();
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
        try {
            for (var expression : orExpression.getExpressions()) {
                if (index++ != 0) {
                    sb.append(URLEncoder.encode(Expression.SERIALIZE_SEPARATOR, StandardCharsets.UTF_8.toString()));

                }
                sb.append(URLEncoder.encode(expression.serialize(this), StandardCharsets.UTF_8.toString()));
            }
        } catch (UnsupportedEncodingException e) {
            throw new SerializationException("Error serializing expression. Unsupported encoding");
        }
        return sb.toString();
    }

    @Override
    public String serialize(QuantityExpression<Bson> quantityExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(QuantityExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        sb.append(quantityExpression.getOperator().ordinal());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(quantityExpression.getValue().doubleValue());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(quantityExpression.getFhirPath().getResource());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(quantityExpression.getFhirPath().getPath());
        return sb.toString();
    }

    @Override
    public String serialize(ReferenceExpression<Bson> referenceExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(ReferenceExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        sb.append(referenceExpression.getType());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(referenceExpression.getId());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(referenceExpression.getFhirPath().getResource());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(referenceExpression.getFhirPath().getPath());
        return sb.toString();
    }

    @Override
    public String serialize(SelectExpression<Bson> andExpression) {
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(SelectExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        sb.append(andExpression.getFhirResource());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(andExpression.getCount());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(andExpression.getOrder());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(URLEncoder.encode(andExpression.getExpression().serialize(this), StandardCharsets.UTF_8));
        // include and rev includes:
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(URLEncoder.encode(encodeInclude(andExpression.getIncludes()), StandardCharsets.UTF_8));
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(URLEncoder.encode(encodeInclude(andExpression.getRevincludes()), StandardCharsets.UTF_8));

        return sb.toString();
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
        var sb = new StringBuilder();
        sb.append(ExpressionSerializationUtils.getCodeForClass(StringExpression.class));
        sb.append(Expression.SERIALIZE_SEPARATOR);
        sb.append(stringExpression.getOperator().ordinal());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(encodeValue(stringExpression.getValue()));
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(stringExpression.getFhirPath().getResource());
        sb.append(Expression.SERIALIZE_VALUE_SEPARATOR);
        sb.append(stringExpression.getFhirPath().getPath());
        return sb.toString();
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

}
