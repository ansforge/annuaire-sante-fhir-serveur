/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import fr.ans.afas.exception.SerializationException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Aes utility class to encrypt / decrypt strings
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AesEncrypter {

    /**
     * Size of the iv key
     */
    private static final int GCM_IV_LENGTH = 12;
    /**
     * Length of the tag
     */
    private static final int GCM_TAG_LENGTH = 16;

    /**
     * The secret key
     */
    private SecretKeySpec secretKey;


    /**
     * Construct the encrypter
     *
     * @param secretKey the secret key to encrypt
     */
    public AesEncrypter(String secretKey) {
        this.setKey(secretKey);
    }


    /**
     * Create the secret key
     *
     * @param myKey the string key
     */
    protected void setKey(final String myKey) {
        try {
            var key = myKey.getBytes(StandardCharsets.UTF_8);
            var sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, GCM_TAG_LENGTH);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new SerializationException("Can't serialize parameters. Encryption (algo) error.");
        }
    }

    /**
     * Encrypt a string with AES
     *
     * @param strToEncrypt the string to encrypt
     * @return the encrypted string
     */
    public String encrypt(final String strToEncrypt) {
        try {
            var iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            var ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);


            var ciphertext = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            var encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);

            return Base64.getUrlEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new SerializationException("Can't serialize parameters.", e);
        }
    }

    /**
     * Decrypt a string with AES
     *
     * @param strToDecrypt the string to decrypt
     * @return the decrypted string
     */
    public String decrypt(final String strToDecrypt) {
        try {
            var decoded = Base64.getUrlDecoder().decode(strToDecrypt);
            var iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            var ivSpec = new GCMParameterSpec(GCM_TAG_LENGTH * Byte.SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            return new String(cipher.doFinal(decoded, GCM_IV_LENGTH, decoded.length - GCM_IV_LENGTH), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SerializationException("Can't deserialize parameters.", e);
        }
    }
}
