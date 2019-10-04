package com.example.ffmpegplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

/**
 * @author jiazhu
 */
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    static {
        System.loadLibrary("native-lib");
    }

    MySurfaceView mSurfaceView;
    private SurfaceHolder mHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceView.setAspect((double)1280/720);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    public native String _test();

    public native int _play(Object surface);

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                _play(mHolder.getSurface());
            }
        }).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private int refreshSize( int width,  int height) {
        final int innerWidth = width;
        final int innerHeight = height;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSurfaceView.setAspect((double) (innerWidth / innerHeight));
            }
        });
        return 0;
    }
}
