package com.example.indoorrtls.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

public class DeviceUtils {
    private static final String PREF_NAME = "rtls_prefs";
    private static final String KEY_DEVICE_ID = "device_id";

    public static String getOrCreateDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String idHex = prefs.getString(KEY_DEVICE_ID, null);
        if (idHex == null) {
            byte[] id = new byte[4];
            new Random().nextBytes(id);
            idHex = bytesToHex(id);
            prefs.edit().putString(KEY_DEVICE_ID, idHex).apply();
        }
        return idHex;
    }

    public static byte[] getOrCreateDeviceIdBytes(Context context) {
        return hexToBytes(getOrCreateDeviceId(context));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
