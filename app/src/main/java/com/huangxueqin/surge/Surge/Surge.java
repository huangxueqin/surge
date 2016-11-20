package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.widget.ImageView;

import com.huangxueqin.surge.Utils.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    public static void loadImage(final String url, final ImageView view) {
        RequestManager.get(view.getContext()).loadImage(url, view);
    }
}
