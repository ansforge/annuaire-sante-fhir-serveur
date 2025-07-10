/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

/**
 * Exception when a select expression is not well formated
 */
public class ForbiddenException extends Exception implements PublicException {

    public ForbiddenException(String message) {
        super(message);
    }
}
