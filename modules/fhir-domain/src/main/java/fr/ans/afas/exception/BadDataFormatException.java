/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.exception;

/**
 * Exception when a data is not well formated
 */
public class BadDataFormatException extends Exception {

    public BadDataFormatException(String message) {
        super(message);
    }
}
