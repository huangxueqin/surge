package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 16/11/13.
 */

public class Surge {
    Context context;
    ExecutorService requestPool = Executors.newFixedThreadPool(5);
    SurgeCache cache;
    Handler mainHandler;

    public Surge(Context context) {
        this.context = context;
        mainHandler = new Handler(context.getMainLooper());
        cache = SurgeCache.getInstance(context);
    }

    public void loadImage(final String url, final ImageView imageView) {
        requestPool.execute(new Runnable() {
            @Override
            public void run() {
                Point preferSize = new Point(imageView.getWidth(), imageView.getHeight());
                Future<Bitmap> bitmapFuture = cache.retrieveImage(url, preferSize);
                Bitmap image = null;
                try {
                    image = bitmapFuture.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if (image != null) {
                    setImage(image, imageView);
                    return;
                }
                InputStream is = new SurgeDownloader(url).call();
                if (is == null) {
                    return;
                }
                cache.storeImage(url, is);
                bitmapFuture = cache.retrieveImage(url, preferSize);
                try {
                    if ((image = bitmapFuture.get()) != null) {
                        setImage(image, imageView);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setImage(final Bitmap image, final ImageView imageView) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(image);
            }
        });
    }
}
