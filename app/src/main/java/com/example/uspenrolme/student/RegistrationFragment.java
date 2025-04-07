package com.example.uspenrolme.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.adapters.RegistrationAdapter;
import com.example.uspenrolme.models.Registration;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrationFragment extends Fragment {

    private RecyclerView registrationRecyclerView, droppedRegistrationRecyclerView;
    private ProgressBar progressBar;
    private Button addCourseButton;
    private TextView studentIdTextView, programTextView, emailTextView, studentNameTextView;
    private RegistrationAdapter registrationAdapter, droppedRegistrationAdapter;
    private SharedPreference sharedPref;
    private RequestQueue requestQueue;

    public RegistrationFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration, container, false);

        // Initialize views
        registrationRecyclerView = view.findViewById(R.id.registrationRecyclerView);
        droppedRegistrationRecyclerView = view.findViewById(R.id.droppedRegistrationRecyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        addCourseButton = view.findViewById(R.id.addCourseButton);
        studentIdTextView = view.findViewById(R.id.studentIdTextView);
        programTextView = view.findViewById(R.id.programTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        studentNameTextView = view.findViewById(R.id.studentNameTextView);

        // Initialize utilities
        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        // Setup RecyclerViews
        registrationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        registrationAdapter = new RegistrationAdapter(new ArrayList<>());
        registrationRecyclerView.setAdapter(registrationAdapter);

        droppedRegistrationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        droppedRegistrationAdapter = new RegistrationAdapter(new ArrayList<>());
        droppedRegistrationRecyclerView.setAdapter(droppedRegistrationAdapter);

        // Load data
        loadProfileData();
        loadActiveRegistrations();
        loadDroppedRegistrations();

        // Set click listener for Add Course button
        addCourseButton.setOnClickListener(v -> {
            // Replace the current fragment with CoursesFragment
            CoursesFragment coursesFragment = new CoursesFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content, coursesFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void loadProfileData() {
        String url = "http://10.0.2.2:5000/api/profile";
        String token = sharedPref.getValue_string("token");

        progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONObject userObj = response.getJSONObject("user");
                            JSONObject dataObj = userObj.getJSONObject("data");
                            JSONObject studentProfile = dataObj.getJSONObject("studentProfile");

                            studentIdTextView.setText(studentProfile.getString("student_id"));
                            studentNameTextView.setText(studentProfile.getString("first_name") + " " + studentProfile.getString("last_name"));
                            programTextView.setText(studentProfile.getString("program_code"));
                            emailTextView.setText(studentProfile.getString("email"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    error.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void loadActiveRegistrations() {
        String studentId = sharedPref.getValue_string("userID");
        String url = "http://10.0.2.2:5000/api/active-registrations?studentId=" + studentId;

        progressBar.setVisibility(View.VISIBLE);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        List<Registration> registrations = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            registrations.add(new Registration(
                                    obj.getString("course_code"),
                                    obj.getString("course_name"),
                                    obj.getString("course_campus"),
                                    obj.getString("course_mode"),
                                    obj.getString("status")
                            ));
                        }
                        registrationAdapter.setRegistrations(registrations);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    error.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                });

        requestQueue.add(request);
    }

    private void loadDroppedRegistrations() {
        String studentId = sharedPref.getValue_string("userID");
        String url = "http://10.0.2.2:5000/api/dropped-registrations?studentId=" + studentId;

        progressBar.setVisibility(View.VISIBLE);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        List<Registration> droppedRegistrations = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            droppedRegistrations.add(new Registration(
                                    obj.getString("course_code"),
                                    obj.getString("course_name"),
                                    obj.getString("course_campus"),
                                    obj.getString("course_mode"),
                                    obj.getString("status")
                            ));
                        }
                        droppedRegistrationAdapter.setRegistrations(droppedRegistrations);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    error.printStackTrace();
                    progressBar.setVisibility(View.GONE);
                });

        requestQueue.add(request);
    }
}