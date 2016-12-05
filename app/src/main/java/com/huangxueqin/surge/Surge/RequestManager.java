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
import com.huangxueqin.surge.Surge.request.Request;
import com.huangxueqin.surge.Surge.request.RequestToken;

import java.util.HashMap;
import java.util.HashSet;
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
    private final HashMap<String, HashSet<RequestToken>> waitingViewQueues = new HashMap<>();

    // record requests are current executing
    private final List<Request> requests = new LinkedList<>();
    private final List<Request> pendingRequests = new LinkedList<>();
    private boolean isStarted = false;
    private boolean isStopped = false;
    private boolean isDestroyed = false;

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
        switch (msg.what) {
            case Request.MSG_REQUEST_SUCCESS:
                RequestToken token = (RequestToken) msg.obj;

                break;
            case Request.MSG_REQUEST_FAIL:
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onStart() {
        Logger.D("on Start");
        isStarted = true;
        isStopped = false;
        isDestroyed = false;
        for (Request request: pendingRequests) {
            request.resume();
        }
    }

    @Override
    public void onStop() {
        Logger.D("on Stop");
        isStopped = true;
        for (Request request: requests) {

        }
    }

    @Override
    public void onDestroy() {
        Logger.D("on Destroy");
        isDestroyed = true;
    }

    public class RequestBuilder {
        private String url;

        public RequestBuilder url(String url) {
            this.url = url;
            return this;
        }

        public void into(ImageView view) {
            if (url == null) {
                throw new IllegalArgumentException("can not start request without a URL");
            }
            if (isDestroyed) {
                throw new IllegalStateException("RequestManager has already destroyed");
            }
            ImageDataRequest request = new ImageDataRequest(view, url, mainNotifier, surge.cache);
            RequestToken oldRequest = currentRequestMap.get(view);
            if (oldRequest != null) {
                oldRequest.cancel();
            }
            currentRequestMap.put(view, request);
            boolean shouldStartRequest = waitingViewQueues.get(url) != null;
            waitingViewQueues.get(url).add(request);

            if (shouldStartRequest) {
                requests.add(request);
                if (isStarted && !isStopped) {
                    request.start();
                } else if (isStopped) {
                    pendingRequests.add(request);
                }
            }
        }
    }
}
