package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressLint("Registered")
public class Encryption extends ChatActivity{
    static byte[] encryptedBytes;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("GetInstance")
    public Encryption(String sms, String name) throws Exception {
        byte[] rnd = new byte[4];
        SecureRandom.getInstanceStrong().nextBytes(rnd);
        byte[] part = "uPidBkle+PUr".getBytes();
        byte[] outKeySold = new byte[16];
        System.arraycopy(part, 0, outKeySold, 0, part.length);
        System.arraycopy(rnd, 0, outKeySold, part.length, rnd.length);
        IvParameterSpec ivSpec = new IvParameterSpec(outKeySold);
        SecretKeySpec spy = new SecretKeySpec(name.getBytes(), "ECB");
        String transformation = "AES/CBC/PKCS5Padding";
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, spy, ivSpec);
        byte[] encode = cipher.doFinal(sms.getBytes());
        encryptedBytes = new byte[encode.length + rnd.length];
        System.arraycopy(encode, 0, encryptedBytes, 0, 1);
        System.arraycopy(rnd, 0, encryptedBytes, 1, 1);
        System.arraycopy(encode, 1, encryptedBytes, 2, 1);
        System.arraycopy(rnd, 1, encryptedBytes, 3, 1);
        System.arraycopy(encode, 2, encryptedBytes, 4, 1);
        System.arraycopy(rnd, 2, encryptedBytes, 5, 1);
        System.arraycopy(encode, 3, encryptedBytes, 6, 1);
        System.arraycopy(rnd, 3, encryptedBytes, 7, 1);
        System.arraycopy(encode, 4, encryptedBytes, 8, 12);
    }
}
