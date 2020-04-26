package com.amdc.firebasetest;

import android.annotation.SuppressLint;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressLint("Registered")
public class Decryption extends ChatActivity{
    static String decryptedSMS;
    public Decryption(String sms) throws Exception {
        @SuppressLint("GetInstance")
        String[] msg = sms.substring(1, sms.length() - 1).split(", ");
        byte[] msgByte = new byte[msg.length];
        for (int i = 0; i < msg.length; i++) {
            try { msgByte[i] = Byte.parseByte(msg[i]);
            } catch (Exception ignored) {}
        }
        byte[] decText = new byte[16];
        System.arraycopy(msgByte, 0, decText, 0, 1);
        System.arraycopy(msgByte, 2, decText, 1, 1);
        System.arraycopy(msgByte, 4, decText, 2, 1);
        System.arraycopy(msgByte, 6, decText, 3, 1);
        System.arraycopy(msgByte, 8, decText, 4, 12);

        byte[] decSol = new byte[4];
        System.arraycopy(msgByte, 1, decSol, 0, 1);
        System.arraycopy(msgByte, 3, decSol, 1, 1);
        System.arraycopy(msgByte, 5, decSol, 2, 1);
        System.arraycopy(msgByte, 7, decSol, 3, 1);

        byte[] part = "uPidBkle+PUr".getBytes();
        byte[] outKeySold = new byte[16];
        System.arraycopy(part, 0, outKeySold, 0, part.length);
        System.arraycopy(decSol, 0, outKeySold, part.length, decSol.length);
        IvParameterSpec ivSpec = new IvParameterSpec(outKeySold);

        String transformation = "AES/CBC/PKCS5Padding";
        SecretKeySpec key = new SecretKeySpec("5afzRx0owl7oDDE6".getBytes(), "ECB");
        Cipher decryptCipher = Cipher.getInstance(transformation);
        decryptCipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        try {
            byte[] decryptedBytes = decryptCipher.doFinal(decText);
            decryptedSMS = new String(decryptedBytes, StandardCharsets.UTF_8);
        }
        catch (Exception e) { decryptedSMS = sms;}
    }
}
