package com.example.publiclib.util;


import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * @Description: java类作用描述
 * @Author: jiazhu
 * @Date: 2019-07-30 09:53
 * @ClassName: LocalHandler
 */
public class LocalHandler extends Handler {
    private static final String TAG = "LocalHandler";
    /**
     * 集成IHandler的类必须为activity（包含looper）
     */
    private WeakReference<IHandler> weakReference;

    public LocalHandler(IHandler iHandler) {
        this.weakReference = new WeakReference<>(iHandler);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        IHandler iHandler = this.weakReference.get();
        if (iHandler != null) {
            iHandler.handlerMessage(msg);
        } else {
            Log.d(TAG, "handleMessage: error");
        }
    }

    /**
     *
     */
    public interface IHandler {
        /**
         * 处理message
         *
         * @param msg 消息
         */
        void handlerMessage(Message msg);
    }
}


