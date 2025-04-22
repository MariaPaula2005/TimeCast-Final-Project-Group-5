package com.example.timecast.weather;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {
    private static WeatherRepository instance;
    private final WeatherService weatherService;

    private WeatherRepository() {
        weatherService = WeatherService.Creator.create();
    }

    public static synchronized WeatherRepository getInstance() {
        if (instance == null) {
            instance = new WeatherRepository();
        }
        return instance;
    }

    public void getWeatherForecast(double latitude, double longitude, final WeatherCallback callback) {
        String hourlyParams = "temperature_2m,weathercode,precipitation_probability";
        String dailyParams = "weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max";
        String timezone = "auto";

        weatherService.getWeatherForecast(latitude, longitude, hourlyParams, dailyParams, timezone)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onWeatherFetched(response.body());
                        } else {
                            callback.onError("Failed to fetch weather data");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                        callback.onError(t.getMessage());
                    }
                });
    }

    public interface WeatherCallback {
        void onWeatherFetched(WeatherResponse weatherResponse);
        void onError(String errorMessage);
    }
} 