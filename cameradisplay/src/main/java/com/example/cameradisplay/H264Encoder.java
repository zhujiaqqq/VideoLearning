package com.example.cameradisplay;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author jiazhu
 */
public class H264Encoder {

    private final static int TIMEOUT_USEC = 12000;
    private MediaCodec mMediaCodec;
    private boolean isRunning = false;
    private int width;
    private int height;
    private int frameRate;

    public byte[] configByte;
    private BufferedOutputStream ous;

    private ArrayBlockingQueue<byte[]> yuv420Queue = new ArrayBlockingQueue<>(10);


    public H264Encoder(int width, int height, int frameRate) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
            createFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void createFile() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/out/test.mp4";
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            ous = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void putData(byte[] buffer) {
        if (yuv420Queue.size() >= 10) {
            yuv420Queue.poll();
        }
        yuv420Queue.add(buffer);
    }

    public void startEncoder() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                isRunning = true;
                byte[] input = null;
                long pts = 0;
                long generateIndex = 0;

                while (isRunning) {
                    if (yuv420Queue.size() > 0) {
                        input = yuv420Queue.poll();
//                        byte[] yuv420sp = new byte[width * height * 3 / 2];
                    }
                    if (input != null) {
                        try {
                            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                            if (inputBufferIndex >= 0) {
                                pts = computePresentationTime(generateIndex);
                                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                                inputBuffer.clear();
                                inputBuffer.put(input);
                                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, System.currentTimeMillis(), 0);
                                generateIndex += 1;
                            }

                            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

                            while (outputBufferIndex >= 0) {
                                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                                byte[] outData = new byte[bufferInfo.size];
                                outputBuffer.get(outData);
                                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                                    configByte = new byte[bufferInfo.size];
                                    configByte = outData;
                                } else if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_SYNC_FRAME) {
                                    byte[] keyFrame = new byte[bufferInfo.size + configByte.length];
                                    System.arraycopy(configByte, 0, keyFrame, 0, configByte.length);
                                    System.arraycopy(outData, 0, keyFrame, configByte.length, outData.length);
                                } else {
                                    ous.write(outData, 0, outData.length);
                                }

                                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                            }
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    } else {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    mMediaCodec.stop();
                    mMediaCodec.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    ous.flush();
                    ous.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopEncoder() {
        isRunning = false;
    }

    private long computePresentationTime(long generateIndex) {
        return 132 + generateIndex * 1000000 / frameRate;
    }
}
