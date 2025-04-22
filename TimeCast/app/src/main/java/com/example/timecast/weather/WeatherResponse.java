package com.example.timecast.weather;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    private double latitude;
    private double longitude;
    private String timezone;
    private HourlyData hourly;
    private DailyData daily;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTimezone() {
        return timezone;
    }

    public HourlyData getHourly() {
        return hourly;
    }

    public DailyData getDaily() {
        return daily;
    }
} 