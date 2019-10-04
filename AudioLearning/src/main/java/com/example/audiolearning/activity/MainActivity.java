package com.example.audiolearning.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.audiolearning.audio.AudioRecorder;
import com.example.audiolearning.audio.AudioTracker;
import com.example.audiolearning.R;

/**
 * @author jiazhu
 */
public class MainActivity extends AppCompatActivity {
    private TextView mtvStatus;
    private Button mBtnStart;
    private Button mBtnStop;
    private Button mBtnPlay;

    private int mStatus = AudioRecord.RECORDSTATE_STOPPED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mtvStatus = findViewById(R.id.tv_status);
        mBtnStart = findViewById(R.id.btn_start);
        mBtnStop = findViewById(R.id.btn_stop);
        mBtnPlay = findViewById(R.id.btn_play);

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStatus == AudioRecord.RECORDSTATE_STOPPED) {
                    AudioRecorder.getInstance().createAudioRecord();
                    mStatus = AudioRecorder.getInstance().startRecord();
                    showStatus(mStatus, mtvStatus);
                }
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStatus == AudioRecord.RECORDSTATE_RECORDING) {
                    mStatus = AudioRecorder.getInstance().stopRecord();
                    showStatus(mStatus, mtvStatus);
                }
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioTracker.getInstance().createTrack();
                AudioTracker.getInstance().setFileName(Environment.getExternalStorageDirectory().getPath() + "/a.pcm");
                AudioTracker.getInstance().play();
            }
        });
    }

    private void showStatus(int status, TextView textView) {
        textView.setText(status == AudioRecord.RECORDSTATE_STOPPED ? "stop" : "recording");
    }
}
