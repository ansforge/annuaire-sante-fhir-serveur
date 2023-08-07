/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service.exception;

/**
 * Generic exception when something was wrong with the indexing process
 */
public class IndexingException extends RuntimeException{
    public IndexingException(Throwable cause) {
        super(cause);
    }
}
