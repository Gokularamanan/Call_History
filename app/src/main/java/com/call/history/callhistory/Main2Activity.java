package com.call.history.callhistory;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.common.collect.Lists;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Main2Activity extends Activity {

    private static final String TAG = Main2Activity.class.getSimpleName();
    //initialize view's
    ListView simpleListView;
    SimpleAdapter simpleAdapter;
    List<HashMap<String, String>> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        simpleListView = (ListView) findViewById(R.id.simpleListView);

        arrayList = new ArrayList<>();

        String[] from = {"number", "time"};//string array
        int[] to = {R.id.textnumber, R.id.textTime};//int array of views id's
        simpleAdapter = new SimpleAdapter(this, arrayList, R.layout.list_view_items, from, to);//Create object and set the parameters for simpleAdapter
        simpleListView.setAdapter(simpleAdapter);//sets the adapter for listView

        //perform listView item click event
        simpleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //Toast.makeText(getApplicationContext(), arrayList.get(i).get("number"), Toast.LENGTH_SHORT).show();//show the selected image in toast according to position
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + arrayList.get(i).get("number")));
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Enable call permission", Toast.LENGTH_LONG).show();
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivity(callIntent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.getReference(Utils.FB_REF_URL).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        arrayList.clear();

                        Log.i(TAG, "getUser:onCancelled " + dataSnapshot.toString());
                        Log.i(TAG, "count = " + String.valueOf(dataSnapshot.getChildrenCount()) + " values " + dataSnapshot.getKey());
                        //List<HashMap<String, String>> tempList = new ArrayList<>();
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            RowEntry entry = data.getValue(RowEntry.class);
                            //arrayList.add(todo);
                            HashMap<String, String> hashMap = new HashMap<>();//create a hashmap to store the data in key value pair
                            hashMap.put("number", entry.number);
                            hashMap.put("time", entry.time);
                            arrayList.add(hashMap);//add the hashmap into arrayList
                        }
                        Collections.reverse(arrayList);
                        simpleAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getUser:onCancelled", databaseError.toException());
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_read, menu);
        menu.getItem(1).setChecked(Utils.getIsLogToFile());
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.recreateMenu:
                recreate();
                return true;
            case R.id.log_to_file:
                item.setChecked(!item.isChecked());
                Utils.setIsLogToFile(item.isChecked());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
