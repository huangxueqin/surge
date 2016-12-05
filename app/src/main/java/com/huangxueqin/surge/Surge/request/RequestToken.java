package com.huangxueqin.surge.Surge.request;


import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import com.huangxueqin.surge.Surge.Utils.Size;

/**
 * Created by huangxueqin on 2016/11/30.
 * RequestToken are used to identify a network request
 */

abstract class RequestToken {
    String url;
    Size size;

    public RequestToken(String url, Size size) {
        this.url = url;
        this.size = new Size(size.width, size.height);
    }

    public RequestToken(RequestToken token) {
        this(token.url, token.size);
    }

    public RequestToken(String url) {
        this(url, null);
    }

    public String getRequestURL() {
        return url;
    }

    public Size getRequestSize() {
        return size;
    }

    public abstract Bitmap getData();

    public abstract View getTarget();
}
