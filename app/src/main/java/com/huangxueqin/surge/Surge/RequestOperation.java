package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class RequestOperation implements Callable<Bitmap> {
    private static final String TAG = "RequestOperation";

    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    public static final int MSG_DOWNLOAD_COMPLETE = 0x100;

    private Object key;
    private String url;
    private Point size;
    private Handler notifier;
    private SurgeCache cache;
    private boolean cancelled;

    public RequestOperation(Object key,
                            String url,
                            Point size,
                            Handler notifier,
                            SurgeCache cache) {
        this.key = key;
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
        Bitmap image = null;
        URL remote = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) remote.openConnection();
        Log.d(TAG, "start to request url");
        conn.setDoInput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK || cancelled) {
            return null;
        }
        Log.d(TAG, "get response code");
        InputStream is = conn.getInputStream();
        cache.storeImage(url, is);
        if (cancelled) {
            return null;
        }
        image = cache.retrieveImage(url, size);
        if (cancelled) {
            return null;
        }
        Message msg = notifier.obtainMessage();
        msg.what = MSG_DOWNLOAD_COMPLETE;
        msg.sendToTarget();
        msg.obj = key;
        D("url: " + url);
        return image;
    }

    public Token submitTo(ExecutorService service) {
        Token t = new Token();
        t.future = service.submit(this);
        t.url = url;
        return t;
    }

    public class Token {
        public String url;


        private Future<Bitmap> future;

        public void cancel() {
            future.cancel(true);
            cancelled = true;
        }

        public Bitmap get() throws ExecutionException, InterruptedException {
            return future.get();
        }
    }

    private static void D(String msg) {
        Log.d(RequestOperation.class.getSimpleName(), msg);
    }
}
