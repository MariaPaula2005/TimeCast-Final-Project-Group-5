package com.example.timecast;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


public class MonthViewFragment extends Fragment  implements CalendarAdapter.OnItemListener{

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private TextView monthYearText;
    private RecyclerView calendarRecyclerView;
    private LocalDate selectedDate;
    private OnMonthChangeListener monthChangeListener;

    public MonthViewFragment() {
        // Required empty public constructor
    }
    public interface OnMonthChangeListener {
        void onMonthChanged(String newMonthYear);
    }

    public static MonthViewFragment newInstance(String param1, String param2) {
        MonthViewFragment fragment = new MonthViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_month_view, container, false);

        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView);

        selectedDate = LocalDate.now();
        setMonthView();

        return view;
    }

    private void setMonthView() {

        ArrayList<String> daysInMonth = daysInMonthArray(selectedDate);
        HashMap<String, String> taskMap = getTaskMapForMonth(selectedDate);

        CalendarAdapter calendarAdapter = new CalendarAdapter(daysInMonth,this, taskMap);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(requireContext(), 7);

        calendarRecyclerView.setLayoutManager(layoutManager);
        calendarRecyclerView.setAdapter(calendarAdapter);

        if(monthChangeListener != null) {
            String monthYear = monthYearFromDate(selectedDate);
            monthChangeListener.onMonthChanged(monthYear);
        }

    }

    private ArrayList<String> daysInMonthArray(LocalDate date) {
        ArrayList<String> daysInMonthArray = new ArrayList<>();
        YearMonth yearMonth = YearMonth.from(date);

        int daysInMonth = yearMonth.lengthOfMonth();

        LocalDate firstOfMonth = selectedDate.withDayOfMonth(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();

        for(int i = 1; i <= 42; i++) {
            if(i <= dayOfWeek || i > daysInMonth + dayOfWeek){
                daysInMonthArray.add("");
            } else {
                daysInMonthArray.add(String.valueOf(i - dayOfWeek));
            }
        }
        return daysInMonthArray;
    }

    private String monthYearFromDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        return date.format(formatter);
    }

    public void previousMonthAction(){
        selectedDate = selectedDate.minusMonths(1);
        setMonthView();
    }
    public void nextMonthAction(){
        selectedDate = selectedDate.plusMonths(1);
        setMonthView();
    }

    @Override
    public void onItemClick(int position, String dayText) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMonthChangeListener) {
            monthChangeListener = (OnMonthChangeListener) context;
        }
    }

    private ArrayList<Event> loadEvents(){
        SharedPreferences prefs = requireContext().getSharedPreferences("TimeCastPrefs", Context.MODE_PRIVATE);

        Gson gson = new Gson();
        String json = prefs.getString("event_data", null);
        Type type = new TypeToken<ArrayList<Event>>() {}.getType();


        return json == null ? new ArrayList<>() : gson.fromJson(json, type);
    }

    private HashMap<String, String> getTaskMapForMonth(LocalDate selectedDate){
        ArrayList<Event> allEvents = loadEvents();
        HashMap<String, String> taskMap = new HashMap<>();

        YearMonth yearMonth = YearMonth.from(selectedDate);

        for(Event event : allEvents){
            Calendar cal  = Calendar.getInstance();
            cal.setTime(event.date);
            int eventDay = cal.get(Calendar.DAY_OF_MONTH);
            int eventMonth = cal.get(Calendar.MONTH) + 1;
            int eventYear = cal.get(Calendar.YEAR);

            if (eventYear == selectedDate.getYear() && eventMonth == selectedDate.getMonthValue()) {
                String key = String.valueOf(eventDay);
                String current = taskMap.getOrDefault(key, "");
                String taskText = event.title;

                taskMap.put(key, current.isEmpty() ? taskText : current + ", " + taskText);
            }
        }
        return taskMap;
    }

    private void SaveEvents(ArrayList<Event> eventList){
        SharedPreferences prefs = requireContext().getSharedPreferences("TimeCastPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(eventList);
        editor.putString("event_data", json);
        editor.apply();
    }
}