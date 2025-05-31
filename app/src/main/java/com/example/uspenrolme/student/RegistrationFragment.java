package com.example.uspenrolme.student;

import android.os.Bundle;
import android.util.Log;
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
import androidx.appcompat.app.AlertDialog;

import com.example.uspenrolme.UtilityService.HoldUtils;
import com.example.uspenrolme.shared.ErrorFragment;
import com.google.android.material.snackbar.Snackbar;

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
        registrationAdapter = new RegistrationAdapter(new ArrayList<>(), this::showCourseDetailsDialog);
        registrationRecyclerView.setAdapter(registrationAdapter);

        droppedRegistrationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        droppedRegistrationAdapter = new RegistrationAdapter(new ArrayList<>(), this::showCourseDetailsDialog);
        droppedRegistrationRecyclerView.setAdapter(droppedRegistrationAdapter);

        String token = sharedPref.getValue_string("token");

        HoldUtils.checkHold(requireContext(), token, "registration", isBlocked -> {
            if(isBlocked){
                showHoldPage();
            } else{
                // Load data
                loadProfileData();
                progressBar.setVisibility(View.VISIBLE);
                loadActiveRegistrations();
                loadDroppedRegistrations();
                progressBar.setVisibility(View.GONE);

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
            }
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

    public void loadActiveRegistrations() {
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

    private void showCourseDetailsDialog(Registration registration) {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_course_details, null);
    
        // Initialize dialog components
        TextView courseTitleTextView = dialogView.findViewById(R.id.courseTitleTextView);
        TextView courseDetailsTextView = dialogView.findViewById(R.id.courseDetailsTextView);
        Button actionButton = dialogView.findViewById(R.id.cancelRegistrationButton);
    
        // Set course details
        courseTitleTextView.setText(registration.getCourseName());
        courseDetailsTextView.setText("Code: " + registration.getCourseCode() + "\nCampus: " + registration.getCourseCampus() +
                "\nMode: " + registration.getCourseMode() + "\nStatus: " + registration.getStatus());
    
        // Initialize the dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
    
        // Set button text, color, and action based on course status
        if (registration.getStatus().equalsIgnoreCase("cancelled") || registration.getStatus().equalsIgnoreCase("Dropped")) {
            actionButton.setText("Cancel Withdrawal");
            actionButton.setBackgroundColor(getResources().getColor(R.color.green));
            actionButton.setOnClickListener(v -> {
                reRegisterCourse(registration.getCourseCode(), dialog);
            });
        } else {
            actionButton.setText("Withdraw Registeration");
            actionButton.setBackgroundColor(getResources().getColor(R.color.red));
            actionButton.setOnClickListener(v -> {
                cancelRegistration(registration.getCourseCode(), dialog);
            });
        }
    
        // Show the dialog
        dialog.show();
    }
    
    private void cancelRegistration(String courseCode, AlertDialog dialog) {
        String studentId = sharedPref.getValue_string("userID");
        String url = "http://10.0.2.2:5000/api/cancelCourse";
    
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("studentId", studentId);
            requestBody.put("courseCode", courseCode);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    dialog.dismiss();
    
                    // Find and remove the course from Active Registrations
                    Registration canceledRegistration = null;
                    for (Registration registration : registrationAdapter.getRegistrations()) {
                        if (registration.getCourseCode().equals(courseCode)) {
                            canceledRegistration = registration;
                            break;
                        }
                    }
                    if (canceledRegistration != null) {
                        registrationAdapter.getRegistrations().remove(canceledRegistration);
                        registrationAdapter.notifyDataSetChanged();
    
                        // Update the status and add the course to Dropped Registrations
                        canceledRegistration.setStatus("Dropped");
                        droppedRegistrationAdapter.getRegistrations().add(canceledRegistration);
                        droppedRegistrationAdapter.notifyDataSetChanged();
                    }
    
                    Snackbar.make(requireView(), "Registration canceled successfully!", Snackbar.LENGTH_LONG).show();
                },
                error -> {
                    error.printStackTrace();
                    Snackbar.make(requireView(), "Failed to cancel registration.", Snackbar.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + sharedPref.getValue_string("token"));
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
    
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

    private void reRegisterCourse(String courseCode, AlertDialog dialog) {
        String studentId = sharedPref.getValue_string("userID");
        String url = "http://10.0.2.2:5000/api/registerCourse"; // Reuse the same endpoint
    
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("studentId", studentId);
            requestBody.put("courseCode", courseCode);
            requestBody.put("semester", "Semester 1"); // Adjust as needed
            requestBody.put("year", 2025); // Adjust as needed
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    dialog.dismiss();
    
                    // Find and remove the course from Dropped Registrations
                    Registration reRegisteredCourse = null;
                    for (Registration registration : droppedRegistrationAdapter.getRegistrations()) {
                        if (registration.getCourseCode().equals(courseCode)) {
                            reRegisteredCourse = registration;
                            break;
                        }
                    }
                    if (reRegisteredCourse != null) {
                        droppedRegistrationAdapter.getRegistrations().remove(reRegisteredCourse);
                        droppedRegistrationAdapter.notifyDataSetChanged();
    
                        // Update the status and add the course to Active Registrations
                        reRegisteredCourse.setStatus("Active");
                        registrationAdapter.getRegistrations().add(reRegisteredCourse);
                        registrationAdapter.notifyDataSetChanged();
                    }
    
                    Snackbar.make(requireView(), "Course re-registered successfully!", Snackbar.LENGTH_LONG).show();
                },
                error -> {
                    error.printStackTrace();
                    Snackbar.make(requireView(), "Failed to re-register course.", Snackbar.LENGTH_LONG).show();
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + sharedPref.getValue_string("token"));
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
    
        requestQueue.add(request);
    }
    private void reloadFragment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .detach(this)
                .attach(this)
                .commit();
    }

    private void showHoldPage(){
        Log.d("YourFragment", "Showing hold page now...");
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, new ErrorFragment())
                .commit();
    }
}