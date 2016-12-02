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

public class ImageDataRequest extends RequestToken implements Handler.Callback {

    private ImageView target;
    private SurgeCache cache;
    private Handler taskNotifier;
    private Handler mainNotifier;

    private volatile boolean cancelled;
    private volatile boolean isStarted;

    private HttpFetcher fetcher;
    private Future<Bitmap> fetcherFuture;

    private Bitmap mData;

    public ImageDataRequest(ImageView target, String url, Handler mainNotifier, SurgeCache cache) {
        super(url);
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
        fetcher = new HttpFetcher(url, size, taskNotifier, cache);
        fetcherFuture = LoadDispatcher.getDefault().submitHttpTask(fetcher);
    }

    @Override
    public void start() {
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
                    mData = fetcherFuture.get();
                    fetcher = null;
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
                mData = (Bitmap) msg.obj;
                mainNotifier.obtainMessage(Request.MSG_REQUEST_SUCCESS, this).sendToTarget();
                break;
        }
        return false;
    }

    @Override
    public Bitmap getData() {
        return mData;
    }

    @Override
    public View getTarget() {
        return target;
    }
}
