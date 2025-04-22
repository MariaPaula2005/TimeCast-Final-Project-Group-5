package com.example.timecast.weather;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {
    @GET("forecast")
    Call<WeatherResponse> getWeatherForecast(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("hourly") String hourly,
            @Query("daily") String daily,
            @Query("timezone") String timezone
    );

    class Creator {
        private static final String BASE_URL = "https://api.open-meteo.com/v1/";

        public static WeatherService create() {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            return retrofit.create(WeatherService.class);
        }
    }
} 