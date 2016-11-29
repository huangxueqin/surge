package com.huangxueqin.surge.Surge.lifecycle;

import android.app.Activity;
import android.app.Fragment;

/**
 * Created by huangxueqin on 2016/11/29.
 */

public interface LifecycleListener {
    /**
     * Callback for when {@link Fragment#onStart()} or {@link Activity#onStart()}
     * is called
     */
    void onStart();

    /**
     * Callback for when {@link Fragment#onStop()} or {@link Activity#onStop()}
     * is called
     */
    void onStop();

    /**
     * Callback for when {@link Fragment#onDestroy()} or {@link Activity#onDestroy()}
     * is called
     */
    void onDestroy();
}
