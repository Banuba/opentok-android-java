package com.tokbox.sample.videotransformers;

import android.text.TextUtils;

import androidx.annotation.NonNull;

public class Config {
    // Set Banuba Face AR token
    public static final String BANUBA_TOKEN = SET VALUE

    // Replace with a OpenTok API key
    public static final String OPENTOK_API_KEY = SET VALUE

    // Replace with a generated OpenTok Session ID
    public static final String OPENTOK_SESSION_ID = SET VALUE

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String OPENTOK_TOKEN = SET VALUE

    // *** The code below is to validate this configuration file. You do not need to modify it  ***

    public static boolean isValid() {
        if (TextUtils.isEmpty(Config.OPENTOK_API_KEY)
                || TextUtils.isEmpty(Config.OPENTOK_SESSION_ID)
                || TextUtils.isEmpty(Config.OPENTOK_TOKEN)) {
            return false;
        }

        return true;
    }

    @NonNull
    public static String getDescription() {
        return "OpenTokConfig:" + "\n"
                + "API_KEY: " + Config.OPENTOK_API_KEY + "\n"
                + "SESSION_ID: " + Config.OPENTOK_SESSION_ID + "\n"
                + "TOKEN: " + Config.OPENTOK_TOKEN + "\n";
    }
}
