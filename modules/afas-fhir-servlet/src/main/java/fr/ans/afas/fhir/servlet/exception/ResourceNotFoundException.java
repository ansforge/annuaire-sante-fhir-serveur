/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

public class ResourceNotFoundException extends Exception implements PublicException {
    public ResourceNotFoundException() {
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
