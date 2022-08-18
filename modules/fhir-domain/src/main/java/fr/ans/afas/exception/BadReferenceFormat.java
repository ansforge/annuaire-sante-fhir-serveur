/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.exception;

import lombok.Getter;

/**
 * Exception when a reference is not well formated
 */
@Getter
public class BadReferenceFormat extends Exception {

    /**
     * The bad reference
     */
    private final String reference;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param reference the reference
     */
    public BadReferenceFormat(String reference) {
        this.reference = reference;
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message   the detail message. The detail message is saved for
     *                  later retrieval by the {@link #getMessage()} method.
     * @param reference the reference
     */
    public BadReferenceFormat(String message, String reference) {
        super(message);
        this.reference = reference;
    }


}
