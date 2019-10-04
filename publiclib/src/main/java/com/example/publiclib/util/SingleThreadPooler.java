package com.example.publiclib.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author jiazhu
 */
public class SingleThreadPooler {
    private static SingleThreadPooler sSingleThreadPooler;
    private ThreadPoolExecutor singleThreadPool;

    private SingleThreadPooler() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("runnable_pool_%d").build();
        singleThreadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
    }

    public static SingleThreadPooler getInstance() {
        if (sSingleThreadPooler == null) {
            sSingleThreadPooler = new SingleThreadPooler();
        }
        return sSingleThreadPooler;
    }

    public void doTast(Runnable runnable) {
        singleThreadPool.execute(runnable);
    }
}
