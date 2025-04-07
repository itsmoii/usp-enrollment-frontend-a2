package com.example.uspenrolme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uspenrolme.R;
import com.example.uspenrolme.models.Registration;

import java.util.List;

public class RegistrationAdapter extends RecyclerView.Adapter<RegistrationAdapter.ViewHolder> {

    private List<Registration> registrations;

    public RegistrationAdapter(List<Registration> registrations) {
        this.registrations = registrations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registration, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Registration registration = registrations.get(position);
        holder.courseCodeTextView.setText(registration.getCourseCode());
        holder.courseNameTextView.setText(registration.getCourseName());
        holder.courseModeTextView.setText(registration.getCourseMode());
    }

    @Override
    public int getItemCount() {
        return registrations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseCodeTextView, courseNameTextView, courseCampusTextView, courseModeTextView, statusTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseCodeTextView = itemView.findViewById(R.id.courseCodeTextView);
            courseNameTextView = itemView.findViewById(R.id.courseNameTextView);
            courseCampusTextView = itemView.findViewById(R.id.courseCampusTextView);
            courseModeTextView = itemView.findViewById(R.id.courseModeTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }
    }

    public void setRegistrations(List<Registration> newRegistrations) {
        this.registrations = newRegistrations;
        notifyDataSetChanged();
    }
    
}

