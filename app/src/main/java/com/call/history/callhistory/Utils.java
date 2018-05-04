package com.call.history.callhistory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

    public static final String TAG_APP = "CALH.";
    private static final String TAG = TAG_APP + Utils.class.getSimpleName();

    //Preferences
    private static final String PREF_XL_ID = "xl_id";
    private static final String ROLE = "device_role";
    private static final String LOG_TO_FILE = "log_to_file";

    //Firebase URL
    public static final String FB_REF_URL_TEST = "callhistory-e2d3e";
    public static final String FB_REF_URL = "callhistory-783df";

    public static final int DEVICE_ROLE_UNKNOWN = 101;
    public static final int DEVICE_ROLE_RECEIVE = 102;
    public static final int DEVICE_ROLE_MAKE = 103;


    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date resultdate = new Date(System.currentTimeMillis());
        return sdf.format(resultdate);
    }

    public static void disconnectCall(Context context) {
        try {
            String serviceManagerName = "android.os.ServiceManager";
            String serviceManagerNativeName = "android.os.ServiceManagerNative";
            String telephonyName = "com.android.internal.telephony.ITelephony";
            Class<?> telephonyClass;
            Class<?> telephonyStubClass;
            Class<?> serviceManagerClass;
            Class<?> serviceManagerNativeClass;
            Method telephonyEndCall;
            Object telephonyObject;
            Object serviceManagerObject;
            telephonyClass = Class.forName(telephonyName);
            telephonyStubClass = telephonyClass.getClasses()[0];
            serviceManagerClass = Class.forName(serviceManagerName);
            serviceManagerNativeClass = Class.forName(serviceManagerNativeName);
            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);
            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod("asInterface", IBinder.class);
            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");
            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface", IBinder.class);
            telephonyObject = serviceMethod.invoke(null, retbinder);
            telephonyEndCall = telephonyClass.getMethod("endCall");
            telephonyEndCall.invoke(telephonyObject);

        } catch (Exception e) {
            e.printStackTrace();
            Utils.appendLog("unable", "msg cant dissconect call....");
            Toast.makeText(context, "Cannot disconnect call", Toast.LENGTH_LONG);

        }
    }

    public static String getPrefRejectNumber(Context ctx) {
        return MyApplication.rejectNumber;
    }

    public static String getStatusText() {
        return MyApplication.statusText;
    }

    public static String getPrefXlId(Context ctx) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return pref.getString(PREF_XL_ID, null);
    }

    public static void setStatusText(String text) {
        MyApplication.statusText = text;
    }

    public static void setPrefRejectNumber(Context ctx, String number) {
        MyApplication.rejectNumber = number;
    }

    public static void setPrefXlId(Context ctx, String xlsID) {
        final SharedPreferences pref = android.preference.PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor edit = pref.edit();
        edit.putString(PREF_XL_ID, xlsID);
        edit.commit();
    }

    public static int getRole(Context context) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getInt(ROLE, DEVICE_ROLE_UNKNOWN);
    }

    public static void setRole(Context context, int role) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(ROLE, role).commit();
    }


    public static boolean getIsLogToFile() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        return pref.getBoolean(LOG_TO_FILE, false);
    }

    public static void setIsLogToFile(boolean logToFile) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getInstance());
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(LOG_TO_FILE, logToFile).commit();
    }

    public static void launchActivity(Context context,
                                      Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            context.startActivity(i);
        } catch (Exception e) {
            Utils.appendLog(TAG, "Not able to start activity" + e.toString());

        }
    }

    public static void appendLog(String file, String text)
    {
        Log.d(file, text);
        if (Utils.getIsLogToFile()) {
            File logFile = new File("sdcard/log.file");
            if (!logFile.exists())
            {
                try
                {
                    logFile.createNewFile();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try
            {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                long yourmilliseconds = System.currentTimeMillis();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");

                Date resultdate = new Date(yourmilliseconds);
                buf.append(sdf.format(resultdate) + ":" + file + ":" + text);
                buf.newLine();
                buf.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}