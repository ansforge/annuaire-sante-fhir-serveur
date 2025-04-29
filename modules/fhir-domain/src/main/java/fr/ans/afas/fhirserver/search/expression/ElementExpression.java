/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;

/**
 * An expression linked to a field (string, token...).
 * The field is accessible with the {@link ElementExpression#getFhirPath} method.
 *
 * @param <T> the type returned when the expression is interpreted
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface ElementExpression<T> extends Expression<T> {
    FhirSearchPath getFhirPath();

    void setFhirPath(FhirSearchPath path);
}
