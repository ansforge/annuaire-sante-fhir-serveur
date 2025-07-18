/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service.exception;

/**
 * When an error occur processing the "nexturl" link.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class BadLinkException extends Exception implements PublicException {
    /**
     * Constructs a new exception with the error message
     *
     * @param message the error message
     */
    public BadLinkException(String message) {
        super(message);
    }
}
