package com.catfish.superdex.utils;

import android.util.Log;

public class LogUtil {

    private static final boolean DEBUG = true;
    public static final String TAG = "catfish";

    public static void d(String loginfo) {
        if (DEBUG) {
            Log.d(TAG, loginfo);
        }
    }

    public static void d(String loginfo, Exception e) {
        if (DEBUG) {
            Log.d(TAG, loginfo, e);
        }
    }

    public static void i(String loginfo) {
        if (DEBUG) {
            Log.i(TAG, loginfo);
        }
    }

    public static void i(String loginfo, Exception e) {
        if (DEBUG) {
            Log.i(TAG, loginfo, e);
        }
    }
}
