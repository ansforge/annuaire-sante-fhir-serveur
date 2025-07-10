/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

/**
 * Exception when a data is not well formated
 */
public class BadDataFormatException extends RuntimeException implements PublicException {

    public BadDataFormatException(String message) {
        super(message);
    }
}
