package com.amdc.firebasetest;

import android.annotation.SuppressLint;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Encryption extends ChatActivity{
    static byte[] encryptedBytes;

    @SuppressLint("GetInstance")
    public Encryption(String sms) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher encryptCipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec("xx777xx777xx777x".getBytes(), "AES");
        encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        encryptedBytes = encryptCipher.doFinal(sms.getBytes());
    }
}
