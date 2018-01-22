package com.example.saniou.videosocket;

import android.graphics.SurfaceTexture;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.saniou.videosocket.model.CameraInfo;
import com.zrhx.base.BaseAppUtils;
import com.zrhx.base.BaseConfig;
import com.zrhx.base.base.BaseActivity;
import com.zrhx.base.utils.LogUtils;
import com.zrhx.base.utils.ScreenUtils;
import com.zrhx.base.utils.ToastUtils;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParserException;

import static com.example.saniou.videosocket.MokeUtil.createCamera;

public class VideoSocketActivity3 extends BaseActivity {

    private TextureView mSurfaceView;
    private DataProcess3 mDataProcess3;
    private CameraInfo mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        printCodec();
        BaseAppUtils.init(new BaseConfig(getApplication(), "", "", "", "", "", "VideoSocket"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSurfaceView = findViewById(R.id.video);
        mDataProcess3 = new DataProcess3();
        mDataProcess3.setCallBack(new DataProcess3.DataProcessCallBack() {
            @Override
            public void onSizeDefine(int w, int h) {
                mSurfaceView.post(() -> {
                    int width = ScreenUtils.getScreenWidth();
                    ViewGroup.LayoutParams params = mSurfaceView.getLayoutParams();
                    params.width = width;
                    params.height = (int) (((float) h) / w * width);
                    mSurfaceView.requestLayout();
                });
            }

            @Override
            public void onByteBuff(byte[] w, int h) {

            }
        });
        mSurfaceView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                LogUtils.e("surfaceCreated()");
                mDataProcess3.setSurface(new Surface(surface));
                if (mCamera != null) {
                    playVideo(mCamera);
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                LogUtils.e(" onSurfaceTextureSizeChanged Window size:" + width + "x" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                LogUtils.e("surfaceDestroyed()");
                mDataProcess3.release();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    private void startPlay(CameraInfo camera) throws IOException, XmlPullParserException {
        mDataProcess3.initLiveVideo(camera);
    }

    public void startVideo(View view) {
        String string = ((TextView) view).getText().toString();
        if (mCamera != null) {
            mDataProcess3.release();
            mCamera = null;
        }
        mCamera = createCamera(string);
        if (mCamera != null) {
            playVideo(mCamera);
        }
    }

    private void playVideo(CameraInfo camera) {
        Schedulers.io().scheduleDirect(() -> {
            try {
                startPlay(camera);
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof IOException) {
                    ToastUtils.showShort("连接视频服务器失败");
                } else if (e instanceof XmlPullParserException) {
                    ToastUtils.showShort("数据解析错误");
                } else {
                    ToastUtils.showShort("视频播放失败");
                }
            }
        });
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
