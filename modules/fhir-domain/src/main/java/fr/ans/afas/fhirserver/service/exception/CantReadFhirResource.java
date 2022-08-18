/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service.exception;


/**
 * When an unexpected error occurred reading an FHIR resource
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class CantReadFhirResource extends RuntimeException {
    /**
     * Constructs a new exception with the error message
     *
     * @param message the error message
     */
    public CantReadFhirResource(String message) {
        super(message);
    }
}
