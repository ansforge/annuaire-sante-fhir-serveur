/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.expression.serialization;

import fr.ans.afas.utils.AesEncrypter;
import org.springframework.util.Assert;

/***
 * Default implementation of the Url encrypter. This encrypts value with a simple AES algorithm and compress the string.
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class DefaultSerializeUrlEncrypter implements SerializeUrlEncrypter {

    /**
     * The AES encrypter
     */
    private final AesEncrypter aesEncrypter;

    public DefaultSerializeUrlEncrypter(String secretKey) {
        Assert.notNull(secretKey, "The secret key must not be null.");
        /*
          The encryption secret key
         */
        this.aesEncrypter = new AesEncrypter(secretKey);
    }

    @Override
    public String encrypt(String val) {
        return aesEncrypter.encrypt(val);
    }

    @Override
    public String decrypt(String val) {
        return aesEncrypter.decrypt(val);
    }

}
