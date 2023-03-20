/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.exception;

/**
 * Exception when a query parameter is not supported
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class BadParametersException extends RuntimeException {

    /**
     * Construct the exception
     *
     * @param message the cause of the parameter error
     */
    public BadParametersException(String message) {
        super(message);
    }
}
