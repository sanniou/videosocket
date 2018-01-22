package com.example.saniou.videosocket;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.zrhx.base.BaseAppUtils;
import com.zrhx.base.BaseConfig;
import com.zrhx.base.base.BaseActivity;
import com.zrhx.base.utils.LogUtils;
import com.zrhx.base.utils.ScreenUtils;
import com.example.saniou.videosocket.model.CameraInfo;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.example.saniou.videosocket.DataProcess.handlePause;
import static com.example.saniou.videosocket.DataProcess.onNativeResize;
import static com.example.saniou.videosocket.DataProcess.onNativeSurfaceChanged;
import static com.example.saniou.videosocket.DataProcess.onNativeSurfaceDestroyed;
import static com.example.saniou.videosocket.MokeUtil.createCamera;

public class VideoSocketActivity extends BaseActivity {


    // Load the .so 加载视频库
    static {
        System.loadLibrary("avutil-54");
        System.loadLibrary("avcodec-56");
        System.loadLibrary("swresample-1");
        System.loadLibrary("avformat-56");
        System.loadLibrary("swscale-3");
        System.loadLibrary("avfilter-5");
        System.loadLibrary("avdevice-56");
        System.loadLibrary("postproc-53");
        System.loadLibrary("SDL2");
        System.loadLibrary("display");
    }

    private TextureView mSurfaceView;
    private TextureView mSurfaceView2;
    private MediaCodec mMediaCodec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BaseAppUtils.init(new BaseConfig(getApplication(), "", "", "", "", "", "VideoSocket"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView2 = findViewById(R.id.video2);
        mSurfaceView = findViewById(R.id.video);
        initSurface1();
    }

    private void initSurface1() {
        DataProcess.setCallBack(new DataProcess3.DataProcessCallBack() {
            @Override
            public void onSizeDefine(int w, int h) {
                mSurfaceView.post(() -> {
                    int width = ScreenUtils.getScreenWidth() / 3 * 2;
                    ViewGroup.LayoutParams params = mSurfaceView.getLayoutParams();
                    params.width = width;
                    params.height = (int) (((float) h) / w * width);
                    mSurfaceView.requestLayout();
                });
                mSurfaceView2.post(() -> {
                    int width = ScreenUtils.getScreenWidth();
                    ViewGroup.LayoutParams params = mSurfaceView2.getLayoutParams();
                    params.width = width;
                    params.height = (int) (((float) h) / w * width);
                    mSurfaceView2.requestLayout();
                });
                // 解码器
                try {
                    mMediaCodec = MediaCodec.createDecoderByType("video/avc");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                MediaFormat mediaFormat = MediaFormat
                        .createVideoFormat("video/avc", w, h);
                mMediaCodec.configure(mediaFormat, new Surface(mSurfaceView2.getSurfaceTexture()),
                        null, 0);
                mMediaCodec.start();
            }

            private int mCount;

            @Override
            public void onByteBuff(byte[] buf, int length) {
                ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(buf, 0, length);
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount, 0);
                    mCount++;
                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            }
        });
        mSurfaceView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                LogUtils.e("surfaceCreated()");
                DataProcess.sSurfaceMap.put(0, new Surface(surface));
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                LogUtils.e("surfaceChanged()");
                int sdlFormat = 0x15151002;
                onNativeResize(width, height, sdlFormat);
                LogUtils.e("Window size:" + width + "x" + height);
                onNativeSurfaceChanged();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                LogUtils.e("surfaceDestroyed()");
                handlePause();
                onNativeSurfaceDestroyed();
                DataProcess.destroy();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    private void startPlay(CameraInfo camera) throws Exception {
        DataProcess.initRealTimePlayVideo(camera);
    }

    public void startVideo(View view) {
        String string = ((TextView) view).getText().toString();
        CameraInfo camera = createCamera(string);
        if (camera == null) {
            return;
        }
        new Thread(() -> {
            try {
                startPlay(camera);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void printCodec() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                Log.e("codecInfo", "codec info:" + codecInfo.getName() + " supportedTypes:" + type +
                        " isEncoder:" + codecInfo.isEncoder());
            }
        }
    }
}
