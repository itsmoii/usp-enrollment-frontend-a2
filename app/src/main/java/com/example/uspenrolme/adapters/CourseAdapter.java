package com.example.uspenrolme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uspenrolme.R;
import com.example.uspenrolme.models.Course;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.ViewHolder> {

    private List<Course> courses;
    private OnCourseClickListener listener;

    public interface OnCourseClickListener {
        void onCourseClick(Course course, boolean isChecked);
    }

    public CourseAdapter(List<Course> courses, OnCourseClickListener listener) {
        this.courses = courses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Course course = courses.get(position);
        holder.bind(course, listener);
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    public void setCourses(List<Course> newCourses) {
        this.courses = newCourses;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseCodeTextView, courseNameTextView, courseModeTextView;
        CheckBox courseCheckBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseCodeTextView = itemView.findViewById(R.id.courseCodeTextView);
            courseNameTextView = itemView.findViewById(R.id.courseNameTextView);
            courseModeTextView = itemView.findViewById(R.id.courseModeTextView);
            courseCheckBox = itemView.findViewById(R.id.courseCheckBox);
        }

        public void bind(final Course course, final OnCourseClickListener listener) {
            courseCodeTextView.setText(course.getCourseCode());
            courseNameTextView.setText(course.getTitle());
            courseModeTextView.setText(course.getMode());
            courseCheckBox.setChecked(course.isSelected());
        
            // Set a listener for the CheckBox
            courseCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                course.setSelected(isChecked);
                if (listener != null) {
                    listener.onCourseClick(course, isChecked);
                }
            });
        }
    }
}