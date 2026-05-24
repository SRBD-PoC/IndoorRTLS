package com.example.indoorrtls;

import android.app.Application;

import com.example.indoorrtls.utils.AppContextUtils;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContextUtils.init(this);
    }
}
