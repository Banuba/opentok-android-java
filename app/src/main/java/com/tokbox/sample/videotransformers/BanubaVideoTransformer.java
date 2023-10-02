package com.tokbox.sample.videotransformers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;

import androidx.annotation.NonNull;

import com.banuba.sdk.offscreen.ImageProcessResult;
import com.banuba.sdk.offscreen.ImageProcessedListener;
import com.banuba.sdk.offscreen.OffscreenEffectPlayer;
import com.banuba.sdk.offscreen.OffscreenEffectPlayerConfig;
import com.banuba.sdk.types.FullImageData;
import com.opentok.android.BaseVideoRenderer.Frame;

import java.nio.ByteBuffer;

import android.text.TextUtils;
import android.util.Log;

import com.opentok.android.PublisherKit;

public class BanubaVideoTransformer implements PublisherKit.CustomVideoTransformer,
        ImageProcessedListener {

    private static final String TAG = "BanubaVideoTransformer";

    // Delay to skip 5 frames
    private static final int WAIT_DELAY = (1000 / 30) * 5;

    // Optimal resolution for streaming
    private static final Size HIGH_RESOLUTION = new Size(720, 1280);
    private static final Size MEDIUM_RESOLUTION = new Size(480, 854);

    private OffscreenEffectPlayer mOep;
    private Frame mCurrentFrame;
    private final Object mLock = new Object();

    private final Context mContext;
    private final String mBanubaToken;

    private String mEffectPath = "";
    private final Size mResolution;

    private final Handler handler = new Handler(Looper.getMainLooper());

    public BanubaVideoTransformer(
            @NonNull final Context context,
            final boolean highResolution,
            @NonNull final String banubaToken
    ) {
        mContext = context;
        mBanubaToken = banubaToken;

        if (highResolution) {
            mResolution = HIGH_RESOLUTION;
        } else {
            mResolution = MEDIUM_RESOLUTION;
        }
    }


    public void applyEffect(String path) {
        loadEffectInternal(path);

    }

    public void discardEffect() {
        loadEffectInternal("");
    }

    private void loadEffectInternal(String path) {
        mEffectPath = path;
        if (mOep != null) {
            mOep.loadEffect(path);
        }
    }

    @Override
    public void onTransform(Frame frame) {
        prepareEffectPlayer();

        if (mOep == null) {
            Log.w(TAG, "onTransform: not prepared!");
            return;
        }

        mCurrentFrame = frame;
        mOep.processFullImageData(
                new FullImageData(
                        new Size(frame.getWidth(), frame.getHeight()),
                        frame.getYplane(), frame.getUplane(), frame.getVplane(),
                        frame.getYstride(), frame.getUvStride(), frame.getUvStride(),
                        1, 1, 1,
                        new FullImageData.Orientation()
                ),
                null,
                System.nanoTime()
        );

        try {
            synchronized (mLock) {
                mLock.wait(WAIT_DELAY);
            }
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void onImageProcessed(@NonNull ImageProcessResult imageProcessResult) {
        ByteBuffer dstY = mCurrentFrame.getYplane();
        ByteBuffer srcY = imageProcessResult.getPlaneBuffer(0);
        srcY.limit(dstY.remaining());
        dstY.put(srcY);

        ByteBuffer dstU = mCurrentFrame.getUplane();
        ByteBuffer srcU = imageProcessResult.getPlaneBuffer(1);
        final int uStride = mCurrentFrame.getUvStride();

        ByteBuffer dstV = mCurrentFrame.getVplane();
        ByteBuffer srcV = imageProcessResult.getPlaneBuffer(2);
        final int vStride = mCurrentFrame.getUvStride();

        final int lineSize = mCurrentFrame.getWidth() / 2;

        for (int h = 0; h < mCurrentFrame.getHeight() / 2 - 1; h++) {
            dstU.position(h * uStride);
            dstV.position(h * vStride);
            srcU.position(h * uStride * 2);
            srcV.position(h * vStride * 2);
            srcU.limit(srcU.position() + lineSize);
            srcV.limit(srcV.position() + lineSize);
            dstU.put(srcU);
            dstV.put(srcV);
            srcU.limit(srcU.capacity());
            srcV.limit(srcV.capacity());
        }

        synchronized (mLock) {
            mLock.notifyAll();
        }
    }

    private void prepareEffectPlayer() {
        final boolean hasEffect = !TextUtils.isEmpty(mEffectPath);

        if (mOep == null && hasEffect) {
            Log.d(TAG, "prepareEffectPlayer: create and apply");
            mOep = new OffscreenEffectPlayer(
                    mContext,
                    OffscreenEffectPlayerConfig.newBuilder(mResolution, new BuffersQueue()
                    ).build(),
                    mBanubaToken
            );
            mOep.loadEffect(mEffectPath);
            mOep.setImageProcessListener(this, handler);
            mOep.playbackPlay();
            return;
        }

        if (mOep != null && !hasEffect) {
            Log.d(TAG, "prepareEffectPlayer: destroy");
            mOep.playbackPause();
            mOep.playbackStop();
            mOep = null;
            return;
        }
    }
}

