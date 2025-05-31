package com.example.uspenrolme.student.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import com.example.uspenrolme.R;

public class StudentApplicationsFragment extends Fragment {

    Button trackApplicationsBtn;
    Button notificationsBtn;
    Button gradeRecheckBtn;
    Button studentFormsBtn;
    

    public StudentApplicationsFragment() {
        super(R.layout.fragment_applications);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_applications, container, false);

        trackApplicationsBtn = view.findViewById(R.id.track_application_btn);
        notificationsBtn = view.findViewById(R.id.application_notification_btn);
        //gradeRecheckBtn = view.findViewById(R.id.grade_recheck_btn);
        studentFormsBtn = view.findViewById(R.id.student_forms_btn);

        // Example: open grade recheck fragment
        //gradeRecheckBtn.setOnClickListener(v -> openFragment(new ApplicationGradeReCheckFragment()));
        studentFormsBtn.setOnClickListener(v -> openFragment(new StudentFormsFragment()));
        // Add other button listeners as needed

        return view;
    }

    private void openFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .addToBackStack(null)
                .commit();
    }
}