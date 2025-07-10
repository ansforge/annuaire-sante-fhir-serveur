/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

public class BadRequestException extends RuntimeException implements PublicException {

    public BadRequestException(String message) {
        super(message);
    }
}
