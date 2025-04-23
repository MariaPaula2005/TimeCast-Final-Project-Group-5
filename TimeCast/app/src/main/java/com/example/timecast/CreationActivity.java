package com.example.timecast;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;


public class CreationActivity extends AppCompatActivity {
    private EditText inputTitle, inputDescription, inputDate, inputStartTime, inputEndTime, inputLocation;
    private Spinner inputType;
    private Button btnSave;
    private Button btnCancel;
    private ArrayList<Event> eventList = new ArrayList<>();
    private SharedPreferences prefs;
    private Gson gson = new Gson();
    private final String PREF_KEY = "event_data";
    private Calendar dateCalendar = Calendar.getInstance();
    private Calendar startTimeCalendar = Calendar.getInstance();
    private Calendar endTimeCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private Spinner inputReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creation);

        initializeViews();
        setupSpinner();
        setupDatePicker();
        setupTimePickers();
        loadEvents();

        btnSave.setOnClickListener(v -> saveEvent());
    }

    private void initializeViews() {
        inputTitle = findViewById(R.id.inputTitle);
        inputDescription = findViewById(R.id.inputDescription);
        inputDate = findViewById(R.id.inputDate);
        inputStartTime = findViewById(R.id.inputStartTime);
        inputEndTime = findViewById(R.id.inputEndTime);
        inputType = findViewById(R.id.inputType);
        inputLocation = findViewById(R.id.inputLocation);
        btnSave = findViewById(R.id.btnSave);
        prefs = getSharedPreferences("TimeCastPrefs", MODE_PRIVATE);
        btnCancel = findViewById(R.id.cancel_button);
        inputReminder = findViewById(R.id.inputReminder);


        // Set current date and time defaults
        inputDate.setText(dateFormat.format(dateCalendar.getTime()));
        inputStartTime.setText(timeFormat.format(startTimeCalendar.getTime()));
        endTimeCalendar.add(Calendar.MINUTE, 30); // 30 minute duration
        inputEndTime.setText(timeFormat.format(endTimeCalendar.getTime()));

        btnCancel.setOnClickListener(v -> {
            startActivity(new Intent(CreationActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.event_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ArrayAdapter<CharSequence> reminder = ArrayAdapter.createFromResource(
                this, R.array.reminder_options, android.R.layout.simple_spinner_item);
        reminder.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputType.setAdapter(adapter);
        inputReminder.setAdapter(reminder);
    }

    private void setupDatePicker() {
        inputDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                dateCalendar.set(Calendar.YEAR, year);
                dateCalendar.set(Calendar.MONTH, month);
                dateCalendar.set(Calendar.DAY_OF_MONTH, day);
                inputDate.setText(dateFormat.format(dateCalendar.getTime()));
            },
                    dateCalendar.get(Calendar.YEAR),
                    dateCalendar.get(Calendar.MONTH),
                    dateCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupTimePickers() {
        inputStartTime.setOnClickListener(v -> showTimePicker(inputStartTime, true));
        inputEndTime.setOnClickListener(v -> showTimePicker(inputEndTime, false));
    }

    private void showTimePicker(EditText editText, boolean isStartTime) {
        Calendar currentCalendar = isStartTime ? startTimeCalendar : endTimeCalendar;

        new TimePickerDialog(this, (view, hour, minute) -> {
            currentCalendar.set(Calendar.HOUR_OF_DAY, hour);
            currentCalendar.set(Calendar.MINUTE, minute);

            // ensure end time is after start time
            if (isStartTime && !endTimeCalendar.after(startTimeCalendar)) {
                endTimeCalendar.set(Calendar.HOUR_OF_DAY, hour);
                endTimeCalendar.set(Calendar.MINUTE, minute + 30);
                inputEndTime.setText(timeFormat.format(endTimeCalendar.getTime()));
            }

            editText.setText(timeFormat.format(currentCalendar.getTime()));
        },
                currentCalendar.get(Calendar.HOUR_OF_DAY),
                currentCalendar.get(Calendar.MINUTE),
                true).show();
    }

    private void saveEvent() {
        try {
            String title = inputTitle.getText().toString().trim();
            String desc = inputDescription.getText().toString().trim();
            String dateStr = inputDate.getText().toString().trim();
            String startTimeStr = inputStartTime.getText().toString().trim();
            String endTimeStr = inputEndTime.getText().toString().trim();
            String type = inputType.getSelectedItem().toString();
            String location = inputLocation.getText().toString().trim();

            // Validate inputs
            if (title.isEmpty() || dateStr.isEmpty() || startTimeStr.isEmpty() || endTimeStr.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse date and times
            Date date = dateFormat.parse(dateStr);
            Date startTime = timeFormat.parse(startTimeStr);
            Date endTime = timeFormat.parse(endTimeStr);
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(date);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = (Calendar) startCal.clone();

            Calendar tempStartTime = Calendar.getInstance();
            tempStartTime.setTime(startTime);
            startCal.set(Calendar.HOUR_OF_DAY, tempStartTime.get(Calendar.HOUR_OF_DAY));
            startCal.set(Calendar.MINUTE, tempStartTime.get(Calendar.MINUTE));

            Calendar tempEndTime = Calendar.getInstance();
            tempEndTime.setTime(endTime);
            endCal.set(Calendar.HOUR_OF_DAY, tempEndTime.get(Calendar.HOUR_OF_DAY));
            endCal.set(Calendar.MINUTE, tempEndTime.get(Calendar.MINUTE));


            if (!endCal.after(startCal)) {
                Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create new event
            Event newEvent = new Event(title, desc, date, startCal.getTime(), endCal.getTime(), type, location);

            // Load existing events
            String json = prefs.getString(PREF_KEY, null);
            ArrayList<Event> allEvents = new ArrayList<>();
            if (json != null) {
                Type listType = new TypeToken<ArrayList<Event>>(){}.getType();
                allEvents = gson.fromJson(json, listType);
            }

            // Check for event conflict
            if (isConflicting(startCal, endCal)) {
                Toast.makeText(CreationActivity.this, "Time conflict with another event!", Toast.LENGTH_LONG).show();
                return;
            }

            // Add new event
            allEvents.add(newEvent);

            // Calculate reminder offset
            int reminderOffsetMinutes = 0;
            String selectedReminder = inputReminder.getSelectedItem().toString();
            switch (selectedReminder) {
                case "5 minutes before": reminderOffsetMinutes = 5; break;
                case "10 minutes before": reminderOffsetMinutes = 10; break;
                case "30 minutes before": reminderOffsetMinutes = 30; break;
                case "1 hour before": reminderOffsetMinutes = 60; break;
            }

            // Schedule reminder
            if (reminderOffsetMinutes > 0) {
                Calendar reminderTime = (Calendar) startCal.clone();
                reminderTime.add(Calendar.MINUTE, -reminderOffsetMinutes);

                Intent reminderIntent = new Intent(this, ReminderReceiver.class);
                reminderIntent.putExtra("title", title);
                reminderIntent.putExtra("desc", desc);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this, (int) System.currentTimeMillis(), reminderIntent, PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(this, "Cannot schedule exact alarms. Enable permission in settings.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(intent);
                        return; // Wait until permission is granted before scheduling
                    }
                }
                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
                } catch (SecurityException e) {
                    Toast.makeText(this, "Permission denied for exact alarm: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }

            // Save all events
            prefs.edit().putString(PREF_KEY, gson.toJson(allEvents)).apply();

            Toast.makeText(this, "Event saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Log.e("ReminderDebug", "Exception when saving event", e); // log full stacktrace
            Toast.makeText(this, "Error saving event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadEvents() {
        String json = prefs.getString(PREF_KEY, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Event>>() {}.getType();
            eventList = gson.fromJson(json, type);
            if (eventList == null) {
                eventList = new ArrayList<>();
            }
        } else {
            eventList = new ArrayList<>();
        }
    }

    private boolean isConflicting(Calendar newStart, Calendar newEnd) {
        for (Event event : eventList) {
            Calendar existingStart = Calendar.getInstance();
            existingStart.setTime(event.startTime);
            Calendar existingEnd = Calendar.getInstance();
            existingEnd.setTime(event.endTime);

            if (newStart.before(existingEnd) && newEnd.after(existingStart)) {
                return true;
            }
        }
        return false;
    }
}