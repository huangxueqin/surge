package com.huangxueqin.surge.Surge.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Created by huangxueqin on 16/11/20.
 */

public class BitmapUtils {

    private static int computeInSampleSize(BitmapFactory.Options ops, Size preferSize) {
        int inSampleSize = 1;
        final int width = ops.outWidth;
        final int height = ops.outHeight;
        if (width > preferSize.width && height > preferSize.height) {
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;
            while (halfWidth/inSampleSize >= preferSize.width && halfHeight/inSampleSize >= preferSize.height) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromStream(InputStream is, Size preferSize) {
        BufferedInputStream bis = new BufferedInputStream(is);
        if (preferSize == null) {
            return BitmapFactory.decodeStream(bis);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(bis, null, options);
        options.inSampleSize = computeInSampleSize(options, preferSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeStream(bis, null, options);
    }
}
