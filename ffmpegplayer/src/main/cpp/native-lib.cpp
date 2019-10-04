#include <string>
#include <jni.h>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}

#define LOG_TAG "ffmpeg_player"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

int refresh_size(JNIEnv *env, jobject instance, int width, int height) {

    jclass clazz = (*env).GetObjectClass(instance);
    if (clazz == 0) {
        LOGD("unable to find class");
        return -1;
    }

    jmethodID javaRefreshSize = env->GetMethodID(clazz, "refreshSize", "(II)I");
    env->CallIntMethod(clazz, javaRefreshSize, width, height);
    return 0;
}

extern "C" JNIEXPORT jstring JNICALL
my_jni_test(JNIEnv *env, jobject instance) {
    return env->NewStringUTF("aaa");
}

extern "C" JNIEXPORT jint JNICALL
android_jni_play(JNIEnv *env, jobject instance, jobject surface) {
    LOGD("play");

    char *file_name = "/storage/sdcard0/test.mp4";

    av_register_all();

    AVFormatContext *pFormatCtx = avformat_alloc_context();

    if (avformat_open_input(&pFormatCtx, file_name, NULL, NULL) != 0) {
        LOGD("couldn't open file: %s\n", file_name);
        return -1;
    }

    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGD("couldn't find stream infomation.");
        return -1;
    }

    int videoStream = -1;
    int i;
    for (i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO
            && videoStream < 0) {
            videoStream = i;
        }
    }

    if (videoStream == -1) {
        LOGD("Can't find a video stream");
        return -1;
    }

    AVCodecContext *pCodecCtx = pFormatCtx->streams[videoStream]->codec;

    AVCodec *pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if (pCodec == NULL) {
        LOGD("Codec not found. ");
        return -1;
    }

    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGD("Can not open codec");
        return -1;
    }

    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);

    int videoWidth = pCodecCtx->width;
    int videoHeight = pCodecCtx->height;

    LOGD("videoWidth: %d\n",videoWidth);
    LOGD("videoHeight: %d\n",videoHeight);
//    refresh_size(env, instance, videoWidth, videoHeight);

    ANativeWindow_setBuffersGeometry(nativeWindow, videoWidth, videoHeight,
                                     WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer windowBuffer;

    AVFrame *pFrame = av_frame_alloc();

    AVFrame *pFrameRGBA = av_frame_alloc();
    if (pFrame == NULL || pFrameRGBA == NULL) {
        LOGD("Can not allocate video frame.");
        return -1;
    }

    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, pCodecCtx->width, pCodecCtx->height,
                                            1);
    uint8_t *buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
    av_image_fill_arrays(pFrameRGBA->data, pFrameRGBA->linesize, buffer, AV_PIX_FMT_RGBA,
                         pCodecCtx->width, pCodecCtx->height, 1);

    struct SwsContext *sws_ctx = sws_getContext(
            pCodecCtx->width,
            pCodecCtx->height,
            pCodecCtx->pix_fmt,
            pCodecCtx->width,
            pCodecCtx->height,
            AV_PIX_FMT_RGBA,
            SWS_BILINEAR,
            NULL, NULL, NULL
    );

    int frameFinished;
    AVPacket packet;
    while (av_read_frame(pFormatCtx, &packet) >= 0) {
        if (packet.stream_index == videoStream) {
            avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, &packet);
            if (frameFinished) {
                ANativeWindow_lock(nativeWindow, &windowBuffer, 0);
                sws_scale(sws_ctx, pFrame->data,
                          pFrame->linesize, 0, pCodecCtx->height,
                          pFrameRGBA->data, pFrameRGBA->linesize);
                uint8_t *dst = (uint8_t *) windowBuffer.bits;
                int dstStride = windowBuffer.stride * 4;
                uint8_t *src = pFrameRGBA->data[0];
                int srcStride = pFrameRGBA->linesize[0];

                for (int h = 0; h < videoHeight; ++h) {
                    memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
                }
                ANativeWindow_unlockAndPost(nativeWindow);
            }
        }
        av_packet_unref(&packet);
    }
    av_free(buffer);
    av_free(pFrameRGBA);
    av_free(pFrame);
    avcodec_close(pCodecCtx);
    avformat_close_input(&pFormatCtx);
    return 0;
}

static JNINativeMethod g_methods[] = {
        {
                "_test",
                "()Ljava/lang/String;",
                (void *) my_jni_test
        },
        {
                "_play",
                "(Ljava/lang/Object;)I",
                (void *) android_jni_play
        },
};

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = nullptr;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    jclass clazz = env->FindClass("com/example/ffmpegplayer/MainActivity");

    env->RegisterNatives(clazz, g_methods, sizeof(g_methods) / sizeof(g_methods[0]));
    return JNI_VERSION_1_6;
}