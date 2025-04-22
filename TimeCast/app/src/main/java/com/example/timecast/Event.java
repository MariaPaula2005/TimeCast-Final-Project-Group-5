package com.example.timecast;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Event implements Serializable {
    public String id;
    public String title;
    public String description;
    public Date date;
    public Date startTime;
    public Date endTime;
    public String type;
    public String location;

    public Event(String title, String description, Date date, Date startTime, Date endTime, String type, String location) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.location = location;
    }

    public String getFormattedTimeRange() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(startTime) + " - " + timeFormat.format(endTime);
    }

    public int getDurationMinutes() {
        return (int)((endTime.getTime() - startTime.getTime()) / (60 * 1000));
    }

    public int getStartPositionMinutes() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startTime);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;

    }

    public void setEndTime(Date endTime) {
        this.endTime =endTime;
    }

    public void setLocation(String location) {
        this.location =location;
    }

    public String getId() {
        return id;
    }

}