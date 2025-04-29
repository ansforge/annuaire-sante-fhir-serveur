/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.exception;

/**
 * Exception when the date precision is not supported
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class BadPrecisionException extends RuntimeException {

    /**
     * Construct the exception
     *
     * @param message the cause of the precision error
     */
    public BadPrecisionException(String message) {
        super(message);
    }
}