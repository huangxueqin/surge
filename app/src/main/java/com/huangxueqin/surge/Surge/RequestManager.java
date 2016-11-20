package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;

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
    private static final int MAX_RETRY_TIMES = 3;

    static final int MSG_RETRIEVE_DONE = 0x100;
    static final int MSG_RETRIEVE_FAIL = 0x101;
    static final int MSG_DOWNLOAD_DONE = 0x102;
    static final int MSG_DOWNLOAD_FAIL = 0x103;


    private static RequestManager INSTANCE;

    private Surge surge;
    private Handler mainHandler;
    private final ExecutorService downloadPool = Executors.newFixedThreadPool(5);
    private final ExecutorService cacheRetrievePool = Executors.newSingleThreadExecutor();
    private HandlerThread notifyHandlerThread;
    private NotifyHandler notifyHandler;
    private final HashMap<View, RequestOperation.Token> operationMap = new HashMap<>();
    private final HashMap<String, List<ImageView>> requestQueues = new HashMap<>();

    private RequestManager(Context context) {
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

    public void loadImage(String url, ImageView view, Point preferSize) {
        cacheRetrievePool.execute(new RetrieveTask(new Token(url, view, preferSize)));
    }

    public void loadImage(String url, ImageView view) {
        loadImage(url, view, null);
    }

    private void onRetrieveCacheFail(Token token) {
        List<ImageView> queue = requestQueues.get(token.url);
        if (queue == null) {
            queue = new LinkedList<>();
            queue.add(token.view);
            requestQueues.put(token.url, queue);
            RequestOperation operation = new RequestOperation(token.url, token.view, token.size,
                    notifyHandler, surge.cache);
            RequestOperation.Token lastRequest = operationMap.get(token.view);
            if (lastRequest != null) {
                lastRequest.cancel();
            }
            operationMap.put(token.view, operation.token);
            operation.token.future = downloadPool.submit(operation);
        } else {
            // add view to request queue of the URL
            Iterator<ImageView> it = queue.iterator();
            while (it.hasNext()) {
                if (it.next() == token.view) {
                    // already in request queue, return;
                    return;
                }
            }
            queue.add(token.view);
        }
    }

    private void onDownloadDone(RequestOperation.Token token) {
        Bitmap image = null;
        try {
            image = token.get();
        } catch (Exception e) {
            image = surge.cache.retrieveImage(token.url, token.size);
        }
        if (image == null) {
            onDownloadFail(token);
            return;
        }
        String url = token.url;
        List<ImageView> queue = requestQueues.get(url);
        if (queue != null) {
            Iterator<ImageView> it = queue.iterator();
            while (it.hasNext()) {
                ImageView queuedView = it.next();
                RequestOperation.Token queuedViewToken = operationMap.get(queuedView);
                if (queuedViewToken == null || token.fits(queuedViewToken)) {
                    setImage(queuedView, image);
                    operationMap.remove(queuedView);
                }
            }
        }
        requestQueues.remove(url);
    }

    private void onDownloadFail(RequestOperation.Token token) {
        if (token.retry < MAX_RETRY_TIMES) {
            List<ImageView> queue = requestQueues.get(token.url);
            if (queue != null) {
                boolean willRetry = false;
                Iterator<ImageView> it = queue.iterator();
                while (it.hasNext()) {
                    ImageView queuedView = it.next();
                    RequestOperation.Token queuedViewToken = operationMap.get(queuedView);
                    if (queuedViewToken == null || token.fits(queuedViewToken)) {
                        willRetry = true;
                        operationMap.put(queuedView, token);
                    }
                }
                if (willRetry) {
                    token.retry += 1;
                    token.future = downloadPool.submit(new RequestOperation(token, notifyHandler, surge.cache));
                }
            }
        } else {
            operationMap.remove(token.view);
            requestQueues.remove(token.url);
        }
    }

    private class NotifyHandler extends Handler {

        public NotifyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DOWNLOAD_DONE:
                    onDownloadDone((RequestOperation.Token) msg.obj);
                    break;
                case MSG_DOWNLOAD_FAIL:
                    onDownloadFail((RequestOperation.Token) msg.obj);
                    break;
                case MSG_RETRIEVE_FAIL:
                    onRetrieveCacheFail((Token) msg.obj);
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

    /**
     * running in cacheRetrievePool
     */
    private class RetrieveTask implements Runnable {
        Token token;

        public RetrieveTask(Token token) {
            this.token = token;
        }

        @Override
        public void run() {
            Bitmap image = surge.cache.retrieveImage(token.url, token.size);
            if (image == null) {
                Message msg = notifyHandler.obtainMessage();
                msg.obj = token;
                msg.what = MSG_RETRIEVE_FAIL;
                msg.sendToTarget();
            } else {
                // retrieve success
                setImage(token.view, image);
            }
        }
    }

    /**
     * Token that identify an image loading job
     */
    static class Token {
        final String url;
        final ImageView view;
        final Point size;

        Token(String url, ImageView view) {
            this(url, view, null);
        }

        Token(String url, ImageView view, Point size) {
            this.url = url;
            this.view = view;
            this.size = size;
        }

        Token(Token t) {
            this.url = t.url;
            this.view = t.view;
            this.size = t.size;
        }

        public boolean fits(Token t) {
            return t != null && t.url.equals(url);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof Token) {
                Token t = (Token) o;
                return t.url.equals(url) && t.view == view && sizeEqual(size, t.size);
            }
            return false;
        }

        private static boolean sizeEqual(final Point lhs, final Point rhs) {
            if (lhs == null) { return rhs == null; }
            if (rhs == null) { return false; }
            return lhs.x == rhs.x && lhs.y == rhs.y;
        }
    }
}
