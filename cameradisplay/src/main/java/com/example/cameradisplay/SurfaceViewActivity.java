package com.example.cameradisplay;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import java.io.IOException;

/**
 * @author jiazhu
 */
public class SurfaceViewActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "SurfaceViewActivity";
    SurfaceView mSurfaceView;
    Button mBtnToTextureView;
    Camera mCamera;
    H264Encoder mEncoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onResume() {
        super.onResume();
        supportH264Codec();
        initView();
        initData();
    }

    private void supportH264Codec() {
        if (Build.VERSION.SDK_INT >= 18) {
            for (int i = MediaCodecList.getCodecCount() - 1; i >= 0; i--) {
                MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i);
                String[] types = codecInfoAt.getSupportedTypes();
                for (String type : types) {
                    Log.i(TAG, "supportH264Codec: "+type);
                }
            }
        }
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.surface_view);
        mBtnToTextureView = findViewById(R.id.btn_to_texture_view);
        mBtnToTextureView.setOnClickListener(v -> {
            mCamera.release();

            Intent intent = new Intent(this, TextureViewActivity.class);
            startActivity(intent);
        });

        mSurfaceView.getHolder().addCallback(this);
    }

    private void initData() {
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPreviewSize(1280, 720);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);

        mCamera.setPreviewCallback((data, camera) -> {
            Log.d(TAG, "initData: " + data.length);
            if (mEncoder != null) {
                mEncoder.putData(data);
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mEncoder = new H264Encoder(1280, 720, 30);
        mEncoder.startEncoder();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.startPreview();
            mCamera.release();
        }

        if (mEncoder != null) {
            mEncoder.stopEncoder();
        }
    }
}
