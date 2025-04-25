package com.example.timecast;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarViewHolder>{

    private final ArrayList<String> daysOfMonth;
    private final OnItemListener onItemListener;
    private final HashMap<String, String> taskMap;

    public CalendarAdapter(ArrayList<String> daysOfMonth, OnItemListener onItemListener, HashMap<String, String> taskMap) {
        this.daysOfMonth = daysOfMonth;
        this.onItemListener = onItemListener;
        this.taskMap = taskMap;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.calendar_cell, parent, false);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.height = (int) (parent.getHeight() * 0.166666666);

        return new CalendarViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        holder.dayOfMonth.setText(daysOfMonth.get(position));

        String day = daysOfMonth.get(position);
        holder.dayOfMonth.setText(day);

        if (!day.equals("")) {
            String dateKey = day; // e.g., "15"
            String task = taskMap.getOrDefault(dateKey, "");
            holder.taskText.setText(task);
        } else {
            holder.taskText.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return daysOfMonth.size();
    }

    public interface OnItemListener {
        void onItemClick(int position, String dayText);
    }
}
