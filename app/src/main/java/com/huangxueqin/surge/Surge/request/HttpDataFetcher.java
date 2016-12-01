package com.huangxueqin.surge.Surge.request;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.RequestManager;
import com.huangxueqin.surge.Surge.SurgeCache;
import com.huangxueqin.surge.Surge.Utils.Logger;
import com.huangxueqin.surge.Surge.Utils.Size;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class HttpDataFetcher implements Callable<Bitmap> {
    private static final int CONNECT_TIMEOUT = 2500;
    private static final int READ_TIMEOUT = 2500;

    private Handler notifier;
    private String url;
    private Size size;
    private SurgeCache cache;
    private boolean cancelled;

    public HttpDataFetcher(String url,
                           Size size,
                           Handler notifier,
                           SurgeCache cache) {
        this.url = url;
        this.size = size;
        this.notifier = notifier;
        this.cache = cache;
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

            if (cancelled || Thread.currentThread().isInterrupted()) {
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
            if (complete) {
                notifier.obtainMessage(RequestManager.MSG_DOWNLOAD_DONE).sendToTarget();
            } else {
                notifier.obtainMessage(RequestManager.MSG_DOWNLOAD_FAIL).sendToTarget();
            }
        }
    }

    public void cancel() {
        cancelled = true;
    }
}
