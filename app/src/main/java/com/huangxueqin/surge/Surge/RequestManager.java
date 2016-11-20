package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by huangxueqin on 16/11/19.
 */

public class RequestManager {
    private static RequestManager INSTANCE;

    private final HashMap<View, RequestOperation.Token> map = new HashMap<>();
    private final ExecutorService executePool = Executors.newFixedThreadPool(5);

    private Context context;
    private Surge surge;
    private Handler handler;

    private RequestManager(Context context) {
        this.context = context;
        this.surge = Surge.get(context);
        this.handler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case RequestOperation.MSG_DOWNLOAD_COMPLETE:
                        ImageView key = (ImageView) msg.obj;
                        RequestOperation.Token t = map.get(key);
                        if (t != null) {
                            D("t.url: " + t.url);
                            try {
                                Bitmap image = t.get();
                                key.setImageBitmap(image);
                                map.remove(key);
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else {
                            D("t == null");
                        }
                        break;
                }
            }
        };
    }

    public static RequestManager get(Context context) {
        if (INSTANCE == null) {
            synchronized (RequestManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new RequestManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public synchronized void loadImage(String url, ImageView view) {
        RequestOperation.Token t = map.get(view);
        if (t != null) {
            t.cancel();
        }
        Bitmap image = surge.cache.retrieveImage(url, null);
        if (image != null) {
            view.setImageBitmap(image);
        } else {
            RequestOperation operation = new RequestOperation(view, url, null, handler, surge.cache);
            t = operation.submitTo(executePool);
            D("submitted");
            map.put(view, t);
        }
    }

    private static void D(String msg) {
        Log.d(RequestManager.class.getSimpleName(), msg);
    }
}
