/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.exception;

/**
 * Exception when a select expression is not well formated
 */
public class BadSelectExpression extends Exception {

    public BadSelectExpression(String message) {
        super(message);
    }
}
