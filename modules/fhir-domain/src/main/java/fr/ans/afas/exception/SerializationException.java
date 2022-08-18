/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.exception;

/**
 * Exception in the (de)serialization process
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class SerializationException extends RuntimeException {
    /**
     * Construct the exception
     *
     * @param message the error description
     */
    public SerializationException(String message) {
        super(message);
    }

    /**
     * Construct the exception
     *
     * @param message the error description
     * @param cause   the cause exception
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
