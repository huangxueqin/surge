package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.Utils.Logger;

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
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;



    String url;
    ImageView toView;
    Token token;
    private Point size;
    private Handler notifier;
    private SurgeCache cache;
    private boolean cancelled;

    public RequestOperation(String url,
                            ImageView toView,
                            Point size,
                            Handler notifier,
                            SurgeCache cache) {
        this.url = url;
        this.size = size;
        this.notifier = notifier;
        this.cache = cache;

        this.token = new Token();
        token.url = url;
        token.view = toView;
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
        URL remote = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) remote.openConnection();
        Logger.D("start to request url: " + url);
        conn.setDoInput(true);
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK || cancelled) {
            sendMessage(RequestManager.MSG_DOWNLOAD_FAIL);
            return bitmap;
        }
        Logger.D("get response code");
        InputStream is = conn.getInputStream();
        boolean success = cache.storeImage(url, is);
        Logger.D("download image(" + url + ") " + success);
        if (success) {
            bitmap = cache.retrieveImage(url, size);
        }
        sendMessage(success ? RequestManager.MSG_DOWNLOAD_COMPLETE : RequestManager.MSG_DOWNLOAD_FAIL);
        return bitmap;
    }

    public class Token {
        String url;
        ImageView view;
        Future<Bitmap> future;

        public void cancel() {
            if (future != null) {
                future.cancel(true);
            }
            cancelled = true;
        }
    }
}
