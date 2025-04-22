package com.example.timecast;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.timecast.weather.WeatherRepository;
import com.example.timecast.weather.WeatherResponse;
import com.example.timecast.weather.WeatherUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    
    private TextView cityNameTextView;
    private TextView currentTempTextView;
    private TextView weatherConditionTextView;
    private ShapeableImageView weatherIconImageView;
    private EditText searchCityEditText;
    
    private FusedLocationProviderClient fusedLocationClient;
    private WeatherRepository weatherRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_layout);

        initializeViews();
        showDefaultWeatherData();
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        weatherRepository = WeatherRepository.getInstance();

        setupSearchFunctionality();
        setupSettingsButton();
        setupBottomNavigation();
        requestLocationPermission();
    }
    
    private void initializeViews() {
        cityNameTextView = findViewById(R.id.city_name);
        currentTempTextView = findViewById(R.id.current_temperature);
        weatherConditionTextView = findViewById(R.id.weather_condition);
        weatherIconImageView = findViewById(R.id.current_weather_icon);
        searchCityEditText = findViewById(R.id.search_city);
    }
    
    private void setupSearchFunctionality() {
        searchCityEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchCity(searchCityEditText.getText().toString());
                return true;
            }
            return false;
        });
    }
    
    private void setupSettingsButton() {
        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Settings functionality coming soon", Toast.LENGTH_SHORT).show();
            // Would normally open settings activity here
        });
    }
    
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_weather);

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_calendar) {
                startActivity(new Intent(WeatherActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            } else if (id == R.id.navigation_weather) {
                return true;
            }
            return false;
        });
    }
    
    /**
     * Shows default weather data while waiting for API data
     */
    private void showDefaultWeatherData() {
        cityNameTextView.setText("Calgary");
        currentTempTextView.setText("-8");
        weatherConditionTextView.setText("Cloudy");
        weatherIconImageView.setImageResource(R.drawable.ic_weather_cloudy);
        
        TextView eventDateTextView = findViewById(R.id.event_date);
        TextView eventTimeTextView = findViewById(R.id.event_time);
        TextView forecastRainTextView = findViewById(R.id.forecast_rain);
        TextView forecastRainTimeTextView = findViewById(R.id.forecast_rain_time);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        
        eventDateTextView.setText(currentDate);
        eventTimeTextView.setText("4:00pm - 5:00pm");
        forecastRainTextView.setText(currentDate);
        forecastRainTimeTextView.setText("4:00pm - 7:00pm");
    }
    
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                // Use default location when permission is denied
                getWeatherForLocation("Calgary", 51.0447, -114.0719);
            }
        }
    }
    
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                getLocationName(location.getLatitude(), location.getLongitude());
                getWeatherForLocation(null, location.getLatitude(), location.getLongitude());
            } else {
                // Use default location when device location is unavailable
                getWeatherForLocation("Calgary", 51.0447, -114.0719);
            }
        });
    }
    
    private void getLocationName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String cityName = addresses.get(0).getLocality();
                if (cityName != null && !cityName.isEmpty()) {
                    cityNameTextView.setText(cityName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void searchCity(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(cityName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                double latitude = address.getLatitude();
                double longitude = address.getLongitude();
                
                String formattedCityName = address.getLocality();
                if (formattedCityName == null || formattedCityName.isEmpty()) {
                    formattedCityName = cityName;
                }
                
                getWeatherForLocation(formattedCityName, latitude, longitude);
            } else {
                Toast.makeText(this, "City not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error finding city", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void getWeatherForLocation(String cityName, double latitude, double longitude) {
        // Show loading state
        weatherConditionTextView.setText("Loading...");
        
        weatherRepository.getWeatherForecast(latitude, longitude, new WeatherRepository.WeatherCallback() {
            @Override
            public void onWeatherFetched(WeatherResponse weatherResponse) {
                runOnUiThread(() -> {
                    // Set city name if provided
                    if (cityName != null) {
                        cityNameTextView.setText(cityName);
                    }
                    
                    // Get current hour's weather data
                    int currentIndex = 0;
                    int weatherCode = weatherResponse.getHourly().getWeathercode().get(currentIndex);
                    double currentTemp = weatherResponse.getHourly().getTemperature2m().get(currentIndex);
                    
                    // Format temperature as integer with minus sign in front for negative temps
                    String tempFormatted;
                    if (currentTemp < 0) {
                        tempFormatted = String.format(Locale.getDefault(), "-%d", Math.abs((int)currentTemp));
                    } else {
                        tempFormatted = String.format(Locale.getDefault(), "%d", (int)currentTemp);
                    }
                    currentTempTextView.setText(tempFormatted);
                    
                    weatherConditionTextView.setText(getString(WeatherUtils.getWeatherDescription(weatherCode)));
                    weatherIconImageView.setImageResource(WeatherUtils.getWeatherIcon(weatherCode));
                    
                    updateWeatherAlertCard(weatherResponse);
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(WeatherActivity.this, 
                            "Error fetching weather: " + errorMessage, Toast.LENGTH_SHORT).show();
                    weatherConditionTextView.setText("Error loading weather");
                });
            }
        });
    }
    
    private void updateWeatherAlertCard(WeatherResponse weatherResponse) {
        // Example values for the weather alert card
        // In a real app, these would be based on scheduled events and forecasted weather
        
        TextView eventDateTextView = findViewById(R.id.event_date);
        TextView eventTimeTextView = findViewById(R.id.event_time);
        TextView forecastRainTextView = findViewById(R.id.forecast_rain);
        TextView forecastRainTimeTextView = findViewById(R.id.forecast_rain_time);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        
        eventDateTextView.setText(currentDate);
        forecastRainTextView.setText(currentDate);
    }
}