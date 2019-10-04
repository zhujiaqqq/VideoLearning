package com.example.audiolearning.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author jiazhu
 */
public class AudioRecorder {
    private static AudioRecorder sRecorder;
    private int mState;

    private AudioRecorder() {
    }

    public static AudioRecorder getInstance() {
        if (sRecorder == null) {
            sRecorder = new AudioRecorder();
        }
        return sRecorder;
    }

    private AudioRecord mAudioRecord;
    private int mMinBufferSize = 0;

    public void createAudioRecord() {
        mMinBufferSize = AudioRecord.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                mMinBufferSize
        );
    }

    public int startRecord() {
        mState = mAudioRecord.getRecordingState();
        if (mState == AudioRecord.RECORDSTATE_STOPPED) {
            mAudioRecord.startRecording();
            mState = mAudioRecord.getRecordingState();

            new Thread(() -> saveToFile()).start();
        }
        return mAudioRecord.getRecordingState();
    }

    private void saveToFile() {
        byte[] buffer = new byte[mMinBufferSize];
        String pathname = Environment.getExternalStorageDirectory().getPath() + "/a.pcm";
        File file = new File(pathname);
        FileOutputStream fos = null;
        if (file.exists()) {
            file.delete();
        }
        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (mState == AudioRecord.RECORDSTATE_RECORDING) {
            int read = mAudioRecord.read(buffer, 0, mMinBufferSize);
            if (read == AudioRecord.ERROR_INVALID_OPERATION ||
                    read == AudioRecord.ERROR_BAD_VALUE ||
                    read == AudioRecord.ERROR_DEAD_OBJECT) {
                break;
            }
            if (fos != null) {
                try {
                    fos.write(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int stopRecord() {
        mState = mAudioRecord.getRecordingState();
        if (mState == AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.stop();
            mState = mAudioRecord.getRecordingState();
            if (mAudioRecord != null) {
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
        return mState;
    }
}
