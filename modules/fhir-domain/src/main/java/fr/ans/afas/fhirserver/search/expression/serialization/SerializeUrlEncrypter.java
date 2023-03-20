/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.serialization;

/**
 * Encrypt / decrypt serialized url.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface SerializeUrlEncrypter {

    /**
     * Encrypt the url to another
     *
     * @param val the value to encrypt
     * @return then encrypted version of the string
     */
    String encrypt(String val);

    /**
     * Decrypt the url to the original value
     *
     * @param val the value
     * @return the original value of the string
     */
    String decrypt(String val);


}
