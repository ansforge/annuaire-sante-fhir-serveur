/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

public class IdDoesntMatchException extends Exception implements PublicException {
    public IdDoesntMatchException() {
    }

    public IdDoesntMatchException(String message) {
        super(message);
    }
}
