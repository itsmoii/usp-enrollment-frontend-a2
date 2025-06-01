package com.example.uspenrolme.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.uspenrolme.R;  // Make sure this matches your package name

public class CompletedGradesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_completed_grades, container, false);

        // Initialize TableLayout
        TableLayout gradesTable = view.findViewById(R.id.gradesTable);

        // Populate your table here
        // Example: addTableRows(gradesTable);

        return view;
    }
}