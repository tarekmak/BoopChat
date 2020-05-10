package com.example.multilingualchatapp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

//class used to represent the Contacts entity
public class Messages {
    String date, body, from, time, type, id;

    public Messages() {

    }

    public Messages(String date, String body, String from, String time, String type, String id) {
        this.date = date;
        this.body = body;
        this.from = from;
        this.time = time;
        this.type = type;
        this.id = id;
    }

    public String getfrom() {
        return from;
    }

    public void setfrom(String from) {
        this.from = from;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

//    public String getMessageId() {
//        return messageId;
//    }
//
//    public void setMessageId(String type) {
//        this.messageId = messageId;
//    }

    public boolean wasSentToday() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");

        String currentDate = dateFormat.format(Calendar.getInstance().getTime());

        if (currentDate.equals(date)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        return (this.getId()).equals(((Messages) o).getId());
    }
}
