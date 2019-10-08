package com.example.videorecorder;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.publiclib.util.LocalHandler;
import com.example.publiclib.util.SingleThreadPooler;
import com.example.publiclib.view.MySurfaceView;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author jiazhu
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, MediaRecorder.OnErrorListener, LocalHandler.IHandler {
    public static final int MSG_REFRESH_TIME = 1;
    public static final int MSG_FINISH_RECORD = 2;
    public static final int MAX_TIME = 600;
    ImageView mIvRecord;
    MySurfaceView mSurfaceView;
    TextView mTvRecordTime;
    ProgressBar mProgressBar;
    ImageView mIvClose;
    ImageView mIvCancel;
    ImageView mIvConfirm;
    ImageView mIvCameraToggle;

    Camera mCamera;
    private SurfaceHolder mHolder;
    private OnRecordListener mOnRecordListener;
    private File mRecordFile;
    private MediaRecorder mMediaRecorder;
    private boolean isRecord;

    private LocalHandler mHandler = new LocalHandler(this);
    private int mFrontIndex;
    private int mBackIndex;

    private boolean isFront;
    private int mFrontOrientation;
    private int mBackOrientation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
    }

    private void initView() {
        mIvRecord = findViewById(R.id.iv_record);
        mSurfaceView = findViewById(R.id.surface_view);
        mTvRecordTime = findViewById(R.id.tv_record_time);
        mProgressBar = findViewById(R.id.pb_progress);
        mIvClose = findViewById(R.id.iv_close);
        mIvCancel = findViewById(R.id.iv_cancel);
        mIvConfirm = findViewById(R.id.iv_confirm);
        mIvCameraToggle = findViewById(R.id.iv_camera_toggle);

        mIvRecord.setSelected(false);

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mProgressBar.setVisibility(View.GONE);
        mTvRecordTime.setVisibility(View.GONE);

        mIvCameraToggle.setOnClickListener(v -> {
            isFront = !isFront;
            initCamera();
        });

        mIvRecord.setOnClickListener(v -> {
            if (isRecord) {
                btnToStopRecord();
            } else {
                btnToStartRecord();
            }
        });

        mIvClose.setOnClickListener(v -> {
            freeCameraResource();
            finish();
        });
    }

    private void initData() {
        getCameraInfo();
//        initCamera();
    }

    private void btnToStartRecord() {
        mProgressBar.setVisibility(View.VISIBLE);
        mTvRecordTime.setVisibility(View.VISIBLE);
        mIvClose.setVisibility(View.GONE);
        mIvCameraToggle.setVisibility(View.GONE);

        mIvCancel.setVisibility(View.GONE);
        mIvConfirm.setVisibility(View.GONE);
        isRecord = true;
        mIvRecord.setSelected(true);
        record(new OnRecordListener() {
            @Override
            public void onRecordFinish() {
                Toast.makeText(MainActivity.this, "finish", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRecordProgress(int progress) {
                Message message = mHandler.obtainMessage();
                message.what = MSG_REFRESH_TIME;
                message.arg1 = progress;
                mHandler.sendMessage(message);
            }
        });
    }

    private void btnToStopRecord() {
        mProgressBar.setVisibility(View.GONE);
        mTvRecordTime.setVisibility(View.GONE);
        mIvClose.setVisibility(View.VISIBLE);
        mIvCameraToggle.setVisibility(View.VISIBLE);
        mIvCancel.setVisibility(View.VISIBLE);
        mIvConfirm.setVisibility(View.VISIBLE);
        isRecord = false;
        mIvRecord.setSelected(false);
        stop();
    }

    private void initCamera() {
        if (mCamera != null) {
            freeCameraResource();
        }
        try {
            mCamera = Camera.open(isFront ? mFrontIndex : mBackIndex);
            if (mCamera == null) {
                return;
            }
            initParameters();
            mCamera.setDisplayOrientation(mBackOrientation);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            mCamera.unlock();
        } catch (Exception e) {
            e.printStackTrace();
            freeCameraResource();
        }
    }

    /**
     * 释放camera资源
     */
    private void freeCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 初始化camera参数
     */
    private void initParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        parameters.setPreviewSize(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight);
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains("continuous-video")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        mCamera.setParameters(parameters);
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
            mCamera.setDisplayOrientation(mBackOrientation);
            mCamera.startPreview();
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        freeCameraResource();
    }

    /**
     * 开始录像
     *
     * @param onRecordListener 录像监听
     */
    public void record(OnRecordListener onRecordListener) {
        mOnRecordListener = onRecordListener;
        createRecordDir();
        try {
            initRecord(isFront ? mFrontOrientation : mBackOrientation);
            SingleThreadPooler.getInstance().doTast(() -> {
                int count = 0;
                while (isRecord) {
                    if (count == MAX_TIME) {
                        mHandler.sendEmptyMessage(MSG_FINISH_RECORD);
                        break;
                    } else {
                        mOnRecordListener.onRecordProgress(count++);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止录像、释放录像资源
     */
    public void stop() {
        stopRecord();
        releaseRecord();
//        freeCameraResource();
        mOnRecordListener.onRecordFinish();
        mCamera.lock();
    }

    /**
     * 释放录像资源
     */
    private void releaseRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            try {
                mMediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mMediaRecorder = null;
        }
    }

    /**
     * 停止录像
     */
    private void stopRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始化录像
     *
     * @param orientationHintDegree
     */
    private void initRecord(int orientationHintDegree) {
        try {
            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
                mMediaRecorder.setOnErrorListener(this);
            } else {
                mMediaRecorder.reset();
            }

            mMediaRecorder.setCamera(mCamera);
            mMediaRecorder.setPreviewDisplay(mHolder.getSurface());

            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
            mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);

            mMediaRecorder.setAudioEncodingBitRate(44100);

            if (profile.videoBitRate > 2 * 1024 * 1024) {
                mMediaRecorder.setVideoEncodingBitRate(281024 * 1024);
            } else {
                mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            }

            mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);

            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            mMediaRecorder.setOrientationHint(orientationHintDegree);

            mMediaRecorder.setOutputFile(mRecordFile.getAbsolutePath());

            mMediaRecorder.prepare();

            mMediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建视频文件
     */
    private void createRecordDir() {
        File sampleDir = new File(Environment.getExternalStorageDirectory().getPath() + "/temp");
        if (!sampleDir.exists()) {
            sampleDir.mkdirs();
        }

        try {
            mRecordFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".mp4", sampleDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null) {
                mr.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handlerMessage(Message msg) {
        if (msg.what == MSG_REFRESH_TIME) {
            mTvRecordTime.setText((countRecordTime(msg.arg1)));
            mProgressBar.setProgress(msg.arg1 / 6);
        } else if (msg.what == MSG_FINISH_RECORD) {
            btnToStopRecord();
        }
    }

    private String countRecordTime(int time) {
        int second = time / 10;
        int lSecond = time % 10;
        return getString(R.string.tv_show_time, second, lSecond);
    }

    /**
     * 获取camera信息
     */
    private void getCameraInfo() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameras = Camera.getNumberOfCameras();

        for (int i = 0; i < cameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFrontIndex = i;
                mFrontOrientation = cameraInfo.orientation;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mBackIndex = i;
                mBackOrientation = cameraInfo.orientation;
            }
        }
    }
}
