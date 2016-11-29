package com.huangxueqin.surge.Surge.Utils;

/**
 * Created by huangxueqin on 2016/11/29.
 */

public abstract class Singleton<T> {
    private T mInstance;
    protected abstract T create();

    public final T get() {
        synchronized (this) {
            if (mInstance == null) {
                mInstance = create();
            }
            return mInstance;
        }
    }
}
