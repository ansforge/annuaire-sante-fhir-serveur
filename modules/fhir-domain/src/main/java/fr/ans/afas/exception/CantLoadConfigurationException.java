/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.exception;

/**
 * Exception throw when the configuration cant be loaded.
 */
public class CantLoadConfigurationException extends RuntimeException {
    public CantLoadConfigurationException(String message) {
        super(message);
    }
}
