/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;

/**
 * A search expression.
 *
 * @param <T> the type returned when the expression is interpreted
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface Expression<T> {

    /**
     * The separator used to tokenize serialization. Used between expressions.
     */
    String SERIALIZE_SEPARATOR = "|";

    /**
     * The separator used to tokenize field for serialization.
     */
    String SERIALIZE_VALUE_SEPARATOR = "$";

    /**
     * Interprets the expression
     *
     * @param expressionContext the context
     * @return the interpreted expression
     */
    T interpreter(ExpressionContext expressionContext);

    /**
     * Serialize the expression
     *
     * @param expressionSerializer the serializer
     * @return a string representation of the expression
     */
    String serialize(ExpressionSerializer<T> expressionSerializer);


    Expression<T> deserialize(ExpressionSerializer<T> expressionDeserializer);

}
