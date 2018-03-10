/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto;

import android.util.Base64;

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
        EncryptionHelper helper = new EncryptionHelper(new TestPersistence());

        helper.setPassword("password", "textto");
        assertEquals("a58761998a5bc2de205ce411c9338ad4", helper.getKey());

        helper.setPassword("password", "texttoo");
        assertEquals("28046dea17c7dbfe56275b8da7efa5eb", helper.getKey());

        helper.setPassword("ēˈmōjē\uD83D\uDE00", "textto");
        assertEquals("642669a4ff81b77cb4ee2c8192d9a985", helper.getKey());
    }

    @Test
    public void testEncryptionDecryption () {

        byte[] b = Base64.encode(new byte[4], Base64.DEFAULT);
        if (b == null) System.out.println("null");


        EncryptionHelper helper = new EncryptionHelper(new TestPersistence());
        helper.setPassword("password", "textto");

        String plaintext = "hello world!";
        String encrypted = helper.encrypt(plaintext);
        System.out.println(encrypted);
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