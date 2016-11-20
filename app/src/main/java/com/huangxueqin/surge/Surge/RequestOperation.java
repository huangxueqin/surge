package com.huangxueqin.surge.Surge;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.Utils.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class RequestOperation implements Callable<Bitmap> {
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    Token token;
    private Handler notifier;
    private SurgeCache cache;
    private boolean cancelled;

    public RequestOperation(String url,
                            ImageView toView,
                            Point size,
                            Handler notifier,
                            SurgeCache cache) {
        this.notifier = notifier;
        this.cache = cache;
        this.token = new Token(url, toView, size);
    }

    public RequestOperation(Token token,
                            Handler notifier,
                            SurgeCache cache) {
        this.notifier = notifier;
        this.cache = cache;
        this.token = token;
    }

    private void sendMessage(int what) {
        Message msg = notifier.obtainMessage();
        msg.what = what;
        msg.obj = token;
        msg.sendToTarget();
    }

    @Override
    public Bitmap call() throws Exception {
        Bitmap bitmap = null;
        URL remote = new URL(token.url);
        HttpURLConnection conn = (HttpURLConnection) remote.openConnection();
        Logger.D("start to request url: " + token.url);
        conn.setDoInput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK || cancelled) {
            sendMessage(RequestManager.MSG_DOWNLOAD_FAIL);
            return bitmap;
        }
        Logger.D("get response code");
        InputStream is = conn.getInputStream();
        boolean success = cache.storeImage(token.url, is);
        Logger.D("download image(" + token.url + ") " + success);
        if (success) {
            bitmap = cache.retrieveImage(token.url, token.size);
        }
        sendMessage(success ? RequestManager.MSG_DOWNLOAD_DONE : RequestManager.MSG_DOWNLOAD_FAIL);
        return bitmap;
    }

    public class Token extends RequestManager.Token {
        Future<Bitmap> future;
        int retry;

        Token(String url, ImageView view) {
            super(url, view);
        }

        Token(String url, ImageView view, Point size) {
            super(url, view, size);
        }

        Token(RequestManager.Token t) {
            super(t);
        }

        public void cancel() {
            if (future != null) {
                future.cancel(true);
            }
            cancelled = true;
        }

        public Bitmap get() throws ExecutionException, InterruptedException {
            if (future != null) {
                return future.get();
            }
            return null;
        }
    }
}
