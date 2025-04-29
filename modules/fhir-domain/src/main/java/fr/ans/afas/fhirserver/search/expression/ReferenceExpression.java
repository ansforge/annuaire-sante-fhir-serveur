/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import lombok.Getter;

import javax.validation.constraints.NotNull;

/**
 * A base of a reference expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public abstract class ReferenceExpression<T> implements ElementExpression<T> {

    /**
     * The fhir reference type like "Organization" in Organization/ID_1
     */
    protected final String type;
    /**
     * The fhir reference id like "ID_1" in Organization/ID_1
     */
    protected final String id;
    /**
     * The fhir path where to find
     */
    protected FhirSearchPath fhirPath;

    /**
     * Constructor
     *
     * @param fhirPath The fhir path where to find
     * @param type     the type of the reference
     * @param id       the id of the reference.  Must not be null
     */
    protected ReferenceExpression(@NotNull FhirSearchPath fhirPath, String type, @NotNull String id) {
        this.fhirPath = fhirPath;
        this.type = type;
        this.id = id;
    }

    @Override
    public String toString() {
        return "ReferenceExpression{" +
                "fhirPath=" + fhirPath +
                ",type='" + type + '\'' +
                ",id='" + id + '\'' +
                '}';
    }

    @Override
    public void setFhirPath(FhirSearchPath path) {
        this.fhirPath = path;
    }
}
