/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

public class ResourceNotFoundException extends RuntimeException implements PublicException {
    public ResourceNotFoundException() {
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
