/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.exception;

public class UnknownErrorWritingResponse extends Exception {

    public UnknownErrorWritingResponse(String message) {
        super(message);
    }
}
