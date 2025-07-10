/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service.exception;

/**
 *
 */
public class DataFormatFhirException extends Exception implements PublicException {

    public DataFormatFhirException() {
        super();
    }

    public DataFormatFhirException(String message) {
        super(message);
    }

    public DataFormatFhirException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataFormatFhirException(Throwable cause) {
        super(cause);
    }
}
