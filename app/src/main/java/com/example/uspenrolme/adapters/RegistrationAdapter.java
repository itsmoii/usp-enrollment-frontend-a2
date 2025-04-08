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
    private OnRegistrationClickListener listener;

    public interface OnRegistrationClickListener {
        void onRegistrationClick(Registration registration);
    }

    public RegistrationAdapter(List<Registration> registrations, OnRegistrationClickListener listener) {
        this.registrations = registrations;
        this.listener = listener;
    }

    public void setRegistrations(List<Registration> registrations) {
        this.registrations = registrations;
        notifyDataSetChanged();
    }

    // Add this method to return the list of registrations
    public List<Registration> getRegistrations() {
        return registrations;
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
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRegistrationClick(registration);
            }
        });
    }

    @Override
    public int getItemCount() {
        return registrations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseCodeTextView, courseNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseCodeTextView = itemView.findViewById(R.id.courseCodeTextView);
            courseNameTextView = itemView.findViewById(R.id.courseNameTextView);
        }
    }
}