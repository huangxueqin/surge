package com.huangxueqin.surge.Surge.request;

import android.os.Handler;
import android.os.HandlerThread;

import com.huangxueqin.surge.Surge.Utils.Singleton;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 2016/12/2.
 */

public class LoadDispatcher {
    private static final int MSG_FIRST = 0xD15;
    static final int MSG_HTTP_TASK_OK       = MSG_FIRST + 1;
    static final int MSG_HTTP_TASK_FAIL     = MSG_FIRST + 2;
    static final int MSG_IO_TASK_OK         = MSG_FIRST + 3;
    static final int MSG_IO_TASK_FAIL       = MSG_FIRST + 4;

    private ExecutorService httpExecutePool;
    private ExecutorService ioExecutorPool;
    private HandlerThread notifyThread;

    private LoadDispatcher() {
        httpExecutePool = Executors.newFixedThreadPool(4);
        ioExecutorPool = Executors.newFixedThreadPool(2);
        notifyThread = new HandlerThread("notify thread");
        notifyThread.start();
    }

    private final static Singleton<LoadDispatcher> sDefault = new Singleton<LoadDispatcher>() {
        @Override
        protected LoadDispatcher create() {
            return new LoadDispatcher();
        }
    };

    public static LoadDispatcher getDefault() {
        return sDefault.get();
    }

    public <T> Future<T> submitHttpTask(Callable<T> callable) {
        return httpExecutePool.submit(callable);
    }

    public <T> Future<T> submitIoTask(Callable<T> callable) {
        return ioExecutorPool.submit(callable);
    }

    public Handler obtainNotifier(Handler.Callback callback) {
        return new Handler(notifyThread.getLooper(), callback);
    }

    public void shutdown() {
        httpExecutePool.shutdownNow();
        ioExecutorPool.shutdownNow();
        notifyThread.quit();
    }
}
