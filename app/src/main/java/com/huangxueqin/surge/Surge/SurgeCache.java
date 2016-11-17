package com.huangxueqin.surge.Surge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Environment;
import android.os.Handler;
import android.util.LruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by huangxueqin on 16/11/12.
 */

public class SurgeCache {

    public static interface Callback {
        void onResult(Bitmap result);
    }

    private static final String CACHE_FOLDER_NAME = "bitmaps";
    private static final long MAX_DISK_CACHE_SIZE = 150 * 1024 * 1024;

    private Context context;
    private LruCache<String, Bitmap> memCache;
    private SurgeDiskCache diskCache;

    private ExecutorService ioQueue;
    private Handler mainHandler;

    private static SurgeCache sInstance;

    public synchronized static SurgeCache getInstance(Context context) {
        if (sInstance == null) {
            try {
                sInstance = new SurgeCache(context);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sInstance;
    }

    private SurgeCache(Context context) throws IOException {
        if (sInstance != null) {
            throw new IllegalStateException("can not create multiple instance of SurgeCache");
        }
        this.context = context.getApplicationContext();
        // init memCache
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory()/1024);
        this.memCache = new LruCache<String, Bitmap>(maxMemory/8) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount()/1024;
            }
        };

        // init diskCache
        diskCache = SurgeDiskCache.open(getDiskCacheDirectory(context), MAX_DISK_CACHE_SIZE);

        // init ioQueue
        ioQueue = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(context.getMainLooper());
    }

    private static File getDiskCacheDirectory(Context context) {
        File cacheDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            cacheDir = context.getExternalCacheDir();
        } else {
            cacheDir = context.getCacheDir();
        }
        return new File(cacheDir, CACHE_FOLDER_NAME);
    }

    private static String cacheFileNameForKey(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(key.getBytes());
            StringBuffer result = new StringBuffer();
            for(byte b : hash) {
                String hex = Integer.toHexString(b);
                if (hex.length() == 1) {
                    result.append('0');
                }
                result.append(hex);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Future<Bitmap> retrieveImage(String url) {
        return retrieveImage(url, null);
    }

    public Future<Bitmap> retrieveImage(final String url, final Point preferSize) {
        return ioQueue.submit(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                Bitmap image = memCache.get(url);
                if (image != null && (preferSize == null ||
                        (preferSize.x <= image.getWidth() && preferSize.y <= image.getHeight()))) {
                    return image;
                }
                String key = cacheFileNameForKey(url);
                InputStream is = null;
                try {
                    is = diskCache.getInputStream(key);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (is != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    if (preferSize != null) {
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(is, null, options);
                        if (image == null || (options.outWidth > image.getWidth() && options.outHeight > image.getHeight())) {
                            options.inSampleSize = computeInSampleSize(options, preferSize);
                        }
                        options.inJustDecodeBounds = false;
                    }
                    if (image == null || (options.outWidth > image.getWidth() && options.outHeight > image.getHeight())) {
                        Bitmap decodedImage = BitmapFactory.decodeStream(is, null, options);
                        memCache.put(url, decodedImage);
                        image.recycle();
                        image = decodedImage;
                    }
                }
                return image;
            }
        });
    }

    public void storeImage(final String url, final InputStream is) {
        ioQueue.execute(new Runnable() {
            @Override
            public void run() {
                String key = cacheFileNameForKey(url);
                SurgeDiskCache.Editor editor = null;
                try {
                    editor = diskCache.edit(key);
                    BufferedOutputStream bos = new BufferedOutputStream(diskCache.edit(key).newOutputStream());
                    BufferedInputStream bis = null;
                    if (!(is instanceof BufferedInputStream)) {
                        bis = new BufferedInputStream(is);
                    } else {
                        bis = (BufferedInputStream) is;
                    }
                    byte[] data = new byte[1024];
                    int readLen = -1;
                    while ((readLen = bis.read(data)) != -1) {
                        bos.write(data, 0, readLen);
                    }
                    bos.flush();
                    bos.close();
                    editor.commit();
                } catch (IOException e) {
                    e.printStackTrace();
                    if (editor != null) {
                        try {
                            editor.abort();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private static int computeInSampleSize(BitmapFactory.Options ops, Point preferSize) {
        int inSampleSize = 1;
        final int width = ops.outWidth;
        final int height = ops.outHeight;
        if (width > preferSize.x && height > preferSize.y) {
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;
            while (halfWidth/inSampleSize >= preferSize.x && halfHeight/inSampleSize >= preferSize.y) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void postResult(final Bitmap bitmap, final Callback callback) {
        if (callback == null) {
            return;
        }
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onResult(bitmap);
            }
        });
    }
}
