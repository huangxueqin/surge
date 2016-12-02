package com.huangxueqin.surge.Surge.request;

import android.graphics.Bitmap;
import android.os.Handler;

import com.huangxueqin.surge.Surge.RequestManager;
import com.huangxueqin.surge.Surge.cache.SurgeCache;
import com.huangxueqin.surge.Surge.Utils.Size;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class HttpFetcher implements Callable<Bitmap> {
    private static final int CONNECT_TIMEOUT = 2500;
    private static final int READ_TIMEOUT = 2500;

    private Handler notifier;
    private String url;
    private Size size;
    private SurgeCache cache;
    private boolean cancelled;

    public HttpFetcher(String url,
                       Size size,
                       Handler notifier,
                       SurgeCache cache) {
        this.url = url;
        this.size = size;
        this.notifier = notifier;
        this.cache = cache;
    }

    private boolean isTaskCancelled() {
        return cancelled || Thread.currentThread().isInterrupted();
    }

    @Override
    public Bitmap call() throws Exception {
        if (cancelled) {
            return null;
        }
        boolean complete = false;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoInput(true);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);

            // connect firstly, avoid exception when decode network stream
            conn.connect();

            if (isTaskCancelled()) {
                return null;
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                if (cache.storeImage(url, is)) {
                    complete = true;
                    return cache.retrieveImage(url, size);
                }
            }
            return null;
        } finally {
            if (!isTaskCancelled()) {
                int what = complete ? LoadDispatcher.MSG_HTTP_TASK_OK : LoadDispatcher.MSG_HTTP_TASK_FAIL;
                notifier.obtainMessage(what).sendToTarget();
            }
        }
    }

    public void cancel() {
        cancelled = true;
    }
}
