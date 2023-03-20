/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
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
     * The encryption secret key
     */
    private final String secretKey;
    /**
     * The AES encrypter
     */
    private final AesEncrypter aesEncrypter;

    public DefaultSerializeUrlEncrypter(String secretKey) {
        Assert.notNull(secretKey, "The secret key must not be null.");
        this.secretKey = secretKey;
        this.aesEncrypter = new AesEncrypter(this.secretKey);
    }

    @Override
    public String encrypt(String val) {
        return aesEncrypter.encrypt(val);
    }

    @Override
    public String decrypt(String val) {
        var original = val;
        return aesEncrypter.decrypt(original);
    }

}
