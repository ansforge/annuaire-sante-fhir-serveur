/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service.exception;

/**
 * Exception when user supply bad parameters to the search service
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class ReferenceTypeNotFoundException extends RuntimeException implements PublicException {

    /**
     * Construct the request exception
     *
     * @param message the cause of the exception
     */
    public ReferenceTypeNotFoundException(String message) {
        super(message);
    }
}
