package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.Utils.Logger;
import com.huangxueqin.surge.Surge.lifecycle.LifecycleListener;
import com.huangxueqin.surge.Surge.request.HttpFetcher;
import com.huangxueqin.surge.Surge.request.ImageDataRequest;
import com.huangxueqin.surge.Surge.request.RequestToken;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by huangxueqin on 16/11/19.
 */

public class RequestManager implements Handler.Callback, LifecycleListener {
    private Surge surge;
    private Handler mainNotifier;
    private final HashMap<View, RequestToken> currentRequestMap = new HashMap<>();
    private final HashMap<String, View> waitingViewQueues = new HashMap<>();

    // record requests are current executing
    private final List<RequestToken> requests = new LinkedList<>();
    private final List<RequestToken> pendingRequests = new LinkedList<>();

    public RequestManager(Context context) {
        this.surge = Surge.get(context);
        this.mainNotifier = new Handler(Looper.getMainLooper(), this);
    }

    public void clear(final ImageView view) {
        view.setImageDrawable(null);
    }

    public RequestBuilder load(String url) {
        RequestBuilder builder = new RequestBuilder();
        return builder.url(url);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void onStart() {
        Logger.D("on Start");
    }

    @Override
    public void onStop() {
        Logger.D("on Stop");
    }

    @Override
    public void onDestroy() {
        Logger.D("on Destroy");
    }

    public class RequestBuilder {
        private String url;

        public RequestBuilder url(String url) {
            this.url = url;
            return this;
        }

        public RequestBuilder into(ImageView view) {
            if (url == null) {
                throw new IllegalArgumentException("can not start request without a URL");
            }
            ImageDataRequest request = new ImageDataRequest(view, url, mainNotifier, surge.cache);

        }
    }
}
