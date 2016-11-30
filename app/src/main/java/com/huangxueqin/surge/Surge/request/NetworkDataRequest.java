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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class NetworkDataRequest implements Callable<Bitmap> {
    private static final int CONNECT_TIMEOUT = 2500;
    private static final int READ_TIMEOUT = 2500;

    private Token token;
    private Handler notifier;
    private SurgeCache cache;
    private boolean cancelled;
    private boolean suspended;
    private Object lock = new Object();

    public NetworkDataRequest(String url,
                              ImageView toView,
                              Point size,
                              Handler notifier,
                              SurgeCache cache) {
        this.notifier = notifier;
        this.cache = cache;
        this.token = new Token(url, toView, size);
    }

    public NetworkDataRequest(Token token,
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
        if (cancelled) {
            return null;
        }

        URL remoteURL = new URL(token.url);
        HttpURLConnection conn = (HttpURLConnection) remoteURL.openConnection();
        conn.setDoInput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        // connect firstly, avoid exception when decode network stream
        conn.connect();

        if (cancelled || Thread.currentThread().isInterrupted()) {
            return null;
        }

        while(suspended) {
            synchronized (lock) {
                lock.wait();
            }
        }

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK || cancelled) {
            sendMessage(RequestManager.MSG_DOWNLOAD_FAIL);
            return null;
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

    public class Token extends RequestToken implements RequestController {
        public Future<Bitmap> future;

        Token(String url, Size size) {
            super(url, size);
        }

        Token(String url, ImageView view, Point size) {
            super(url, view, size);
        }

        Token(RequestManager.Token t) {
            super(t);
        }

        @Override
        public void resume() {

        }

        @Override
        public void suspend() {

        }

        @Override
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
