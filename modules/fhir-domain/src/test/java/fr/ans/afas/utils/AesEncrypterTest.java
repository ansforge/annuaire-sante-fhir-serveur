/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test the encrypter
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AesEncrypterTest {

    @Test
    public void testEncryption() {
        var s = "Some string";
        var en = new AesEncrypter("a");
        var encrypted = en.encrypt(s);
        var decrypted = en.decrypt(encrypted);
        Assert.assertEquals(s, decrypted);
    }
}
