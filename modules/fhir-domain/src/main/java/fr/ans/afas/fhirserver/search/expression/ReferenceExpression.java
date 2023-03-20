/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
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
     * The fhir path where to find
     */
    protected final FhirSearchPath fhirPath;

    /**
     * The fhir reference type like "Organization" in Organization/ID_1
     */
    protected final String type;
    /**
     * The fhir reference id like "ID_1" in Organization/ID_1
     */
    protected final String id;

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
}
