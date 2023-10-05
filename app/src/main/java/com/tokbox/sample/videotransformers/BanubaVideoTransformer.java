package com.tokbox.sample.videotransformers;

import com.banuba.sdk.frame.FramePixelBuffer;
import com.banuba.sdk.frame.FramePixelBufferFormat;
import com.banuba.sdk.input.StreamInput;
import com.banuba.sdk.output.FrameOutput;
import com.banuba.sdk.player.IDirectBufferAllocator;
import com.banuba.sdk.player.Player;
import com.opentok.android.BaseVideoRenderer.Frame;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.opentok.android.PublisherKit;

public class BanubaVideoTransformer implements PublisherKit.CustomVideoTransformer,
        FrameOutput.IFramePixelBufferProvider {

    private static final String TAG = "BanubaVideoTransformer";

    private Player mPlayer;
    private FrameOutput mOutput;
    private StreamInput mInput;
    private final BuffersQueue mBuffersQueue = new BuffersQueue();

    private Frame mCurrentFrame;

    private String mEffectPath = "";

    public BanubaVideoTransformer() {
    }

    public void applyEffect(String path) {
        loadEffectInternal(path);
    }

    public void discardEffect() {
        loadEffectInternal("");
        preparePlayer();
    }

    private void loadEffectInternal(String path) {
        mEffectPath = path;
        if (mPlayer != null) {
            mPlayer.loadAsync(path);
        }
    }

    @Override
    public void onTransform(Frame frame) {
        preparePlayer();

        final boolean hasEffect = !TextUtils.isEmpty(mEffectPath);
        if (mPlayer == null || !hasEffect) {
            Log.w(TAG, "onTransform: not prepared!");
            return;
        }

        mInput.push(
            new FramePixelBuffer(
                frame.getBuffer(),
                new int[] {0, frame.getYplaneSize(), frame.getYplaneSize() + frame.getUVplaneSize()},
                new int[] {frame.getYstride(), frame.getUvStride(), frame.getUvStride()},
                new int[] {1, 1, 1},
                frame.getWidth(),
                frame.getHeight(),
                FramePixelBufferFormat.I420_BT709_FULL),
            System.nanoTime()
        );
        mCurrentFrame = frame;
        mPlayer.render();
    }

    @Override
    public void onFrame(FramePixelBuffer framePixelBuffer) {
        ByteBuffer dstY = mCurrentFrame.getYplane();
        ByteBuffer srcY = framePixelBuffer.getPlane(0);
        srcY.limit(dstY.remaining());
        dstY.put(srcY);

        ByteBuffer dstU = mCurrentFrame.getUplane();
        ByteBuffer srcU = framePixelBuffer.getPlane(1);
        final int uStride = mCurrentFrame.getUvStride();

        ByteBuffer dstV = mCurrentFrame.getVplane();
        ByteBuffer srcV = framePixelBuffer.getPlane(2);
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
        mBuffersQueue.retainBuffer(framePixelBuffer.getBuffer());
    }

    private void preparePlayer() {
        final boolean hasEffect = !TextUtils.isEmpty(mEffectPath);

        if (mPlayer == null && hasEffect) {
            Log.d(TAG, "preparePlayer: create and apply");
            mInput = new StreamInput();
            mOutput = new FrameOutput(this, mBuffersQueue);
            mOutput.setFormat(FramePixelBufferFormat.I420_BT709_FULL);
            mPlayer = new Player();
            mPlayer.setRenderMode(Player.RenderMode.MANUAL);
            mPlayer.use(mInput, mOutput);
            mPlayer.loadAsync(mEffectPath);
            mPlayer.play();
            return;
        }

        if (mPlayer != null && !hasEffect) {
            Log.d(TAG, "preparePlayer: destroy");
            mPlayer.close();
            mPlayer = null;
            mOutput.close();
            mOutput = null;
            mInput = null;
        }
    }

    //
    public static class BuffersQueue implements IDirectBufferAllocator {
        private final Queue<ByteBuffer> mQueue = new LinkedList<>();

        public BuffersQueue() {
        }

        @NonNull
        public synchronized ByteBuffer allocateBuffer(int capacity) {
            final ByteBuffer buffer = mQueue.poll();
            if (buffer != null && buffer.capacity() == capacity) {
                buffer.rewind();
                return buffer;
            }
            return ByteBuffer.allocateDirect(capacity);
        }

        public synchronized void retainBuffer(@NonNull ByteBuffer buffer) {
            if (mQueue.size() < 4) {
                mQueue.add(buffer);
            }
        }
    }
}
