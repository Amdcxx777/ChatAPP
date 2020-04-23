package com.amdc.firebasetest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.widget.Toast;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Decryption extends ChatActivity{
    static byte[] decryptedBytes;

    public Decryption(String sms) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        @SuppressLint("GetInstance")
        Cipher decryptCipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec("xx777xx777xx777x".getBytes(), "AES");
        decryptCipher.init(Cipher.DECRYPT_MODE, key);
        String[] msg = sms.substring(1, sms.length() - 1).split(", ");
        byte[] msgByte = new byte[msg.length];

        for (int i = 0; i < msg.length; i++) {
            try { msgByte[i] = Byte.parseByte(msg[i]);
            } catch (Exception ignored) {}
        }
        try { decryptedBytes = decryptCipher.doFinal(msgByte);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Key not valid", Toast.LENGTH_SHORT).show();
        }
    }
}
