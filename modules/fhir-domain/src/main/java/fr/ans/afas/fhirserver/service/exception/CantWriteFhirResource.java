/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service.exception;

/**
 * When an unexpected error occurred writing an FHIR resource
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class CantWriteFhirResource extends RuntimeException implements PublicException {
    /**
     * Constructs a new exception with the error message
     *
     * @param message the error message
     */
    public CantWriteFhirResource(String message) {
        super(message);
    }

    public CantWriteFhirResource(String message, Throwable cause) {
        super(message, cause);
    }
}
