/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.serialization;

import fr.ans.afas.fhirserver.search.expression.Expression;

/**
 * Deserialize an expression from string to the expression
 *
 * @param <T> the type of the expression
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface ExpressionDeserializer<T> {

    /**
     * Deserialize the expression
     *
     * @param val the string value of the expression to deserialize
     * @return the expression
     */
    Expression<T> deserialize(String val);

}
