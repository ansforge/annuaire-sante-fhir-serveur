/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.exception;

import fr.ans.afas.fhirserver.service.exception.PublicException;

/**
 * Exception when a select expression is not well formated
 */
public class BadSelectExpression extends Exception implements PublicException {

    public BadSelectExpression(String message) {
        super(message);
    }
}
