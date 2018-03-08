/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto;

import com.moduloapps.textto.encryption.EncryptionHelper;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;

/**
 * Created by hudson on 3/7/18.
 */
public class EncryptionTests {

    @Test
    public void testSetPassword () {
        EncryptionHelper helper = new EncryptionHelper();

        helper.setPassword("password", "textto");
        assertEquals("87bb305c10fe451c6c1bc1363fd5656a", helper.getKey());

        helper.setPassword("password", "texttoo");
        assertEquals("777cca91781909138d0b16a927fb1c4f", helper.getKey());

        helper.setPassword("ēˈmōjē\uD83D\uDE00", "textto");
        assertEquals("fb85042b72ecc58ee3080792f06e60d0", helper.getKey());
    }

    @Test
    public void testEncryptionDecryption () {
        EncryptionHelper helper = new EncryptionHelper();
        helper.setPassword("password", "textto");

        String plaintext = "hello world!";
        String encrypted = helper.encrypt(plaintext);
        assertNotSame(encrypted.substring(32), plaintext);
        String decrypted = helper.decrypt(encrypted);
        assertEquals(plaintext, decrypted);

        plaintext = "ēˈmōjē\uD83D\uDE00";
        encrypted = helper.encrypt(plaintext);
        assertNotSame(encrypted.substring(32), plaintext);
        decrypted = helper.decrypt(encrypted);
        assertEquals(plaintext, decrypted);

        plaintext = "ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¯Ã¢â‚¬Â Ã‚ï†";
        encrypted = helper.encrypt(plaintext);
        assertNotSame(encrypted.substring(32), plaintext);
        decrypted = helper.decrypt(encrypted);
        assertEquals(plaintext, decrypted);


    }
}