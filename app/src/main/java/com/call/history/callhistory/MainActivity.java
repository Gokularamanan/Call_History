package com.call.history.callhistory;

import com.call.history.callhistory.database.DataHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.sheets.v4.SheetsScopes;

import com.google.api.services.sheets.v4.model.*;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {
    protected MyApplication nMyApplication;
    private static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 1001;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1002;
    private static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1003;
    private static final String TAG = Utils.TAG_APP + MainActivity.class.getSimpleName();
    GoogleAccountCredential mCredential;
    private Button mCallApiButton;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "XL Connection:";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {SheetsScopes.SPREADSHEETS};

    //String spreadsheetId = "1W3HSA1-JHTHcl_2wOdyymssYQgS6_E2EAzh--GnvchQ"; //Using
    String spreadsheetId = "1datVmtPuADPN4kwaEx22bkLliTGCVEF0cqeO4X3t2fE"; //TEST
    String range = "Sheet1!A2:E";
    Thread thread;
    boolean isVisible;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nMyApplication = (MyApplication)getApplication();
        nMyApplication.onActivityCreated(this, savedInstanceState);
        isVisible = true;
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Request permission: READ_PHONE_STATE");
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
            return;
        }

        if (checkSelfPermission(Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Request permission: CALL_PHONE");
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
            return;
        }

        if (checkSelfPermission(Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Request permission: GET_ACCOUNTS");
            requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
            return;
        }

        IntentFilter rejectCall = new IntentFilter();
        rejectCall.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(rejectCallReceiver, rejectCall);

        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 300, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mCallApiButton = new Button(this);
        mCallApiButton.setText(BUTTON_TEXT + "...");
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton);
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                DataHelper helper = new DataHelper(getApplicationContext());
                int count = helper.getPendingNumber().size();
                if (isVisible) {
                    if (count >0) {
                        mCallApiButton.setText(BUTTON_TEXT + "Fail. Must click here");
                    } else {
                        mCallApiButton.setText(BUTTON_TEXT + "Success");
                    }
                }
            }
        });
        thread.start();

        setContentView(activityLayout);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    BroadcastReceiver rejectCallReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());

            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.d(TAG, "Call state:" + stateStr + " number:" + number);

            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
                upload(context, number);
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
                Utils.disconnectCall(context);
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        nMyApplication.onActivityStarted(this);
        isVisible = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        nMyApplication.onActivityResumed(this);
        isVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        nMyApplication.onActivityPaused(this);
        isVisible = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        nMyApplication.onActivityStopped(this);
        isVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        nMyApplication.onActivityDestroyed(this);
        unregisterReceiver(rejectCallReceiver);
        isVisible = false;
    }

    private void upload(Context context, String number) {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;
        //String serial = Build.getSerial();

        DataHelper dataHelper = new DataHelper(context);
        List<Pair<String, String>> pendingNumber = dataHelper.getPendingNumber();
        if (number != null) {
            Pair pair = new Pair(number, Utils.getCurrentTime());
            pendingNumber.add(pair);
        }
        List<RowEntry> data = new ArrayList<>();

        for (Pair<String, String> pair: pendingNumber) {
            RowEntry a = new RowEntry();
            a.setNumber(pair.first);
            a.setTime(pair.second);
            a.setDetail(manufacturer + "," + model + "," + version + "," + versionRelease/* + "," + serial*/);
            data.add(a);
        }
        new MakeRequestTask(context, mCredential, data).execute();

    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        Log.d(TAG, "getResultsFromApi");
        if (!isGooglePlayServicesAvailable()) {
            Log.d(TAG, "isGooglePlayServicesAvailable");
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            Log.d(TAG, "getSelectedAccountName");
            chooseAccount();
        } else if (!isDeviceOnline()) {
            Log.d(TAG, "isDeviceOnline");
            mCallApiButton.setText("No network connection available.");
        } else {
            Log.d(TAG, "Ok to upload now");
            //TODO: Check pending data from DB and upload
            upload(this, null);
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    private void chooseAccount() {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                Log.d(TAG, "pref has account");
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                Log.d(TAG, "Dialog- Choose account");
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode  code indicating the result of the incoming
     *                    activity result.
     * @param data        Intent (containing result data) returned by incoming
     *                    activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult. requestCode:" + requestCode);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mCallApiButton.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     *
     * @param requestCode  The request code passed in
     *                     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult. requestCode:" + requestCode);
        if (requestCode == MY_PERMISSIONS_REQUEST_GET_ACCOUNTS || requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE
                || requestCode == MY_PERMISSIONS_REQUEST_CALL_PHONE) {
            recreate();
        }
    }
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     *
     * @return true if Google Play Services is available and up to
     * date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Sheets API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    public class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;
        List<RowEntry> mData = null;
        GoogleAccountCredential mCredential = null;
        Context mContext;

        MakeRequestTask(Context context, GoogleAccountCredential credential, List<RowEntry> data) {
            mContext = context;
            mData = data;
            mCredential = credential;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, mCredential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                //Read from DB if any
                return append(mContext, mData);
            } catch (Exception e) {
                MyApplication.setCanActivityAct(false);
                writeLastToDb(mContext, mData.get(mData.size()-1));
                e.printStackTrace();
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        public List<String> append(Context context, List<RowEntry> myData) {

            try {
                int rowSize = myData.size();
                List<List<Object>> writeData = new ArrayList<>();
                for (RowEntry someRowEntry : myData) {
                    List<Object> dataRow = new ArrayList<>();
                    dataRow.add(someRowEntry.number);
                    dataRow.add(someRowEntry.time);
                    dataRow.add(someRowEntry.detail);
                    writeData.add(dataRow);
                }


                ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");
                this.mService.spreadsheets().values()
                        .append(spreadsheetId, range, vr)
                        .setValueInputOption("RAW")
                        .execute();
                Log.d(TAG, "append success:" + rowSize);
                MyApplication.setCanActivityAct(true);
                DataHelper helper = new DataHelper(mContext);
                if (helper.getPendingNumber().size() > 0) {
                    helper.deleteDBEntry();
                }
                if (isVisible) {
                    mCallApiButton.setText(BUTTON_TEXT + "Success");
                }
            } catch (UserRecoverableAuthIOException e) {
                MyApplication.setCanActivityAct(false);
                writeLastToDb(context, mData.get(mData.size()-1));
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (Exception e) {
                MyApplication.setCanActivityAct(false);
                writeLastToDb(context, mData.get(mData.size()-1));
                e.printStackTrace();
                //Toast.makeText(getApplicationContext(), "Not working. Click button", Toast.LENGTH_LONG);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(List<String> output) {
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mCallApiButton.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mCallApiButton.setText("Request cancelled.");
            }
        }


        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         *
         * @return List of names and majors
         * @throws IOException
         */
        private List<String> read() throws IOException {
            List<String> results = new ArrayList<String>();
            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();
            if (values != null) {
                results.add("Name, Major");
                for (List row : values) {
                    results.add(row.get(0) + ", " + row.get(4));
                }
            }
            return results;
        }

        public List<String> update(List<RowEntry> myData) {

            try {
                List<List<Object>> writeData = new ArrayList<>();
                for (RowEntry someRowEntry : myData) {
                    List<Object> dataRow = new ArrayList<>();
                    dataRow.add(someRowEntry.number);
                    writeData.add(dataRow);
                }

                ValueRange vr = new ValueRange().setValues(writeData).setMajorDimension("ROWS");
                this.mService.spreadsheets().values()
                        .update(spreadsheetId, range, vr)
                        .setValueInputOption("RAW")
                        .execute();
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void writeLastToDb(Context context, RowEntry rowEntry) {
        DataHelper helper = new DataHelper(context);
        helper.writeNumber(rowEntry.number);
    }
}