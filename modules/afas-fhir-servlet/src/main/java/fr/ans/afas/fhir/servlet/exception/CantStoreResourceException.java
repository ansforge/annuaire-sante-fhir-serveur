/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

public class CantStoreResourceException extends Exception implements PublicException {
    public CantStoreResourceException() {
    }

    public CantStoreResourceException(String message) {
        super(message);
    }
}
