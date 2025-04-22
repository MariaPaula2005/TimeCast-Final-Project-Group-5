package com.example.timecast.weather;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.example.timecast.R;

public class WeatherUtils {

    /**
     * Gets a human-readable description for a given weather code.
     * Based on WMO Weather interpretation codes (WW)
     * https://open-meteo.com/en/docs
     */
    @StringRes
    public static int getWeatherDescription(int weatherCode) {
        switch (weatherCode) {
            case 0:
                return R.string.weather_clear_sky;
            case 1:
                return R.string.weather_mainly_clear;
            case 2:
                return R.string.weather_partly_cloudy;
            case 3:
                return R.string.weather_overcast;
            case 45:
            case 48:
                return R.string.weather_fog;
            case 51:
                return R.string.weather_light_drizzle;
            case 53:
                return R.string.weather_moderate_drizzle;
            case 55:
                return R.string.weather_dense_drizzle;
            case 56:
            case 57:
                return R.string.weather_freezing_drizzle;
            case 61:
                return R.string.weather_slight_rain;
            case 63:
                return R.string.weather_moderate_rain;
            case 65:
                return R.string.weather_heavy_rain;
            case 66:
            case 67:
                return R.string.weather_freezing_rain;
            case 71:
                return R.string.weather_slight_snow;
            case 73:
                return R.string.weather_moderate_snow;
            case 75:
                return R.string.weather_heavy_snow;
            case 77:
                return R.string.weather_snow_grains;
            case 80:
                return R.string.weather_slight_showers;
            case 81:
                return R.string.weather_moderate_showers;
            case 82:
                return R.string.weather_violent_showers;
            case 85:
            case 86:
                return R.string.weather_snow_showers;
            case 95:
                return R.string.weather_thunderstorm;
            case 96:
            case 99:
                return R.string.weather_thunderstorm_hail;
            default:
                return R.string.weather_unknown;
        }
    }

    /**
     * Gets a drawable resource for the weather icon based on the weather code.
     * Using our custom weather icons
     */
    @DrawableRes
    public static int getWeatherIcon(int weatherCode) {
        if (weatherCode == 0) {
            return R.drawable.ic_weather_sunny;
        } else if (weatherCode <= 2) {
            return R.drawable.ic_weather_partly_cloudy;
        } else if (weatherCode == 3) {
            return R.drawable.ic_weather_cloudy;
        } else if (weatherCode <= 48) {
            return R.drawable.ic_weather_fog;
        } else if (weatherCode <= 67) {
            return R.drawable.ic_weather_rain;
        } else if (weatherCode <= 77) {
            return R.drawable.ic_weather_snow;
        } else if (weatherCode <= 82) {
            return R.drawable.ic_weather_rain; 
        } else if (weatherCode <= 86) {
            return R.drawable.ic_weather_snow;
        } else if (weatherCode <= 99) {
            return R.drawable.ic_weather_thunderstorm;
        } else {
            return R.drawable.ic_weather_unknown;
        }
    }

    /**
     * Determines if the weather is suitable for outdoor activities
     */
    public static boolean isWeatherSuitableForOutdoorActivity(int weatherCode, double temperature, int precipitationProbability) {
        boolean badWeather = weatherCode >= 51 || 
                             temperature < 10.0 || 
                             temperature > 35.0 || 
                             precipitationProbability > 50; 
        
        return !badWeather;
    }
} 