package com.call.history.callhistory;

import java.io.Serializable;
import java.util.HashMap;

public class Entry implements Serializable {

    private String number;
    private String time;
    private String detail;

    public Entry() {

    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }


    public HashMap<String, String> toFirebaseObject() {
        HashMap<String, String> entry = new HashMap<String, String>();
        entry.put("number", number);
        entry.put("time", time);
        entry.put("detail", detail);

        return entry;
    }

}
