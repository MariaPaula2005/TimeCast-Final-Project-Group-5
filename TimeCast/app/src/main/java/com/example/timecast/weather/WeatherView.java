package com.example.timecast.weather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.timecast.R;

/**
 * Custom view to display weather information
 */
public class WeatherView extends LinearLayout {
    private ImageView weatherIcon;
    private TextView locationText;
    private TextView descriptionText;
    private TextView temperatureText;
    private TextView minTempText;
    private TextView maxTempText;
    private TextView precipitationText;

    public WeatherView(Context context) {
        super(context);
        init(context);
    }

    public WeatherView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeatherView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_weather, this, true);
        
        weatherIcon = findViewById(R.id.weather_icon);
        locationText = findViewById(R.id.weather_location);
        descriptionText = findViewById(R.id.weather_description);
        temperatureText = findViewById(R.id.weather_temperature);
        minTempText = findViewById(R.id.weather_min_temp);
        maxTempText = findViewById(R.id.weather_max_temp);
        precipitationText = findViewById(R.id.weather_precipitation);
    }

    /**
     * Update the view with weather data
     * 
     * @param location Location name
     * @param weatherCode Weather code from API
     * @param currentTemp Current temperature
     * @param minTemp Minimum temperature
     * @param maxTemp Maximum temperature
     * @param precipProb Precipitation probability
     */
    public void updateWeather(String location, int weatherCode, double currentTemp, 
                              double minTemp, double maxTemp, int precipProb) {
        weatherIcon.setImageResource(WeatherUtils.getWeatherIcon(weatherCode));
        locationText.setText(location);
        descriptionText.setText(getContext().getString(WeatherUtils.getWeatherDescription(weatherCode)));
        temperatureText.setText(String.format("%.1f°C", currentTemp));
        minTempText.setText(String.format("Min: %.1f°C", minTemp));
        maxTempText.setText(String.format("Max: %.1f°C", maxTemp));
        precipitationText.setText(String.format("Precipitation: %d%%", precipProb));
    }
    
    /**
     * Update the view with the current location's weather
     * 
     * @param latitude Latitude
     * @param longitude Longitude
     */
    public void loadWeatherForLocation(double latitude, double longitude) {
        descriptionText.setText("Loading weather...");
        
        WeatherRepository.getInstance().getWeatherForecast(latitude, longitude, 
                new WeatherRepository.WeatherCallback() {
                    @Override
                    public void onWeatherFetched(WeatherResponse weatherResponse) {
                        int currentIndex = 0;
                        
                        int weatherCode = weatherResponse.getHourly().getWeathercode().get(currentIndex);
                        double currentTemp = weatherResponse.getHourly().getTemperature2m().get(currentIndex);
                        int precipProb = weatherResponse.getHourly().getPrecipitationProbability().get(currentIndex);
                        
                        double minTemp = weatherResponse.getDaily().getTemperature2mMin().get(0);
                        double maxTemp = weatherResponse.getDaily().getTemperature2mMax().get(0);
                        
                        String locationName = String.format("%.2f, %.2f", 
                                weatherResponse.getLatitude(), weatherResponse.getLongitude());
                        updateWeather(locationName, weatherCode, currentTemp, minTemp, maxTemp, precipProb);
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        descriptionText.setText("Error: " + errorMessage);
                    }
                });
    }
} 