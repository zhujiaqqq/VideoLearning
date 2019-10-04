package com.example.audiolearning.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.text.TextUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author jiazhu
 */
public class AudioTracker {

    private static AudioTracker sTracker;

    private ThreadPoolExecutor singleThreadPool;

    private AudioTracker() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("runnable_pool_%d").build();
        singleThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    public static AudioTracker getInstance() {
        if (sTracker == null) {
            sTracker = new AudioTracker();
        }
        return sTracker;
    }

    private AudioTrack mAudioTrack;
    private int mMinBufferSize;

    private String fileName;

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void createTrack() {
        mMinBufferSize = AudioTrack.getMinBufferSize(
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                mMinBufferSize,
                AudioTrack.MODE_STREAM
        );
    }

    public void play() {
        int playState = mAudioTrack.getPlayState();
        if (playState == AudioTrack.PLAYSTATE_STOPPED &&
                mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            mAudioTrack.play();

            singleThreadPool.execute(this::trackPlay);
        }
    }

    private void trackPlay() {
        byte[] buffer = new byte[mMinBufferSize];

        if (TextUtils.isEmpty(fileName)) {
            return;
        }
        File file = new File(fileName);
        if (file.exists()) {
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            int read = 0;
            while (read >= 0) {

                if (dis != null &&
                        mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        read = dis.read(buffer, 0, mMinBufferSize);
                        mAudioTrack.write(buffer, 0, read);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mAudioTrack != null) {
                if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    mAudioTrack.stop();
                }
                if (mAudioTrack != null) {
                    mAudioTrack.release();
                }
            }
            try {
                dis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
