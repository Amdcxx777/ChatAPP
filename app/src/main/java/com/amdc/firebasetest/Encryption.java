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
    public Encryption(String sms) throws Exception {
//        SecureRandom random = SecureRandom.getInstanceStrong();
        byte[] rnd = new byte[4];
        SecureRandom.getInstanceStrong().nextBytes(rnd);
        byte[] partKey = "uPidBkle+PUr".getBytes();
        byte[] outKeySold = new byte[16];
        System.arraycopy(partKey, 0, outKeySold, 0, partKey.length);
        System.arraycopy(rnd, 0, outKeySold, partKey.length, rnd.length);
        IvParameterSpec ivSpec = new IvParameterSpec(outKeySold);
        SecretKeySpec key = new SecretKeySpec("5afzRx0owl7oDDE6".getBytes(), "ECB");
        String transformation = "AES/CBC/PKCS5Padding";
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
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




        //decode
//        byte[] decText = new byte[16];
//        System.arraycopy(encryptedBytes, 0, decText, 0, 1);
//        System.arraycopy(encryptedBytes, 2, decText, 1, 1);
//        System.arraycopy(encryptedBytes, 4, decText, 2, 1);
//        System.arraycopy(encryptedBytes, 6, decText, 3, 1);
//        System.arraycopy(encryptedBytes, 8, decText, 4, 12);
////        encryptedBytes = decText;
//
//        byte[] decSol = new byte[4];
//        System.arraycopy(encryptedBytes, 1, decSol, 0, 1);
//        System.arraycopy(encryptedBytes, 3, decSol, 1, 1);
//        System.arraycopy(encryptedBytes, 5, decSol, 2, 1);
//        System.arraycopy(encryptedBytes, 7, decSol, 3, 1);
//        encryptedBytes = decSol;


//        byte[] partKey1 = "uPidBkle+PUr".getBytes();
//        byte[] decKeySold = new byte[16];
//        System.arraycopy(partKey1, 0, decKeySold, 0, partKey1.length);
//        System.arraycopy(decSol, 0, decKeySold, partKey1.length, decSol.length);
//        IvParameterSpec ivSpec1 = new IvParameterSpec(decKeySold);
//        SecretKeySpec key1 = new SecretKeySpec("5afzRx0owl7oDDE6".getBytes(), "ECB");
//
//        String transformation1 = "AES/CBC/PKCS5Padding";
//        Cipher decryptCipher = Cipher.getInstance(transformation1);
//        decryptCipher.init(Cipher.DECRYPT_MODE, key1, ivSpec1);


//
//        try {
//            decryptedBytes = decryptCipher.doFinal(decText);
//
////            decryptedSMS = new String(decryptedBytes, StandardCharsets.UTF_8);
//        }
//        catch (Exception e) {
////            decryptedSMS = sms;
//        }
//        encryptedBytes = decryptedBytes;
//        Cipher encryptCipher = Cipher.getInstance("AES");
//        SecretKeySpec key = new SecretKeySpec("xx777xx777xx777x".getBytes(), "ECB");
//        encryptCipher.init(Cipher.ENCRYPT_MODE, key);
//        encryptedBytes = encryptCipher.doFinal(sms.getBytes());
    }
}
