package com.huangxueqin.surge.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Created by huangxueqin on 16/11/20.
 */

public class BitmapUtils {

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

    public static Bitmap decodeBitmapFromStream(InputStream is, Point preferSize) {
        BufferedInputStream bis = new BufferedInputStream(is);
        if (preferSize == null || preferSize.equals(0, 0)) {
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
