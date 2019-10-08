package com.example.videorecorder.record;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.view.SurfaceHolder;

import com.example.publiclib.util.LocalHandler;
import com.example.publiclib.util.SingleThreadPooler;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author jiazhu
 * <p>
 * 1 获取camera的信息，获取前后摄像头的索引和方向
 * 2 初始化camera
 * 3 录像：创建视频文件，初始化录像参数，开始录制视频
 * 4 关闭录像
 * 5 释放camera资源
 */
public class RecorderControllerImpl implements MediaRecorder.OnErrorListener, IRecorderController {
    private static final int MAX_TIME = 600;
    public static final int MSG_REFRESH_TIME = 1;
    public static final int MSG_FINISH_RECORD = 2;

    private int mFrontIndex;
    private int mBackIndex;

    private int mFrontOrientation;
    private int mBackOrientation;

    private boolean isFront;

    private boolean isRecord;

    private Camera mCamera;

    private SurfaceHolder mHolder;

    private LocalHandler mHandler;

    private OnRecordListener mOnRecordListener;

    private File mRecordFile;

    private MediaRecorder mMediaRecorder;

    public RecorderControllerImpl(SurfaceHolder holder,
                                  LocalHandler handler,
                                  OnRecordListener onRecordListener) {

        mHolder = holder;
        mHandler = handler;
        this.isRecord = isRecord;
        mOnRecordListener = onRecordListener;
    }

    @Override
    public void initCamera() {
        getCameraInfo();
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
    @Override
    public void freeCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void toggleCamera() {
        isFront = !isFront;
        initCamera();
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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
            mCamera.setDisplayOrientation(mBackOrientation);
            mCamera.startPreview();
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        freeCameraResource();
    }

    /**
     * 开始录像
     */
    @Override
    public void startRecorder() {
        isRecord = true;
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
                        if (mOnRecordListener != null) {
                            mOnRecordListener.onRecordProgress(count++);
                        }
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

    /**
     * 停止录像、释放录像资源
     */
    @Override
    public void stopRecorder() {
        isRecord = false;
        stopRecord();
        releaseRecord();
        if (mOnRecordListener != null) {
            mOnRecordListener.onRecordFinish();
        }
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

}
