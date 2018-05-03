package com.call.history.callhistory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.call.history.callhistory.database.DataHelper;

public class AppReceiver extends BroadcastReceiver {
    private static final String TAG = Utils.TAG_APP + AppReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Utils.getRole(context) == Utils.DEVICE_ROLE_RECEIVE && intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, action);
            if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                int state = 0;
                if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    state = TelephonyManager.CALL_STATE_IDLE;
                    if (!MyApplication.canActivityAct()) {
                        String numFromPref = Utils.getPrefRejectNumber(context);
                        Utils.setPrefRejectNumber(context, null);
                        if (numFromPref != null) {
                            Log.d(TAG, "Write to DB");
                            DataHelper helper = new DataHelper(context);
                            helper.writeNumber(number);
                        } else {
                            Log.e(TAG, "Should be outgoing call disconnect");
                        }
                    } else {
                        Log.d(TAG, "End- Activity could have taken care");
                    }
                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    state = TelephonyManager.CALL_STATE_OFFHOOK;
                } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    state = TelephonyManager.CALL_STATE_RINGING;
                    if (!MyApplication.canActivityAct()) {
                        Log.d(TAG, "Disconnect call in receiver");
                        Utils.setPrefRejectNumber(context, number);
                        Utils.disconnectCall(context);
                    } else {
                        Log.d(TAG, "Ring- Activity could have taken care");
                    }
                }
                Log.d(TAG, "Call state:" + String.valueOf(state) + " number:" + number);
            }
        }
    }
}
