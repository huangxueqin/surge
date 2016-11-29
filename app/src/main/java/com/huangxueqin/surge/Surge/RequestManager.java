package com.huangxueqin.surge.Surge;

import android.app.Activity;
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

public class RequestManager implements Handler.Callback {
    private static final int MAX_RETRY_TIMES = 3;

    static final int MSG_RETRIEVE_DONE = 0x100;
    static final int MSG_RETRIEVE_FAIL = 0x101;
    static final int MSG_DOWNLOAD_DONE = 0x102;
    static final int MSG_DOWNLOAD_FAIL = 0x103;
    static final int MSG_CANCEL        = 0x104;


    private static RequestManager INSTANCE;

    private Surge surge;
    private Handler mainHandler;
    private final ExecutorService downloadPool = Executors.newFixedThreadPool(5);
    private final ExecutorService cacheRetrievePool = Executors.newSingleThreadExecutor();
    private HandlerThread notifyHandlerThread;
    private NotifyHandler notifyHandler;
    private final HashMap<View, Token> operationMap = new HashMap<>();
    private final HashMap<String, List<ImageView>> requestQueues = new HashMap<>();

    private RequestManager(Context context) {
        this.surge = Surge.get(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
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

    public void loadImage(final String url, final ImageView view, final Point preferSize) {
        cancelCurrentLoading(view);
        cacheRetrievePool.execute(new RetrieveTask(new Token(url, view, preferSize)));
    }

    public void loadImage(final String url, final ImageView view) {
        loadImage(url, view, null);
    }

    public void clear(final ImageView view) {
        cancelCurrentLoading(view);
        view.setImageDrawable(null);
    }

    private void cancelCurrentLoading(final ImageView view) {
        // just cancel set image, not cancel downloading process
        Message msg = notifyHandler.obtainMessage();
        msg.what = MSG_CANCEL;
        msg.obj = view;
        msg.sendToTarget();
    }

    private void onRetrieveCacheFail(Token token) {
        List<ImageView> queue = requestQueues.get(token.url);
        if (queue == null) {
            queue = new LinkedList<>();
            queue.add(token.view);
            requestQueues.put(token.url, queue);
            RequestOperation operation = new RequestOperation(token.url, token.view, token.size,
                    notifyHandler, surge.cache);
            Token lastRequestToken = operationMap.get(token.view);
            if (lastRequestToken != null && lastRequestToken instanceof RequestOperation.Token) {
                ((RequestOperation.Token)lastRequestToken).cancel();
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
            Token queuedViewToken = operationMap.get(token.view);
            if (queuedViewToken == null || !queuedViewToken.fits(token)) {
                operationMap.put(token.view, token);
            }
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
                Token queuedViewToken = operationMap.get(queuedView);
                if (token.fits(queuedViewToken)) {
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
                    Token queuedViewToken = operationMap.get(queuedView);
                    if (token.fits(queuedViewToken)) {
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
            List<ImageView> queue = requestQueues.get(token.url);
            Iterator<ImageView> it = queue.iterator();
            while (it.hasNext()) {
                ImageView queuedView = it.next();
                Token queuedViewToken = operationMap.get(queuedView);
                if (token.fits(queuedViewToken)) {
                    operationMap.remove(queuedView);
                }
            }
            requestQueues.remove(token.url);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
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
                case MSG_RETRIEVE_DONE:
                    Token token = (Token)msg.obj;
                    setImage(token.view, surge.cache.retrieveImage(token.url, token.size));
                    break;
                case MSG_CANCEL:
                    operationMap.remove(msg.obj);
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
            Message msg = notifyHandler.obtainMessage();
            msg.obj = token;
            msg.what = image == null ? MSG_RETRIEVE_FAIL : MSG_RETRIEVE_DONE;
            msg.sendToTarget();
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
            if (this == t) return true;
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
