package com.example.uspenrolme.student.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import com.example.uspenrolme.R;

public class StudentFormsFragment extends Fragment {

    Button trackApplicationsBtn;
    Button notificationsBtn;
    Button gradeRecheckBtn;
    Button compassionateFormBtn;
    Button graduationApplicationsBtn;

    public StudentFormsFragment() {
        super(R.layout.fragment_student_forms);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_forms, container, false);

        trackApplicationsBtn = view.findViewById(R.id.track_application_btn);
        notificationsBtn = view.findViewById(R.id.application_notification_btn);
        //gradeRecheckBtn = view.findViewById(R.id.grade_recheck_btn);
        graduationApplicationsBtn = view.findViewById(R.id.graduation_form_btn);
        compassionateFormBtn = view.findViewById(R.id.compassionate_form_btn);


        compassionateFormBtn.setOnClickListener(v -> openFragment(new CompassionateAegrotatApplicationFragment()));

        // Example: open grade recheck fragment
        //gradeRecheckBtn.setOnClickListener(v -> openFragment(new ApplicationGradeReCheckFragment()));
        graduationApplicationsBtn.setOnClickListener(v -> openFragment(new GraduationApplicationFragment()));
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