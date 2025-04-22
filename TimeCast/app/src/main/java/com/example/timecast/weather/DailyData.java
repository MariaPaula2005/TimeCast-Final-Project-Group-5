package com.example.timecast.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DailyData {
    private List<String> time;
    
    private List<Integer> weathercode;
    
    @SerializedName("temperature_2m_max")
    private List<Double> temperature2mMax;
    
    @SerializedName("temperature_2m_min")
    private List<Double> temperature2mMin;
    
    @SerializedName("precipitation_probability_max")
    private List<Integer> precipitationProbabilityMax;

    public List<String> getTime() {
        return time;
    }

    public List<Integer> getWeathercode() {
        return weathercode;
    }

    public List<Double> getTemperature2mMax() {
        return temperature2mMax;
    }

    public List<Double> getTemperature2mMin() {
        return temperature2mMin;
    }

    public List<Integer> getPrecipitationProbabilityMax() {
        return precipitationProbabilityMax;
    }
} 