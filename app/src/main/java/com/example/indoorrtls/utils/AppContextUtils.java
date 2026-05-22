package com.example.indoorrtls.utils;

import android.content.Context;

public class AppContextUtils {
    private static Context context;

    public static void init(Context context) {
        AppContextUtils.context = context.getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }
}
