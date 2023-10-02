package com.tokbox.sample.videotransformers;

import android.Manifest;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.banuba.sample.tokbox.R;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;
import com.opentok.android.Stream;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String EFFECT_1 = "effects/CubemapEverest";
    private static final String EFFECT_2 = "effects/DebugFRX";

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.INTERNET,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

    private Session mOtSession;
    private Publisher mOtPublisher;

    private FrameLayout mPublisherViewContainer;

    private BanubaVideoTransformer mBanubaVideoTransformer;

    private final ArrayList<PublisherKit.VideoTransformer> mVideoTransformers = new ArrayList<>();

    private final PublisherKit.PublisherListener mOtPublisherListener = new PublisherKit.PublisherListener() {
        @Override
        public void onStreamCreated(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamCreated: Publisher Stream Created. Own stream " + stream.getStreamId());
        }

        @Override
        public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {
            Log.d(TAG, "onStreamDestroyed: Publisher Stream Destroyed. Own stream " + stream.getStreamId());
        }

        @Override
        public void onError(PublisherKit publisherKit, OpentokError opentokError) {
            finishWithMessage("PublisherKit onError: " + opentokError.getMessage());
        }
    };

    private final Session.SessionListener mOtSessionListener = new Session.SessionListener() {
        @Override
        public void onConnected(Session session) {
            Log.d(TAG, "onConnected: Connected to session: " + session.getSessionId());

            mOtPublisher = new Publisher.Builder(MainActivity.this).build();
            mOtPublisher.setPublisherListener(mOtPublisherListener);
            mOtPublisher.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL);

            mPublisherViewContainer.addView(mOtPublisher.getView());

            if (mOtPublisher.getView() instanceof GLSurfaceView) {
                ((GLSurfaceView) mOtPublisher.getView()).setZOrderOnTop(true);
            }

            session.publish(mOtPublisher);
        }

        @Override
        public void onDisconnected(Session session) {
            Log.d(TAG, "onDisconnected: Disconnected from session: " + session.getSessionId());
        }

        @Override
        public void onStreamReceived(Session session, Stream stream) {
            Log.d(TAG, "onStreamReceived: New Stream Received " + stream.getStreamId() + " in session: " + session.getSessionId());
        }

        @Override
        public void onStreamDropped(Session session, Stream stream) {
            Log.d(TAG, "onStreamDropped: Stream Dropped: " + stream.getStreamId() + " in session: " + session.getSessionId());
        }

        @Override
        public void onError(Session session, OpentokError opentokError) {
            finishWithMessage("Session error: " + opentokError.getMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPublisherViewContainer = findViewById(R.id.publisher_container);

        findViewById(R.id.applyEffect1).setOnClickListener(v -> createAndApplyEffect(EFFECT_1));
        findViewById(R.id.applyEffect2).setOnClickListener(v -> createAndApplyEffect(EFFECT_2));

        findViewById(R.id.noEffect).setOnClickListener(v -> {
            if (mBanubaVideoTransformer != null) {
                mBanubaVideoTransformer.discardEffect();
            }
            mBanubaVideoTransformer = null;

            applyVideoTransformation();
        });
    }

    private void createAndApplyEffect(final String effectName) {
        if (mBanubaVideoTransformer == null) {
            mBanubaVideoTransformer = new BanubaVideoTransformer(getApplicationContext(),
                    true, Config.BANUBA_TOKEN);
        }

        applyVideoTransformation();

        mBanubaVideoTransformer.applyEffect(effectName);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mOtSession != null) {
            mOtSession.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mOtSession != null) {
            mOtSession.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ": " + perms);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        finishWithMessage("onPermissionsDenied: " + requestCode + ": " + perms);
    }

    @AfterPermissionGranted(PERMISSIONS_REQUEST_CODE)
    private void requestPermissions() {
        if (EasyPermissions.hasPermissions(this, PERMISSIONS)) {
            if (!Config.isValid()) {
                finishWithMessage("Invalid OpenTokConfig. " + Config.getDescription());
                return;
            }

            initializeOpentok();
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.rationale_video_app), PERMISSIONS_REQUEST_CODE, PERMISSIONS);
        }
    }

    private void initializeOpentok() {
        if (!Config.isValid()) {
            finishWithMessage("Please specify OpenTok config values");
            return;
        }

        mOtSession = new Session.Builder(this,
                Config.OPENTOK_API_KEY,
                Config.OPENTOK_SESSION_ID).build();
        mOtSession.setSessionListener(mOtSessionListener);
        mOtSession.connect(Config.OPENTOK_TOKEN);
    }

    private void disconnect() {
        Log.d(TAG, "Disconnect");

        if (mPublisherViewContainer != null && mPublisherViewContainer.getChildCount() > 0) {
            mPublisherViewContainer.removeAllViews();
        }

        if (mOtSession != null) {
            mOtSession.unpublish(mOtPublisher);
            mOtSession.disconnect();
        }

        mOtSession = null;
        mOtPublisher = null;
    }

    private void finishWithMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        this.finish();
    }

    private void applyVideoTransformation() {
        if (mOtPublisher == null) {
            return;
        }

        mVideoTransformers.clear();

        if (mBanubaVideoTransformer != null) {
            mVideoTransformers.add(
                    mOtPublisher.new VideoTransformer(
                            "banubaTransformer", mBanubaVideoTransformer)
            );
        }
        mOtPublisher.setVideoTransformers(mVideoTransformers);
    }
}
