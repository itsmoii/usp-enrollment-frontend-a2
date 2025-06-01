package com.example.uspenrolme.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import androidx.fragment.app.Fragment;

import com.example.uspenrolme.R;

public class RegisteredCoursesFragment extends Fragment {
    private TableLayout registeredCoursesTable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registered_courses, container, false);
        registeredCoursesTable = view.findViewById(R.id.registeredCoursesTable);
        // Populate your table here
        return view;
    }
}