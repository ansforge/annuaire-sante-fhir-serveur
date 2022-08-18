/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.fhirserver.search.expression.serialization.DefaultSerializeUrlEncrypter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Test the url (de)serialization process
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class UrlSerializationTest {

    /**
     * A fake AES secret key
     */
    private static final String SOME_AES_KEY = "Some AES key";

    /**
     * Test the serialization/deserialization of a string with the default encoder.
     */
    @Test
    public void testSerialization() {
        var defaultSerializeUrlEncrypter = new DefaultSerializeUrlEncrypter(SOME_AES_KEY);
        var someStringToEncode = "A|d&\r\n_ç^$/";
        var encoded = defaultSerializeUrlEncrypter.encrypt(someStringToEncode);
        var decoded = defaultSerializeUrlEncrypter.decrypt(encoded);
        Assert.assertEquals(someStringToEncode, decoded);
    }

    /**
     * Test the usage with very large urls
     */
    @Test
    public void testSerializationOfLargeUrls() {
        byte[] array = new byte[12000];
        new Random().nextBytes(array);
        String someStringToEncode = new String(array, StandardCharsets.UTF_8);
        var defaultSerializeUrlEncrypter = new DefaultSerializeUrlEncrypter(SOME_AES_KEY);
        var encoded = defaultSerializeUrlEncrypter.encrypt(someStringToEncode);
        var decoded = defaultSerializeUrlEncrypter.decrypt(encoded);
        Assert.assertEquals(someStringToEncode, decoded);
    }


    /**
     * Test multi-instance decryption
     */
    @Test
    public void testMultiInstanceDecryption() {
        byte[] array = new byte[12000];
        new Random().nextBytes(array);
        String someStringToEncode = new String(array, StandardCharsets.UTF_8);
        var defaultSerializeUrlEncrypter1 = new DefaultSerializeUrlEncrypter(SOME_AES_KEY);
        var defaultSerializeUrlEncrypter2 = new DefaultSerializeUrlEncrypter(SOME_AES_KEY);
        var encoded = defaultSerializeUrlEncrypter1.encrypt(someStringToEncode);
        var decoded = defaultSerializeUrlEncrypter2.decrypt(encoded);
        Assert.assertEquals(someStringToEncode, decoded);
    }

}
