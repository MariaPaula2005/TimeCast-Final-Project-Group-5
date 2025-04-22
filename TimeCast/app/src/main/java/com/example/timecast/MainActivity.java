package com.example.timecast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button addActivityButton;
    private LinearLayout timeLabels;
    private FrameLayout eventsContainer;
    private ArrayList<Event> eventList = new ArrayList<>();
    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private TextView dateTextView;
    private Calendar currentDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupNavigation();
        loadEventsForDate();
    }

    private void initializeViews() {
        addActivityButton = findViewById(R.id.addActivity);
        timeLabels = findViewById(R.id.timelineContainer);
        eventsContainer = findViewById(R.id.eventsContainer);
        dateTextView = findViewById(R.id.date);
        prefs = getSharedPreferences("TimeCastPrefs", MODE_PRIVATE);

        updateDateDisplay();

        findViewById(R.id.arrow_left).setOnClickListener(v -> {
            currentDate.add(Calendar.DAY_OF_YEAR, -1);
            updateDateDisplay();
            loadEventsForDate();
        });

        findViewById(R.id.arrow_right).setOnClickListener(v -> {
            currentDate.add(Calendar.DAY_OF_YEAR, 1);
            updateDateDisplay();
            loadEventsForDate();
        });

        addActivityButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreationActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_calendar) {
                return true;
            } else if (id == R.id.navigation_weather) {
                startActivity(new Intent(MainActivity.this, WeatherActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventsForDate();
    }

    private void updateDateDisplay() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        dateTextView.setText(dateFormat.format(currentDate.getTime()));
    }

    private void loadEventsForDate() {
        loadEvents();
        displayTimeline();
    }

    private void loadEvents() {
        eventList.clear();
        String json = prefs.getString("event_data", null);

        if (json != null) {
            Type type = new TypeToken<ArrayList<Event>>() {}.getType();
            ArrayList<Event> allEvents = gson.fromJson(json, type);

            if (allEvents != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDateStr = dateFormat.format(currentDate.getTime());

                for (Event event : allEvents) {
                    String eventDateStr = dateFormat.format(event.date);
                    if (currentDateStr.equals(eventDateStr)) {
                        eventList.add(event);
                    }
                }
            }
        }
    }


    public void saveEvent(Event newEvent) {
        String json = prefs.getString("event_data", null);
        Type type = new TypeToken<ArrayList<Event>>() {}.getType();
        ArrayList<Event> allEvents = (json != null) ? gson.fromJson(json, type) : new ArrayList<>();

        allEvents.add(newEvent);
        prefs.edit().putString("event_data", gson.toJson(allEvents)).apply();
    }


    private void displayTimeline() {
        timeLabels.removeAllViews();
        eventsContainer.removeAllViews();

        int startTimeMinutes = 8 * 60;
        int endTimeMinutes = 20 * 60;
        int totalMinutes = endTimeMinutes - startTimeMinutes;


        for (int minutes = startTimeMinutes; minutes <= endTimeMinutes; minutes += 30) {
            int hour = minutes / 60;
            int minute = minutes % 60;
            addTimeLabel(hour, minute);
        }

        for (Event event : eventList) {
            addEventToTimeline(event);
        }
    }


    private void addTimeLabel(int hour, int minute) {
        TextView timeLabel = new TextView(this);
        timeLabel.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        timeLabel.setTextSize(12);
        timeLabel.setTextColor(Color.GRAY);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                minutesToPixels(30)
        );
        timeLabel.setLayoutParams(params);
        timeLabels.addView(timeLabel);
    }


    private void addEventToTimeline(Event event) {
        try {
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(event.startTime);
            int startHour = startCal.get(Calendar.HOUR_OF_DAY);
            int startMinute = startCal.get(Calendar.MINUTE);
            int startMinutesSinceMidnight = startHour * 60 + startMinute;

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(event.endTime);
            int endHour = endCal.get(Calendar.HOUR_OF_DAY);
            int endMinute = endCal.get(Calendar.MINUTE);
            int endMinutesSinceMidnight = endHour * 60 + endMinute;

            int timelineStartMinutes = 8 * 60;

            int positionInTimeline = startMinutesSinceMidnight - timelineStartMinutes;
            int durationMinutes = endMinutesSinceMidnight - startMinutesSinceMidnight;

            if (positionInTimeline < 0 || positionInTimeline > 720) return;
            if (positionInTimeline + durationMinutes > 720) {
                durationMinutes = 720 - positionInTimeline;
            }
            if (durationMinutes <= 0) return;

            TextView eventView = new TextView(this);
            eventView.setText(String.format("%s\n%s\n%s", event.title, event.getFormattedTimeRange(), event.type));

            eventView.setBackgroundResource(R.drawable.activity_background);
            eventView.setTextColor(Color.WHITE);
            eventView.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            eventView.setGravity(Gravity.TOP);

            eventView.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, EventDetailsActivity.class);
                intent.putExtra("event", event);

                startActivity(intent);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    minutesToPixels(durationMinutes)
            );
            params.setMargins(dpToPx(4), minutesToPixels(positionInTimeline), dpToPx(4), 0);
            eventsContainer.addView(eventView, params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private int minutesToPixels(int minutes) {
        float dpPerMinute = 0.5f;
        return (int) (minutes * dpPerMinute * getResources().getDisplayMetrics().density);
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
