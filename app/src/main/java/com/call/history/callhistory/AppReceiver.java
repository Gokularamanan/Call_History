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
        if (Utils.getRole(context) == Utils.DEVICE_ROLE_RECEIVE) {
            Utils.appendLog(TAG, "Device role is receice call");
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                Utils.appendLog(TAG, action);
                if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                    String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                    String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    if (MyApplication.canActivityAct()) {
                        Utils.appendLog(TAG, "Activity could have taken care. State-" + stateStr);
                        return;
                    }
                    if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        String numFromPref = Utils.getPrefRejectNumber(context);
                        Utils.setPrefRejectNumber(context, null);
                        if (numFromPref != null) {
                            Utils.appendLog(TAG, "Write to DB");
                            DataHelper helper = new DataHelper(context);
                            helper.writeNumber(number);
                        } else {
                            Utils.appendLog(TAG, "Should be outgoing call disconnect");
                        }
                    } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        Utils.appendLog(TAG, "Disconnect call in receiver");
                        Utils.setPrefRejectNumber(context, number);
                        Utils.disconnectCall(context);
                    }
                }
            }
        } else {
            Utils.appendLog(TAG, "Device role is make call. No action");
        }
    }
}
