/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service.impl.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

/**
 * Exception when a task is launched twice
 */
public class AlreadyRunningTaskException extends RuntimeException implements PublicException {

    public AlreadyRunningTaskException(String message) {
        super(message);
    }
}
