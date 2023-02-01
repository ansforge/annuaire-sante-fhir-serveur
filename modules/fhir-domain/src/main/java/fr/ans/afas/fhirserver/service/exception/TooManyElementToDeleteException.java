/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service.exception;

/**
 * An exception when we try to delete too many elements at a time.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class TooManyElementToDeleteException extends Exception {

    public TooManyElementToDeleteException(String message) {
        super(message);
    }
}
