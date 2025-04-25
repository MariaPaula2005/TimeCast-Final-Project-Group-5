package com.example.timecast;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class WeekViewFragment extends Fragment {

    private static final int START_HOUR = 8;
    private static final int END_HOUR = 20;
    private static final int SLOT_HEIGHT_DP = 60;
    private final String PREF_KEY = "event_data";

    private LinearLayout weekTimeLineContainer;
    private LinearLayout timeLabelColumn;
    private LinearLayout weekDayHeaders;
    private Calendar currentWeekStart = null;
    private OnWeekChangeListener weekChangeListener;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week_view, container, false);

        timeLabelColumn = view.findViewById(R.id.timeLabelColumn);
        weekTimeLineContainer = view.findViewById(R.id.weekTimelineContainer);
        weekDayHeaders = view.findViewById(R.id.weekDayHeaders);

        buildWeekDayHeaders();
        buildTimeLabels();
        setupWeekColumns();

        return view;
    }

    private void buildWeekDayHeaders() {
        weekDayHeaders.removeAllViews();
        String[] dayLabels = {"S", "M", "T", "W", "T", "F", "S"};

        View timeSpacer = new View(requireContext());
        timeSpacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(50), ViewGroup.LayoutParams.MATCH_PARENT));
        weekDayHeaders.addView(timeSpacer);

        for (String label : dayLabels) {
            TextView dayHeader = new TextView(getContext());
            dayHeader.setText(label);
            dayHeader.setGravity(Gravity.CENTER);
            dayHeader.setTextColor(Color.BLACK);
            dayHeader.setTextSize(16);
            dayHeader.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            weekDayHeaders.addView(dayHeader);
        }
    }

    private void buildTimeLabels() {
        timeLabelColumn.removeAllViews();
        for (int hour = START_HOUR; hour < END_HOUR; hour++) {
            for (int half = 0; half < 2; half++) {
                TextView label = new TextView(getContext());
                label.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, half == 0 ? 0 : 30));
                label.setTextSize(12);
                label.setTextColor(Color.DKGRAY);
                label.setPadding(4, 4, 4, 4);
                label.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, dpToPx(SLOT_HEIGHT_DP / 2)));
                timeLabelColumn.addView(label);
            }
        }
    }

    private void setupWeekColumns() {
        weekTimeLineContainer.removeAllViews();
        currentWeekStart = (currentWeekStart != null) ? (Calendar) currentWeekStart.clone() : GetStartOfCurrentWeek();
        ArrayList<Event> allEvents = loadEvents();

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) currentWeekStart.clone();
            day.add(Calendar.DAY_OF_YEAR, i);

            LinearLayout dayColumn = new LinearLayout(getContext());
            dayColumn.setOrientation(LinearLayout.VERTICAL);

            dayColumn.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            dayColumn.setBackgroundColor(Color.WHITE); // visible base

            dayColumn.setFocusable(true);
            dayColumn.setClickable(true);

            dayColumn.setOnDragListener((view, dragEvent) -> {
                Log.d("WeekViewDrag", "DragEvent: " + dragEvent.getAction());
                switch (dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;
                    case DragEvent.ACTION_DROP:
                        float dropY = dragEvent.getY();
                        int minutesFromTop = (int) (dropY / getResources().getDisplayMetrics().density);
                        int newStartMinutes = minutesFromTop + START_HOUR * 60;

                        Event draggedEvent = (Event) dragEvent.getLocalState();

                        Calendar newStart = (Calendar) day.clone();
                        newStart.set(Calendar.HOUR_OF_DAY, newStartMinutes / 60);
                        newStart.set(Calendar.MINUTE, newStartMinutes % 60);

                        Calendar newEnd = (Calendar) newStart.clone();
                        long durationMillis = draggedEvent.endTime.getTime() - draggedEvent.startTime.getTime();
                        newEnd.setTimeInMillis(newStart.getTimeInMillis() + durationMillis);

                        draggedEvent.startTime = newStart.getTime();
                        draggedEvent.endTime = newEnd.getTime();
                        draggedEvent.date = newStart.getTime();

                        saveUpdatedEvent(draggedEvent);
                        refreshWeekView();
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        return true;
                    default:
                        return false;
                }
            });

            addEventsToDayColumn(day, allEvents, dayColumn);
            weekTimeLineContainer.addView(dayColumn);

            if (i < 6) {
                View divider = new View(getContext());
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(dpToPx(1), ViewGroup.LayoutParams.MATCH_PARENT);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(Color.BLACK); // ← the visible divider
                weekTimeLineContainer.addView(divider);
            }
        }
    }

    private void addEventsToDayColumn(Calendar day, ArrayList<Event> events, LinearLayout column) {
        column.removeAllViews();
        for (Event event : events) {
            Calendar eventDay = Calendar.getInstance();
            eventDay.setTime(event.date);
            if (eventDay.get(Calendar.YEAR) != day.get(Calendar.YEAR) ||
                    eventDay.get(Calendar.DAY_OF_YEAR) != day.get(Calendar.DAY_OF_YEAR)) {
                continue;
            }

            Calendar start = Calendar.getInstance();
            start.setTime(event.startTime);
            int startMinutes = start.get(Calendar.HOUR_OF_DAY) * 60 + start.get(Calendar.MINUTE);
            int offsetMinutes = startMinutes - START_HOUR * 60;

            Calendar end = Calendar.getInstance();
            end.setTime(event.endTime);
            int endMinutes = end.get(Calendar.HOUR_OF_DAY) * 60 + end.get(Calendar.MINUTE);
            int durationMinutes = endMinutes - startMinutes;

            if (offsetMinutes < 0 || offsetMinutes + durationMinutes > (END_HOUR - START_HOUR) * 60) continue;

            TextView eventBlock = new TextView(getContext());
            eventBlock.setText(String.format("%s\n%s\n%s", event.title, event.getFormattedTimeRange(), event.type));
            eventBlock.setTextColor(Color.WHITE);
            eventBlock.setTextSize(12);
            eventBlock.setGravity(Gravity.TOP);
            eventBlock.setBackgroundResource(R.drawable.activity_background);
            eventBlock.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(durationMinutes));
            params.topMargin = dpToPx(offsetMinutes);
            eventBlock.setLayoutParams(params);

            eventBlock.setOnLongClickListener(v -> {
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(null, shadowBuilder, event, 0);
                return true;
            });

            column.addView(eventBlock);
        }
    }

    private void saveUpdatedEvent(Event updatedEvent) {
        SharedPreferences prefs = requireContext().getSharedPreferences("TimeCastPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("event_data", null);
        Type type = new TypeToken<ArrayList<Event>>() {}.getType();
        ArrayList<Event> allEvents = (json != null) ? new Gson().fromJson(json, type) : new ArrayList<>();

        for (int i = 0; i < allEvents.size(); i++) {
            Event e = allEvents.get(i);
            if (e.id.equals(updatedEvent.id)) {
                allEvents.set(i, updatedEvent);
                break;
            }
        }

        prefs.edit().putString("event_data", new Gson().toJson(allEvents)).apply();
    }

    private int dpToPx(int dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private ArrayList<Event> loadEvents() {
        SharedPreferences prefs = requireContext().getSharedPreferences("TimeCastPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString(PREF_KEY, null);
        Type type = new TypeToken<ArrayList<Event>>() {}.getType();
        return (json != null) ? new Gson().fromJson(json, type) : new ArrayList<>();
    }

    private Calendar GetStartOfCurrentWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        int diff = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        if (diff != 0) cal.add(Calendar.DAY_OF_YEAR, -diff);
        return cal;
    }

    public void navigateWeek(int offset) {
        if (currentWeekStart == null) {
            currentWeekStart = GetStartOfCurrentWeek();
        }
        currentWeekStart.add(Calendar.WEEK_OF_YEAR, offset);
        refreshWeekView();
    }

    private void refreshWeekView() {

        if (currentWeekStart == null) currentWeekStart = GetStartOfCurrentWeek();
        buildWeekDayHeaders();
        buildTimeLabels();
        setupWeekColumns();
        if (weekChangeListener != null) {
            SimpleDateFormat fmt = new SimpleDateFormat("MMM d", Locale.getDefault());
            Calendar end = (Calendar) currentWeekStart.clone();
            end.add(Calendar.DAY_OF_YEAR, 6);
            String range = fmt.format(currentWeekStart.getTime()) + " - " + fmt.format(end.getTime());
            weekChangeListener.onWeekChanged(range);
        }
    }

    public interface OnWeekChangeListener {
        void onWeekChanged(String weekRange);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWeekChangeListener) {
            weekChangeListener = (OnWeekChangeListener) context;
        }
    }

}
