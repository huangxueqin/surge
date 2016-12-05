package com.huangxueqin.surge.Surge.request;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.Utils.Size;
import com.huangxueqin.surge.Surge.cache.SurgeCache;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 2016/12/1.
 */

public class ImageDataRequest extends RequestToken implements Request, Handler.Callback {
    private enum RequestPhase {STARTED, }

    private ImageView target;
    private Bitmap image;

    private SurgeCache cache;
    private Handler taskNotifier;
    private Handler mainNotifier;

    private volatile boolean cancelled;
    private volatile boolean isStarted;
    private volatile boolean isSuspended;

    private HttpFetcher httpFetcher;
    private Future<Bitmap> fetcherFuture;


    public ImageDataRequest(ImageView target, String url, Handler mainNotifier, SurgeCache cache) {
        super(url);
        this.target = target;
        this.cache = cache;
        this.taskNotifier = LoadDispatcher.getDefault().obtainNotifier(this);
        this.mainNotifier = mainNotifier;
    }

    private void fetchDataFromCache() {
        LoadDispatcher.getDefault().submitIoTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Bitmap image = cache.retrieveImage(url, size);
                int what = image == null ? LoadDispatcher.MSG_IO_TASK_FAIL : LoadDispatcher.MSG_IO_TASK_OK;
                taskNotifier.obtainMessage(what, image).sendToTarget();
                return null;
            }
        });
    }

    private void fetchDataFromNetwork() {
        fetcherFuture = LoadDispatcher.getDefault().submitHttpTask(httpFetcher);
    }

    @Override
    public void start() {
        if (sizeReady(target)) {

        }
        if (target.getWidth() > 0 && target.getHeight() > 0) {
            isStarted = true;
            size = new Size(target.getWidth(), target.getHeight());
            fetchDataFromCache();
        } else {
            target.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (!cancelled) {
                        isStarted = true;
                        size = new Size(target.getWidth(), target.getHeight());
                        fetchDataFromCache();
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public void resume() {

    }

    @Override
    public void suspend() {

    }

    @Override
    public void cancel() {
        cancelled = true;
        if (fetcherFuture != null) {
            fetcherFuture.cancel(true);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (cancelled) {
            return true;
        }
        switch (msg.what) {
            case LoadDispatcher.MSG_HTTP_TASK_FAIL:
                mainNotifier.obtainMessage(Request.MSG_REQUEST_FAIL, this).sendToTarget();
                break;
            case LoadDispatcher.MSG_HTTP_TASK_OK:
                try {
                    image = fetcherFuture.get();
                    httpFetcher = null;
                    fetcherFuture = null;
                    mainNotifier.obtainMessage(Request.MSG_REQUEST_SUCCESS, this).sendToTarget();
                } catch (Exception e) {
                    mainNotifier.obtainMessage(Request.MSG_REQUEST_FAIL, this).sendToTarget();
                }
                break;
            case LoadDispatcher.MSG_IO_TASK_FAIL:
                fetchDataFromNetwork();
                break;
            case LoadDispatcher.MSG_IO_TASK_OK:
                image = (Bitmap) msg.obj;
                mainNotifier.obtainMessage(Request.MSG_REQUEST_SUCCESS, this).sendToTarget();
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public Bitmap getData() {
        return image;
    }

    @Override
    public View getTarget() {
        return target;
    }

    private static boolean sizeReady(View view) {
        return view.getWidth() > 0 && view.getHeight() > 0;
    }

    private static Size getSize(View view) {
        return new Size(view.getWidth(), view.getHeight());
    }
}
