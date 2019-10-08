package com.example.videorecorder.record;

import android.view.SurfaceHolder;

/**
 * @author jiazhu
 */
public interface IRecorderController extends SurfaceHolder.Callback {
    /**
     * 初始化camera
     */
    void initCamera();

    /**
     * 开始录制
     */
    void startRecorder();

    /**
     * 停止录制
     */
    void stopRecorder();

    /**
     * 释放camera资源
     */
    void freeCameraResource();

    /**
     * 切换前后摄像头
     */
    void toggleCamera();

    @Override
    void surfaceCreated(SurfaceHolder holder);

    @Override
    void surfaceChanged(SurfaceHolder holder, int format, int width, int height);

    @Override
    void surfaceDestroyed(SurfaceHolder holder);
}
