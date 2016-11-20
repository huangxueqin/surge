package com.huangxueqin.surge.Surge.Utils;

import android.util.Log;

/**
 * Created by huangxueqin on 16/11/20.
 */

public class Logger {
    private static final String TAG = "Surge Log";

    public static void D(Class<?> clazz,  String msg) {
        Log.d(clazz.getSimpleName(), msg);
    }

    public static void D(String msg) {
        Log.d(TAG, msg);
    }
}
