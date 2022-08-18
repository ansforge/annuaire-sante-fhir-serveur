/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.exception;

/**
 * Exception when the configuration is not supported
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class BadConfigurationException extends RuntimeException {

    /**
     * Construct the exception
     *
     * @param message the cause of the configuration error
     */
    public BadConfigurationException(String message) {
        super(message);
    }
}
