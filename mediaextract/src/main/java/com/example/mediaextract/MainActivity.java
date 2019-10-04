package com.example.mediaextract;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Button mBtnStart;

    private ThreadPoolExecutor singleThreadPool;
    private int mVideoTrack;
    private int mFramerate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData(
        );

    }

    private void initData() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("runnable_pool_%d").build();
        singleThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    }

    private void initView() {
        mBtnStart = findViewById(R.id.btn_start);
        mBtnStart.setOnClickListener(v -> {
            singleThreadPool.execute(this::getVideo);
        });
    }

    private void getVideo() {
        File outFile = new File(Environment.getExternalStorageDirectory().getPath() + "/out.mp4");
        if (outFile.exists()) {
            outFile.delete();
        }
        MediaMuxer mMediaMuxer = null;
        try {
             mMediaMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getPath() + "/out.mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            FileOutputStream fos = new FileOutputStream(outFile);
            MediaExtractor extractor = new MediaExtractor();
            String path = Environment.getExternalStorageDirectory().getPath() + "/test.mp4";
            extractor.setDataSource(path);
            int trackCount = extractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = extractor.getTrackFormat(i);

                String string = trackFormat.getString(MediaFormat.KEY_MIME);
                Log.i(TAG, "getVideo: " + string);
                if (string.startsWith("video")) {
                    mMediaMuxer.addTrack(trackFormat);
                    extractor.selectTrack(i);
                    mVideoTrack = i;
                    break;
                }
            }
            mMediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            info.presentationTimeUs = 0;
            while (true) {
                int readSampleData = extractor.readSampleData(byteBuffer, 0);
                Log.i(TAG, "getVideo: readSampleData size: " + readSampleData);
                if (readSampleData <= 0) {
                    break;
                }
                info.offset = 0;
                info.size = readSampleData;
                info.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
//                info.presentationTimeUs = extractor.getSampleTime();
                mMediaMuxer.writeSampleData(mVideoTrack, byteBuffer, info);
                extractor.advance();
            }
            extractor.release();
            mMediaMuxer.stop();
            mMediaMuxer.release();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

//     mediaExtractor.selectTrack(audioTrackIndex);
//            while (true) {
//        int readSampleCount = mediaExtractor.readSampleData(byteBuffer, 0);
//        Log.d(TAG, "audio:readSampleCount:" + readSampleCount);
//        if (readSampleCount < 0) {
//            break;
//        }
//        //保存音频信息
//        byte[] buffer = new byte[readSampleCount];
//        byteBuffer.get(buffer);
//        /************************* 用来为aac添加adts头**************************/
//        byte[] aacaudiobuffer = new byte[readSampleCount + 7];
//        addADTStoPacket(aacaudiobuffer, readSampleCount + 7);
//        System.arraycopy(buffer, 0, aacaudiobuffer, 7, readSampleCount);
//        audioOutputStream.write(aacaudiobuffer);
//        /***************************************close**************************/
//        //  audioOutputStream.write(buffer);
//        byteBuffer.clear();
//        mediaExtractor.advance();
//    }


    /**
     * 这里之前遇到一个坑，以为这个packetLen是adts头的长度，也就是7，仔细看了下代码，发现这个不是adts头的长度，而是一帧音频的长度
     *
     * @param packet    一帧数据（包含adts头长度）
     * @param packetLen 一帧数据（包含adts头）的长度
     */
    private static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = getFreqIdx(44100);
        int chanCfg = 2; // CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    private static int getFreqIdx(int sampleRate) {
        int freqIdx;

        switch (sampleRate) {
            case 96000:
                freqIdx = 0;
                break;
            case 88200:
                freqIdx = 1;
                break;
            case 64000:
                freqIdx = 2;
                break;
            case 48000:
                freqIdx = 3;
                break;
            case 44100:
                freqIdx = 4;
                break;
            case 32000:
                freqIdx = 5;
                break;
            case 24000:
                freqIdx = 6;
                break;
            case 22050:
                freqIdx = 7;
                break;
            case 16000:
                freqIdx = 8;
                break;
            case 12000:
                freqIdx = 9;
                break;
            case 11025:
                freqIdx = 10;
                break;
            case 8000:
                freqIdx = 11;
                break;
            case 7350:
                freqIdx = 12;
                break;
            default:
                freqIdx = 8;
                break;
        }

        return freqIdx;
    }

}
