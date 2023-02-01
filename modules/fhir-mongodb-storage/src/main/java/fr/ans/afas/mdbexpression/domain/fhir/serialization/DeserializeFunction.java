/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;

/**
 * Fonctional interface for deserialize functions
 *
 * @param <T>
 */
@FunctionalInterface
public interface DeserializeFunction<T> {

    /**
     * Deserialize an expression from a string
     *
     * @param searchConfig         the search config
     * @param expressionFactory    the expression factory
     * @param expressionSerializer the expression serializer
     * @param val                  the serialized version of the expression
     * @return the expression
     */
    Expression<T> process(SearchConfig searchConfig, ExpressionFactory<T> expressionFactory, ExpressionSerializer<T> expressionSerializer, String val);

}
