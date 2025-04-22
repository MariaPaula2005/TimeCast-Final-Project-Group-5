package com.example.timecast.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class HourlyData {
    private List<String> time;
    
    @SerializedName("temperature_2m")
    private List<Double> temperature2m;
    
    private List<Integer> weathercode;
    
    @SerializedName("precipitation_probability")
    private List<Integer> precipitationProbability;

    public List<String> getTime() {
        return time;
    }

    public List<Double> getTemperature2m() {
        return temperature2m;
    }

    public List<Integer> getWeathercode() {
        return weathercode;
    }

    public List<Integer> getPrecipitationProbability() {
        return precipitationProbability;
    }
} 