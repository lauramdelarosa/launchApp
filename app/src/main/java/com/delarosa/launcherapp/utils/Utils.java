package com.delarosa.launcherapp.utils;

import android.util.Base64;

import java.security.MessageDigest;

public class Utils {

    public static String convertToBase64(byte[] data) {
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    /**
     * encripta una cadena de texto
     * @param input
     * @return
     */
    public static String sha256(String input) {
        String hash = "";
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(input.getBytes("UTF-8"));
            byte[] data = digest.digest();
            hash = convertToBase64(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return hash;
    }

}
