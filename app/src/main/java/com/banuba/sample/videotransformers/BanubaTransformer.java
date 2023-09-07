package com.banuba.sample.videotransformers;

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
import com.opentok.android.PublisherKit.CustomVideoTransformer;

import java.nio.ByteBuffer;

public class BanubaTransformer implements CustomVideoTransformer, ImageProcessedListener {

    public BanubaTransformer(Context context) {
        mContext = context;
    }

    public void loadEffect(String path) {
        mEffectPath = path;
        if (mOep != null) {
            mOep.loadEffect(path);
        }
    }

    @Override
    public void onTransform(Frame frame) {
        createBanubaPlayerIfRequired(frame.getWidth(), frame.getHeight());
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
            synchronized (mSinchLock) {
                mSinchLock.wait();
            }
        } catch (InterruptedException ignored) { }
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

        synchronized (mSinchLock) {
            mSinchLock.notifyAll();
        }
    }

    private void createBanubaPlayerIfRequired(int w, int h) {
        if (mOep == null) {
            mOep = new OffscreenEffectPlayer(
                mContext,
                OffscreenEffectPlayerConfig.newBuilder(
                    new Size(w, h),
                    new BuffersQueue()
                ).build(),
                BanubaClientToken.CLIENT_TOKEN
            );
            mOep.loadEffect(mEffectPath);

            mOep.setImageProcessListener(this, new Handler(Looper.getMainLooper()));

            mOep.playbackPlay();
        }
    }

    private OffscreenEffectPlayer mOep;
    private Frame mCurrentFrame;
    private final Object mSinchLock = new Object();

    private final Context mContext;

    private String mEffectPath = "";
}
