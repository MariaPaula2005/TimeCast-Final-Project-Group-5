package com.example.timecast;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class EventDetailsActivity extends AppCompatActivity {
    private EditText inputTitle, inputDescription, inputDate, inputStartTime, inputEndTime, inputLocation;
    private Button btnSave, btnDelete, btnCancel;
    private Event event;
    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private final String PREF_KEY = "event_data";
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_details_activity);

        initializeViews();
        loadEventFromIntent();
        setupButtonListeners();
    }

    private void initializeViews() {
        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputDate = findViewById(R.id.inputDate);
        inputStartTime = findViewById(R.id.inputStartTime);
        inputEndTime = findViewById(R.id.inputEndTime);
        inputLocation = findViewById(R.id.inputLocation);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.buttonDelete);
        btnCancel = findViewById(R.id.buttonCancel);
        prefs = getSharedPreferences("TimeCastPrefs", MODE_PRIVATE);
    }

    private void loadEventFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("event")) {
            event = (Event) intent.getSerializableExtra("event");
            if (event != null) {
                populateEventFields();
            } else {
                showErrorAndFinish("Invalid event data");
            }
        } else {
            showErrorAndFinish("No event data received");
        }
    }

    private void populateEventFields() {
        inputTitle.setText(event.title);
        inputDescription.setText(event.description);
        inputDate.setText(dateFormat.format(event.date));
        inputStartTime.setText(timeFormat.format(event.startTime));
        inputEndTime.setText(timeFormat.format(event.endTime));
        inputLocation.setText(event.location);
    }

    private void setupButtonListeners() {
        btnSave.setOnClickListener(v -> saveEvent());

        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show());

        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveEvent() {
        try {
            if (event == null) {
                event = new Event("", "", new Date(), new Date(), new Date(), "", "");
            }

            if (event.id == null) {
                event.id = UUID.randomUUID().toString();
            }

            String title = inputTitle.getText().toString().trim();
            if (title.isEmpty()) {
                inputTitle.setError("Title is required");
                return;
            }

            Date date = safeParseDate(inputDate.getText().toString());
            Date startTime = safeParseTime(inputStartTime.getText().toString());
            Date endTime = safeParseTime(inputEndTime.getText().toString());

            if (date == null || startTime == null || endTime == null) {
                showToast("Invalid date/time format");
                return;
            }

            if (endTime.before(startTime)) {
                showToast("End time must be after start time");
                return;
            }

            // Update event
            event.title = title;
            event.description = inputDescription.getText().toString().trim();
            event.date = date;
            event.startTime = startTime;
            event.endTime = endTime;
            event.location = inputLocation.getText().toString().trim();

            // Save to storage
            ArrayList<Event> events = loadEvents();
            if (events == null) {
                events = new ArrayList<>();
            }

            boolean exists = false;
            for (int i = 0; i < events.size(); i++) {
                Event e = events.get(i);
                if (e != null && e.id != null && e.id.equals(event.id)) {
                    events.set(i, event);
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                events.add(event);
            }

            saveEvents(events);
            showToast("Event saved successfully");
            setResult(RESULT_OK);
            finish();

        } catch (Exception e) {
            Log.e("EventDetails", "Save error", e);
            showToast("Error: " + e.getMessage());
        }
    }
    private Date safeParseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private Date safeParseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return null;
        try {
            return timeFormat.parse(timeStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private void deleteEvent() {
        try {
            if (event == null || event.id == null) {
                showToast("Invalid event data");
                return;
            }

            ArrayList<Event> events = loadEvents();
            if (events == null) {
                events = new ArrayList<>();
            }

            boolean removed = false;
            for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
                Event e = iterator.next();
                if (e != null && e.id != null && e.id.equals(event.id)) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }

            if (removed) {
                saveEvents(events);
                showToast("Event deleted successfully");
                setResult(RESULT_OK);
                finish();
            } else {
                showToast("Event not found in database");
            }
        } catch (Exception e) {
            Log.e("EventDetails", "Delete error", e);
            showToast("Error deleting event: " + e.getMessage());
        }
    }

    private Date parseDate(String dateStr) {
        try {
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private Date parseTime(String timeStr) {
        try {
            return timeFormat.parse(timeStr);
        } catch (ParseException e) {
            return null;
        }
    }

    private void updateOrAddEvent(ArrayList<Event> events, Event updatedEvent) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).id.equals(updatedEvent.id)) {
                events.set(i, updatedEvent);
                return;
            }
        }
        events.add(updatedEvent);
    }

    private ArrayList<Event> loadEvents() {
        String json = prefs.getString(PREF_KEY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<ArrayList<Event>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void saveEvents(ArrayList<Event> events) {
        prefs.edit()
                .putString(PREF_KEY, gson.toJson(events))
                .apply();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorAndFinish(String message) {
        showToast(message);
        finish();
    }
}