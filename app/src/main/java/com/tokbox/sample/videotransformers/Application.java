package com.tokbox.sample.videotransformers;

import static java.util.Objects.requireNonNull;

import com.banuba.sdk.manager.BanubaSdkManager;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BanubaSdkManager.initialize(requireNonNull(getApplicationContext()), Config.BANUBA_TOKEN);
    }

    @Override
    public void onTerminate() {
        BanubaSdkManager.deinitialize();
        super.onTerminate();
    }
}