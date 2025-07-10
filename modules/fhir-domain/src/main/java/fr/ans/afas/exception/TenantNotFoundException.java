/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

/**
 * Exception when a data is not well formated
 */
public class TenantNotFoundException extends RuntimeException implements PublicException {

    public TenantNotFoundException(String message) {
        super(message);
    }
}
