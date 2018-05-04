package com.call.history.callhistory.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import com.call.history.callhistory.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataHelper extends SQLiteOpenHelper {

    //database version, current ver is 1.
    public static final int DATABASE_VER = 1;

    //database Name or db name
    public static final String DATABASE_NAME = "call_history";

    //table Name, table person
    public static final String TABLE_NAME = "entry";

    //table fields name,fist name,email and domain
    public static final String KEY_NUMBER = "number";
    public static final String KEY_TIME = "time";
    private static final String TAG = Utils.TAG_APP + DataHelper.class.getSimpleName();


    public DataHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VER);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        //creating string sqlTable for creating a table
        String sqlTable = "create table " + TABLE_NAME + "(" + KEY_NUMBER + " text," + KEY_TIME + " text);";
        //db.execSQL() will execute string which we provide and will create a table with given table name and fields.
        db.execSQL(sqlTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }

    public void writeNumber(String number) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_NUMBER, number);
        contentValues.put(KEY_TIME, Utils.getCurrentTime());
        long result = db.insert(TABLE_NAME,null,contentValues);
        db.close();
        Utils.appendLog(TAG, "Write number: " + number + "-" + result);
        Utils.setStatusText("XL Connection:Pending,Must click");
    }

    public void deleteDBEntry() {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public int getCount() {
        int count = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String col[] = { KEY_NUMBER };
        Cursor cur = db.query(TABLE_NAME, col, null, null, null, null, null);
        count = cur.getCount();
        return count;
    }

    public List<Pair<String, String>> getPendingNumber() {
        List<Pair<String, String>> stringList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String col[] = { KEY_NUMBER, KEY_TIME};
        Cursor cur = db.query(TABLE_NAME, col, null, null, null, null, null);
        if (cur != null && cur.getCount() > 0) {
            if (cur.moveToFirst()){
                do{
                    String number = cur.getString(cur.getColumnIndex(KEY_NUMBER));
                    String time = cur.getString(cur.getColumnIndex(KEY_TIME));
                    Pair pair = new Pair(number, time);
                    stringList.add(pair);
                    // do what ever you want here
                }while(cur.moveToNext());
            }
            cur.close();
        }
        return stringList;
    }
}