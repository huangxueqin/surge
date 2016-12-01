package com.huangxueqin.surge.Surge.request;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.RequestManager;
import com.huangxueqin.surge.Surge.SurgeCache;
import com.huangxueqin.surge.Surge.Utils.Size;

import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 2016/12/1.
 */

public class ImageDataRequest extends RequestToken implements Handler.Callback {
    private ImageView target;
    private SurgeCache cache;
    private Handler mainHandler;
    private boolean cancelled;

    private Future<Bitmap> fetcherFuture;

    public ImageDataRequest(ImageView target, String url, SurgeCache cache) {
        super(url);
        this.cache = cache;
        this.mainHandler = new Handler(Looper.getMainLooper(), this);
    }

    @Override
    public void start() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void suspend() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what == RequestManager.MSG_DOWNLOAD_DONE) {
            if (!cancelled) {
                try {
                    target.setImageBitmap(fetcherFuture.get());
                } catch (Exception e) {}
            }
        } else if (msg.what == RequestManager.MSG_DOWNLOAD_FAIL) {

        }
        return false;
    }
}
