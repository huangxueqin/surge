package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.Utils.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by huangxueqin on 16/11/19.
 */

public class RequestManager {
    static final int MSG_RETRIEVE_COMPLETE = 0x100;
    static final int MSG_RETRIEVE_FAIL     = 0x101;
    static final int MSG_DOWNLOAD_COMPLETE = 0x102;
    static final int MSG_DOWNLOAD_FAIL     = 0x103;

    private static RequestManager INSTANCE;

    private Context context;
    private Surge surge;
    private Handler mainHandler;
    private final ExecutorService executePool = Executors.newFixedThreadPool(5);
    private final HashMap<View, RequestOperation.Token> operationMap = new HashMap<>();
    private final HashMap<String, List<ImageView>> requestQueues = new HashMap<>();
    private HandlerThread notifyHandlerThread;
    private NotifyHandler notifyHandler;


    private RequestManager(Context context) {
        this.context = context;
        this.surge = Surge.get(context);
        this.mainHandler = new Handler(context.getMainLooper());
        notifyHandlerThread = new HandlerThread("notify handler thread");
        notifyHandlerThread.start();
        notifyHandler = new NotifyHandler(notifyHandlerThread.getLooper());
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

    /**
     * @param url
     * @param v
     * @return return true if url is already in request, else return false
     */
    private boolean addToRequestQueue(String url, ImageView v) {
        boolean exist = false;
        List<ImageView> queue = requestQueues.get(url);
        if (queue == null) {
            queue = new LinkedList<>();
            queue.add(v);
            requestQueues.put(url, queue);
        } else {
            for (ImageView iv : queue) {
                if (iv == v) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                queue.add(v);
            }
        }
        return exist;
    }

    public synchronized void loadImage(String url, ImageView view) {
        RetrieveTask retrieveTask = new RetrieveTask(url, view, null);
        executePool.execute(retrieveTask);
    }


    private class NotifyHandler extends Handler {

        public NotifyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DOWNLOAD_COMPLETE:
                    RequestOperation.Token t0 = (RequestOperation.Token) msg.obj;
                    String url = t0.url;
                    List<ImageView> queues = requestQueues.get(url);
                    if (queues != null) {
                        Iterator<ImageView> it = queues.iterator();
                        while(it.hasNext()) {
                            ImageView iv = it.next();
                            RequestOperation.Token t = operationMap.get(iv);
                            if (t == null || t.url.equals(url)) {
                                try {
                                    setImage(iv, t0.future.get());
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }
                            it.remove();
                        }
                    }
                    break;
                case MSG_DOWNLOAD_FAIL:
                    RequestOperation.Token t1 = (RequestOperation.Token) msg.obj;
                    operationMap.remove(t1.view);
                    requestQueues.remove(t1.url);
                    break;
                case MSG_RETRIEVE_COMPLETE:
                    RetrieveTask.Token t2 = (RetrieveTask.Token) msg.obj;
                    setImage(t2.view, t2.image);
                    break;
                case MSG_RETRIEVE_FAIL:
                    RetrieveTask.Token t3 = (RetrieveTask.Token) msg.obj;
                    boolean startRequest = (addToRequestQueue(t3.url, t3.view) == false);
                    RequestOperation operation = new RequestOperation(t3.url, t3.view, t3.size, notifyHandler, surge.cache);
                    RequestOperation.Token token = operationMap.get(t3.view);
                    if (token != null && (requestQueues.get(token.url) == null)) {
                        token.cancel();
                    }
                    operationMap.put(t3.view, operation.token);
                    if (startRequest) {
                        operation.token.future = executePool.submit(operation);
                    }
                    break;
            }
        }
    }

    private void setImage(final ImageView view, final Bitmap bitmap) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                view.setImageBitmap(bitmap);
            }
        });
    }

    private class RetrieveTask implements Runnable {

        class Token {
            String url;
            ImageView view;
            Point size;
            Bitmap image;
        }

        Token token;

        public RetrieveTask(String url, ImageView view, Point size) {
            token = new Token();
            token.url = url;
            token.view = view;
            token.size = size;
        }

        @Override
        public void run() {
            token.image = surge.cache.retrieveImage(token.url, token.size);
            Message msg = notifyHandler.obtainMessage();
            msg.obj = token;
            msg.what = (token.image != null ? MSG_RETRIEVE_COMPLETE : MSG_RETRIEVE_FAIL);
            msg.sendToTarget();
        }
    }
}
