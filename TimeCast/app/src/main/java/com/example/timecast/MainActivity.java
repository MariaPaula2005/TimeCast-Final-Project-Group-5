package com.example.timecast;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity  implements MonthViewFragment.OnMonthChangeListener,WeekViewFragment.OnWeekChangeListener  {

    private Button addActivityButton;
    private LinearLayout timeLabels;
    private FrameLayout eventsContainer;
    private ArrayList<Event> eventList = new ArrayList<>();
    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private TextView dateTextView;
    private Calendar currentDate = Calendar.getInstance();
    private MonthViewFragment monthViewFragment;
    private WeekViewFragment weekViewFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupNavigation();
        loadEventsForDate();
        updateDateDisplay();
    }

    private void initializeViews() {
        addActivityButton = findViewById(R.id.addActivity);
        timeLabels = findViewById(R.id.timelineContainer);
        eventsContainer = findViewById(R.id.eventsContainer);
        dateTextView = findViewById(R.id.date);
        prefs = getSharedPreferences("TimeCastPrefs", MODE_PRIVATE);

        monthViewFragment = new MonthViewFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.calendar_fragment_container, monthViewFragment)
                .commit();

        findViewById(R.id.arrow_left).setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.calendar_fragment_container);

            if (isDayViewVisible()) {
                currentDate.add(Calendar.DAY_OF_YEAR, -1);
                updateDateDisplay();
                loadEventsForDate();
            } else if (currentFragment instanceof MonthViewFragment) {
                ((MonthViewFragment) currentFragment).previousMonthAction();
            } else if (currentFragment instanceof WeekViewFragment) {
                Log.d("WEEK_ARROW", "Calling navigateWeek(-1)");
                ((WeekViewFragment) currentFragment).navigateWeek(-1);
            } else {
                Log.d("WEEK_ARROW", "Current fragment is not week view: " + currentFragment);
            }
        });

        findViewById(R.id.arrow_right).setOnClickListener(v -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.calendar_fragment_container);

            if (isDayViewVisible()) {
                currentDate.add(Calendar.DAY_OF_YEAR, 1);
                updateDateDisplay();
                loadEventsForDate();
            } else if (currentFragment instanceof MonthViewFragment) {
                ((MonthViewFragment) currentFragment).nextMonthAction();
            } else if (currentFragment instanceof WeekViewFragment) {
                Log.d("WEEK_ARROW", "Calling navigateWeek(+1)");
                ((WeekViewFragment) currentFragment).navigateWeek(1);
            } else {
                Log.d("WEEK_ARROW", "Current fragment is not week view: " + currentFragment);
            }
        });

        addActivityButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreationActivity.class);
            startActivityForResult(intent, 100);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        ImageView menuIcon = findViewById(R.id.viewMenuIcon);
        menuIcon.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, menuIcon);
            popupMenu.getMenuInflater().inflate(R.menu.view_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.day_view) {
                    updateDateDisplay();
                    findViewById(R.id.timelineContainer).setVisibility(View.VISIBLE);
                    findViewById(R.id.eventsContainer).setVisibility(View.VISIBLE);
                    findViewById(R.id.calendar_fragment_container).setVisibility(View.GONE);
                    return true;
                } else if (id == R.id.month_view) {
                    monthViewFragment = new MonthViewFragment();
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.calendar_fragment_container, monthViewFragment)
                            .commit();

                    findViewById(R.id.timelineContainer).setVisibility(View.GONE);
                    findViewById(R.id.eventsContainer).setVisibility(View.GONE);
                    findViewById(R.id.calendar_fragment_container).setVisibility(View.VISIBLE);
                    return true;
                }
                else if (id == R.id.week_view) {

                    findViewById(R.id.timelineContainer).setVisibility(View.GONE);
                    findViewById(R.id.eventsContainer).setVisibility(View.GONE);
                    findViewById(R.id.calendar_fragment_container).setVisibility(View.VISIBLE);

                    weekViewFragment = new WeekViewFragment();
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.calendar_fragment_container, weekViewFragment)
                            .commit();

                    return true;
                }
                return false;
            });
            popupMenu.show();
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
        if (dateTextView != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
            String formatted = dateFormat.format(currentDate.getTime());
            Log.d("DATE_DEBUG", "Setting date to: " + formatted);
            dateTextView.setText(formatted);
        } else {
            Log.d("DATE_DEBUG", "dateTextView is null");
        }
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

            eventView.setOnLongClickListener(v -> {
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(null, shadowBuilder, event, 0);
                return true;
            });

            // 🔍 On click to view details
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

        eventsContainer.setOnDragListener((view, dragEvent) -> {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DROP:
                    float dropY = dragEvent.getY();
                    int minutesFromTop = (int) (dropY / getResources().getDisplayMetrics().density);
                    int newStartMinutes = minutesFromTop + 8 * 60;

                    Event draggedEvent = (Event) dragEvent.getLocalState();
                    Calendar newStart = Calendar.getInstance();
                    newStart.setTime(draggedEvent.date);
                    newStart.set(Calendar.HOUR_OF_DAY, newStartMinutes / 60);
                    newStart.set(Calendar.MINUTE, newStartMinutes % 60);

                    Calendar newEnd = (Calendar) newStart.clone();
                    long durationMillis = draggedEvent.endTime.getTime() - draggedEvent.startTime.getTime();
                    newEnd.setTimeInMillis(newStart.getTimeInMillis() + durationMillis);

                    // Update event times
                    draggedEvent.startTime = newStart.getTime();
                    draggedEvent.endTime = newEnd.getTime();

                    saveUpdatedEvent(draggedEvent);
                    loadEventsForDate(); // Refresh UI
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    return true;

                default:
                    return false;
            }
        });
    }

    private void saveUpdatedEvent(Event updatedEvent) {
        String json = prefs.getString("event_data", null);
        Type type = new TypeToken<ArrayList<Event>>() {}.getType();
        ArrayList<Event> allEvents = (json != null) ? gson.fromJson(json, type) : new ArrayList<>();

        for (int i = 0; i < allEvents.size(); i++) {
            Event e = allEvents.get(i);
            if (e.title.equals(updatedEvent.title) && e.date.equals(updatedEvent.date)) {
                allEvents.set(i, updatedEvent);
                break;
            }
        }

        prefs.edit().putString("event_data", gson.toJson(allEvents)).apply();
    }


    private int minutesToPixels(int minutes) {
        float dpPerMinute = 1f;
        return (int) (minutes * dpPerMinute * getResources().getDisplayMetrics().density);
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);

    }
    @Override
    public void onMonthChanged(String newMonthYear) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.calendar_fragment_container);

        if (currentFragment instanceof MonthViewFragment
                && findViewById(R.id.calendar_fragment_container).getVisibility() == View.VISIBLE
                && findViewById(R.id.timelineContainer).getVisibility() == View.GONE) {

            TextView dateTextView = findViewById(R.id.date);
            dateTextView.setText(newMonthYear);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.calendar_fragment_container, new MonthViewFragment())
                    .commit();
        }
    }

    private boolean isDayViewVisible() {
        View calendarFragmentContainer = findViewById(R.id.calendar_fragment_container);
        View timelineContainer = findViewById(R.id.timelineContainer);

        return timelineContainer.getVisibility() == View.VISIBLE
                && calendarFragmentContainer.getVisibility() == View.GONE;
    }

    @Override
    public void onWeekChanged(String weekRange) {
        if (findViewById(R.id.calendar_fragment_container).getVisibility() == View.VISIBLE
                && findViewById(R.id.timelineContainer).getVisibility() == View.GONE) {
            TextView dateTextView = findViewById(R.id.date);
            dateTextView.setText(weekRange);
        }
    }
}