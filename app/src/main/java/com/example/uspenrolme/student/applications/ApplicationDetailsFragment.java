package com.example.uspenrolme.student.applications;

import com.example.uspenrolme.models.ApplicationsModel;
import com.example.uspenrolme.adapters.ApplicationsAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.uspenrolme.R;

public class ApplicationDetailsFragment extends Fragment {

    private static final String ARG_APPLICATION = "application";

    private ApplicationsModel application;

    public static ApplicationDetailsFragment newInstance(ApplicationsModel application) {
        ApplicationDetailsFragment fragment = new ApplicationDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_APPLICATION, application);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            application = (ApplicationsModel) getArguments().getSerializable(ARG_APPLICATION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_application_details_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView idView = view.findViewById(R.id.detail_app_id);
        TextView dateView = view.findViewById(R.id.detail_app_date);
        TextView typeView = view.findViewById(R.id.detail_app_type);
        TextView statusView = view.findViewById(R.id.detail_app_status);

        if (application != null) {
            idView.setText("Application ID: " + application.getId());
            dateView.setText("Date Submitted: " + application.getDate());
            typeView.setText("Application Type: " + application.getType());
            statusView.setText("Status: " + application.getStatus());
        }
    }
}
