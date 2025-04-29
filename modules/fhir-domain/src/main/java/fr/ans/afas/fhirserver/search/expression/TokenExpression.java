/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * A base of a token expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public abstract class TokenExpression<T> implements ElementExpression<T> {

    /**
     * The system of the token
     */
    protected final String system;
    /**
     * The value of the token
     */
    protected final String value;
    /**
     * The fhir path where to find
     */
    protected FhirSearchPath fhirPath;

    /**
     * Construct a token expression
     *
     * @param fhirPath the fhir path where to find
     * @param system   the system of the token
     * @param value    the value of the token
     */
    protected TokenExpression(@NotNull FhirSearchPath fhirPath, String system, String value) {
        this.fhirPath = fhirPath;
        this.system = system;
        this.value = value;
    }

    @Override
    public String toString() {
        return "TokenExpression{" +
                "fhirPath=" + fhirPath +
                ",system='" + system + '\'' +
                ",value='" + value + '\'' +
                '}';
    }

    @Override
    public void setFhirPath(FhirSearchPath path) {
        this.fhirPath = path;
    }
}
