package com.amdc.firebasetest;

import android.annotation.SuppressLint;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

@SuppressLint("Registered")
public class Decryption extends ChatActivity{
    static String decryptedSMS;

    public Decryption(String sms) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        @SuppressLint("GetInstance")
        Cipher decryptCipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec("xx777xx777xx777x".getBytes(), "AES");

        String[] msg = sms.substring(1, sms.length() - 1).split(", ");
        byte[] msgByte = new byte[msg.length];
        for (int i = 0; i < msg.length; i++) {
            try { msgByte[i] = Byte.parseByte(msg[i]);
            } catch (Exception ignored) {}
        }
        decryptCipher.init(Cipher.DECRYPT_MODE, key);
        try {
            byte[] decryptedBytes = decryptCipher.doFinal(msgByte);
            decryptedSMS = new String(decryptedBytes, StandardCharsets.UTF_8);
        }
        catch (BadPaddingException | IllegalBlockSizeException e) {
            decryptedSMS = sms;
        }
    }
}
