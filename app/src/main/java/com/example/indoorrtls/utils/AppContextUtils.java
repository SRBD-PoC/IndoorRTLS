package com.example.indoorrtls.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

public class AppContextUtils {
    private static Context context;

    public static void init(Context context) {
        AppContextUtils.context = context.getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }

    public static Activity getActivity(View view) {
        Context context = view.getContext();

        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }

        return null;
    }
}
