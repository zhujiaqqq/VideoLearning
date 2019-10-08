package com.example.videorecorder;

import android.os.Bundle;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.publiclib.util.LocalHandler;
import com.example.publiclib.view.MySurfaceView;
import com.example.videorecorder.record.IRecorderController;
import com.example.videorecorder.record.OnRecordListener;
import com.example.videorecorder.record.RecorderControllerImpl;

import static com.example.videorecorder.record.RecorderControllerImpl.MSG_FINISH_RECORD;
import static com.example.videorecorder.record.RecorderControllerImpl.MSG_REFRESH_TIME;

/**
 * @author jiazhu
 */
public class MainActivity extends AppCompatActivity implements LocalHandler.IHandler {
    ImageView mIvRecord;
    MySurfaceView mSurfaceView;
    TextView mTvRecordTime;
    ProgressBar mProgressBar;
    ImageView mIvClose;
    ImageView mIvCancel;
    ImageView mIvConfirm;
    ImageView mIvCameraToggle;

    private boolean isRecord;

    private LocalHandler mHandler = new LocalHandler(this);

    private IRecorderController mRecorderController;


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

        mProgressBar.setVisibility(View.GONE);
        mTvRecordTime.setVisibility(View.GONE);

        mIvCameraToggle.setOnClickListener(v -> mRecorderController.toggleCamera());

        mIvRecord.setOnClickListener(v -> {
            if (isRecord) {
                btnToStopRecord();
            } else {
                btnToStartRecord();
            }
        });

        mIvClose.setOnClickListener(v -> {
            mRecorderController.freeCameraResource();
            finish();
        });
    }

    private void initData() {

        SurfaceHolder holder = mSurfaceView.getHolder();

        mRecorderController = new RecorderControllerImpl(
                holder, mHandler,
                new OnRecordListener() {
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

                }
        );

        holder.addCallback(mRecorderController);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
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

        mRecorderController.startRecorder();
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

        mRecorderController.stopRecorder();
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
}
