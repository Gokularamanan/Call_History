package com.call.history.callhistory;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = Utils.TAG_APP + MyApplication.class.getSimpleName();
    private static MyApplication sInstance;

    private static boolean canActivityAct;
    public static String rejectNumber;
    public static String statusText = "XL Connection:Fail,Must click";

    public static MyApplication getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // Register to be notified of activity state changes
        registerActivityLifecycleCallbacks(this);
        if (activity instanceof MainActivity) {
            canActivityAct = true;
            Log.d(TAG, "onActivityCreated");
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (activity instanceof MainActivity) {
            canActivityAct = true;
            Log.d(TAG, "onActivityStarted");
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (activity instanceof MainActivity) {
            canActivityAct = true;
            Log.d(TAG, "onActivityResumed");
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity instanceof MainActivity) {
            canActivityAct = true;
            Log.d(TAG, "onActivityPaused");
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity instanceof MainActivity) {
            canActivityAct = true;
            Log.d(TAG, "onActivityStopped");
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity instanceof MainActivity) {
            canActivityAct = false;
            Log.d(TAG, "onActivityDestroyed");
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    public static boolean canActivityAct() {
        return canActivityAct;
    }

    public static void setCanActivityAct(boolean canActivityAct) {
        MyApplication.canActivityAct = canActivityAct;
    }
}
