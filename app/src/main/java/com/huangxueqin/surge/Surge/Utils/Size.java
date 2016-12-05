package com.huangxueqin.surge.Surge.Utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

/**
 * Created by huangxueqin on 2016/11/30.
 */

public class Size {
    public final int width;
    public final int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Size(View target) {
        this.width = target.getWidth();
        this.height = target.getHeight();
    }

    public boolean fit(@NonNull Size sz) {
        return width >= sz.width && height >= sz.height;
    }

    public static boolean match(Size lhs, Size rhs) {
        if (lhs == null) {
            return rhs == null;
        }
        return lhs.equals(rhs);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (o instanceof Size) {
            Size sz = (Size) o;
            return sz.width == width && sz.height == height;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // assuming most sizes are <2^16, doing a rotate will give us perfect hashing
        return height ^ ((width << (Integer.SIZE / 2)) | (width >>> (Integer.SIZE / 2)));
    }
}
