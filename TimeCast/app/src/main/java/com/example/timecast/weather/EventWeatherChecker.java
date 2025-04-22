package com.example.timecast.weather;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.timecast.R;
import com.example.timecast.MainActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Service to check events against weather forecasts and provide notifications
 * for weather that might affect outdoor activities.
 */
public class EventWeatherChecker {
    private static final String CHANNEL_ID = "weather_alerts";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm";
    private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT, Locale.US);
    
    private final Context context;
    private final WeatherRepository weatherRepository;
    
    public EventWeatherChecker(Context context) {
        this.context = context;
        this.weatherRepository = WeatherRepository.getInstance();
        createNotificationChannel();
    }
    public void checkWeatherForEvent(long eventId, String eventTitle, String eventDateTime, 
                                     boolean isOutdoor, double latitude, double longitude) {
        // Only check weather for outdoor events
        if (!isOutdoor) {
            return;
        }
        
        weatherRepository.getWeatherForecast(latitude, longitude, new WeatherRepository.WeatherCallback() {
            @Override
            public void onWeatherFetched(WeatherResponse weatherResponse) {
                try {
                    Date eventDate = SDF.parse(eventDateTime);
                    if (eventDate == null) return;
                    
                    int closestHourlyIndex = findClosestForecastIndex(weatherResponse.getHourly().getTime(), eventDate);
                    if (closestHourlyIndex == -1) return;
                    
                    int weatherCode = weatherResponse.getHourly().getWeathercode().get(closestHourlyIndex);
                    double temperature = weatherResponse.getHourly().getTemperature2m().get(closestHourlyIndex);
                    int precipProbability = weatherResponse.getHourly().getPrecipitationProbability().get(closestHourlyIndex);
                    
                    if (!WeatherUtils.isWeatherSuitableForOutdoorActivity(weatherCode, temperature, precipProbability)) {
                        sendWeatherAlert(eventId, eventTitle, weatherCode, temperature);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                System.err.println("Weather fetch error: " + errorMessage);
            }
        });
    }
    
    /**
     * Finds the index of the closest forecast time to the event date
     */
    private int findClosestForecastIndex(List<String> forecastTimes, Date eventDate) {
        long eventTimeMillis = eventDate.getTime();
        long closestDiff = Long.MAX_VALUE;
        int closestIndex = -1;
        
        for (int i = 0; i < forecastTimes.size(); i++) {
            try {
                Date forecastDate = SDF.parse(forecastTimes.get(i));
                if (forecastDate != null) {
                    long diff = Math.abs(forecastDate.getTime() - eventTimeMillis);
                    if (diff < closestDiff) {
                        closestDiff = diff;
                        closestIndex = i;
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        
        // Only return an index if it's within 3 hours (reasonable forecast accuracy)
        return (closestDiff <= TimeUnit.HOURS.toMillis(3)) ? closestIndex : -1;
    }
    
    /**
     * Sends a notification about the weather that might affect an outdoor event
     */
    private void sendWeatherAlert(long eventId, String eventTitle, int weatherCode, double temperature) {
        String weatherDesc = context.getString(WeatherUtils.getWeatherDescription(weatherCode));
        
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("EVENT_ID", eventId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) eventId, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(WeatherUtils.getWeatherIcon(weatherCode))
                .setContentTitle(context.getString(R.string.weather_alert_title))
                .setContentText(String.format(context.getString(R.string.weather_alert_outdoor_activity), eventTitle))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(String.format(context.getString(R.string.weather_forecast_for_event), 
                                eventTitle, weatherDesc, temperature)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify((int) eventId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Weather Alerts";
            String description = "Notifications for weather that might affect your scheduled events";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
} 