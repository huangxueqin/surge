package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.widget.ImageView;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class RequestOption {
    private Context context;
    private SurgeCache cache;
    private String url;
    private ImageView imageView;

    public RequestOption(Context context) {
        this.context = context;
        this.cache = SurgeCache.getInstance(context);
        if (cache == null) {
            throw new IllegalStateException("can not init cache");
        }
    }

    public RequestOption load(String url) {
        this.url = url;
        return this;
    }

    public void into(ImageView imageView) {
        this.imageView = imageView;
        if (url != null) {
            startLoadImage();
        }
    }

    private void startLoadImage() {

    }
}
