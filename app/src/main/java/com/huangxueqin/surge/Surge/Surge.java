package com.huangxueqin.surge.Surge;

import android.content.Context;

import com.huangxueqin.surge.Surge.cache.SurgeCache;
import com.huangxueqin.surge.Surge.lifecycle.RequestManagerRetriever;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class Surge {
    private static final String DISK_CACHE_DIR = "bitmaps";
    private static final long DISK_CACHE_SIZE = 150 * 1024 * 1024;

    private static volatile Surge surge;
    SurgeCache cache;

    private Surge(Context context) {
        if (surge != null) {
            throw new IllegalStateException("Can not initialize multiple \"Surge\" instance");
        }
        cache = new SurgeCache(context, DISK_CACHE_DIR, DISK_CACHE_SIZE);
    }

    public static Surge get(Context context) {
        if (surge == null) {
            synchronized (Surge.class) {
                if (surge == null) {
                    surge = new Surge(context);
                }
            }
        }
        return surge;
    }

    public static RequestManager with(Context context) {
        return RequestManagerRetriever.getDefault().get(context);
    }
}
