package com.example.mp4player;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.widget.Button;

import java.io.IOException;

/**
 * @author jiazhu
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";
    MySurfaceView surfaceView;
    Button mBtnStart;

    MediaPlayer mMediaPlayer;
    private SurfaceHolder mHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initView() {
        surfaceView = findViewById(R.id.surface_view);
        mBtnStart = findViewById(R.id.btn_start);
    }

    private void initData() {

        mHolder = surfaceView.getHolder();
        mHolder.addCallback(this);
        mBtnStart.setOnClickListener(v -> {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            } else {
                mMediaPlayer.reset();
            }

            try {
                mMediaPlayer.setAudioAttributes(new AudioAttributes.Builder().build());
                mMediaPlayer.setDataSource(Environment.getExternalStorageDirectory().getPath() + "/test.mp4");
                mMediaPlayer.setOnPreparedListener(MediaPlayer::start);

                mMediaPlayer.setOnVideoSizeChangedListener((mp, width, height) ->
                        surfaceView.setAspect((double)width/height));
                mMediaPlayer.setOnCompletionListener(MediaPlayer::release);
                if (mHolder != null) {
                    mMediaPlayer.setDisplay(mHolder);
                }
                mMediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

        super.onDestroy();
    }
}
