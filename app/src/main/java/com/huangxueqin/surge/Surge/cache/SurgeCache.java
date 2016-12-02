package com.huangxueqin.surge.Surge.cache;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.LruCache;

import com.huangxueqin.surge.Surge.Utils.BitmapUtils;
import com.huangxueqin.surge.Surge.Utils.Logger;
import com.huangxueqin.surge.Surge.Utils.Size;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Created by huangxueqin on 16/11/12.
 */

public class SurgeCache {
    private static HashMap<String, String> urlToFileNameMap = new HashMap<>();

    private LruCache<String, Bitmap> memCache;
    private SurgeDiskCache diskCache;

    public SurgeCache(Context context, String diskCacheDirName, long diskCacheSize) {
        // init memCache
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory()/1024);
        this.memCache = new LruCache<String, Bitmap>(maxMemory/8) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount()/1024;
            }
        };

        // init diskCache
        File sysCacheDir = getApplicationCacheDirectory(context);
        File diskCacheDir = new File(sysCacheDir, diskCacheDirName);
        try {
            diskCache = SurgeDiskCache.open(diskCacheDir, diskCacheSize);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.D("can not initialize disk cache");
        }
    }

    private static File getApplicationCacheDirectory(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return context.getApplicationContext().getExternalCacheDir();
        } else {
            return context.getApplicationContext().getCacheDir();
        }
    }

    private static String cacheFileNameForURL(String url) {
        String name = urlToFileNameMap.get(url);
        if (name == null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(url.getBytes());
                StringBuffer result = new StringBuffer();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) {
                        result.append('0');
                    }
                    result.append(hex);
                }
                name = result.toString();
                urlToFileNameMap.put(url, name);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return name;
    }

    private boolean fitSize(Bitmap image, Size requiredSize) {
        if (requiredSize == null) {
            return true;
        } else {
            return requiredSize.width <= image.getWidth() && requiredSize.height <= image.getHeight();
        }
    }

    public void close() {
        try {
            diskCache.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        memCache.evictAll();
    }

    public Bitmap retrieveImage(String url) {
        return retrieveImage(url, null);
    }

    public Bitmap retrieveImage(final String url, final Size preferSize) {
        Bitmap image = memCache.get(url);
        if (image != null && fitSize(image, preferSize)) {
            return image;
        }
        if (diskCache == null) {
            // no disk cache...
            return image;
        }
        String diskFileName = cacheFileNameForURL(url);
        InputStream is = null;
        try {
            is = diskCache.getInputStream(diskFileName);
            image = BitmapUtils.decodeBitmapFromStream(is, preferSize);
            if (image != null) {
                memCache.put(url, image);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                diskCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return image;
    }

    public boolean storeImage(final String url, final InputStream is) {
        return storeImage(url, is, null);
    }

    /**
     *
     * @param url
     * @param is
     * @param preferSize
     * @return true if store success
     */
    public boolean storeImage(final String url, final InputStream is, Size preferSize) {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            Bitmap image = BitmapUtils.decodeBitmapFromStream(is, preferSize);
            if (image == null) {
                return false;
            }
            memCache.put(url, image);
            // save bitmap to disk
            if (diskCache != null) {
                String diskFileName = cacheFileNameForURL(url);
                SurgeDiskCache.Editor editor = null;
                try {
                    editor = diskCache.edit(diskFileName);
                    if (editor == null) {
                        return false;
                    }
                    BufferedOutputStream bos = new BufferedOutputStream(editor.newOutputStream());
                    image.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    bos.close();
                    editor.commit();
                    return true;
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
                        diskCache.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
