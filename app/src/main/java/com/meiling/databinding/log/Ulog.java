package com.meiling.databinding.log;

import android.util.Log;

public class Ulog {
    private static final String TAG = "AndroidRuntime";

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable throwable) {
        Log.e(TAG, msg, throwable);
    }

    public static void w(String msg) {
        Log.w(TAG, msg);
    }

    public static void w(String msg, Throwable throwable) {
        Log.w(TAG, msg, throwable);
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
    }

    public static void i(String msg, Throwable throwable) {
        Log.i(TAG, msg, throwable);
    }
}
