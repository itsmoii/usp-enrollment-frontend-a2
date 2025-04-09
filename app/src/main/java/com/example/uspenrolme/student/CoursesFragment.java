package com.example.uspenrolme.student;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Color; // For Color
import android.widget.FrameLayout; // For FrameLayout
import android.content.Context; // For Context
import android.widget.ScrollView; // For ScrollView
import android.graphics.drawable.GradientDrawable; // For GradientDrawable
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.AuthFailureError;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.adapters.CourseAdapter;
import com.example.uspenrolme.models.Course;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.R;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Arrays;
import java.util.TreeMap;

public class CoursesFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private LinearLayout coursesContainer;
    private ProgressBar progressBar;
    private List<Course> selectedCourses;
    private RequestQueue requestQueue;
    private SharedPreference sharedPref;
    private List<Course> programCourses = new ArrayList<>();

    public CoursesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        // Initialize UI components
        coursesContainer = view.findViewById(R.id.coursesContainer);
        Button registerButton = view.findViewById(R.id.registerButton);
        progressBar = view.findViewById(R.id.progressBar); // Ensure this is initialized
        selectedCourses = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireContext());
        sharedPref = new SharedPreference(requireContext());

        // Remove toggleArrow logic
        // The toggle functionality is now implemented for Year 'N' Courses only

        // Fetch courses from API
        fetchCourses();

        loadPrerequisiteFragment();


        // Register Button listener
        registerButton.setOnClickListener(v -> registerSelectedCourses());
        

        return view;
    }

    private void fetchCourses() {
        String studentId = sharedPref.getValue_string("userID");
        String programCoursesUrl = "http://10.0.2.2:5000/api/program-courses?studentId=" + studentId;
        String completedCoursesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;
        String activeRegistrationsUrl = "http://10.0.2.2:5000/api/active-registrations?studentId=" + studentId;
    
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    
        // Fetch program courses, completed courses, and active registrations
        JsonArrayRequest programCoursesRequest = new JsonArrayRequest(Request.Method.GET, programCoursesUrl, null,
                programCoursesResponse -> {
                    JsonArrayRequest completedCoursesRequest = new JsonArrayRequest(Request.Method.GET, completedCoursesUrl, null,
                            completedCoursesResponse -> {
                                JsonArrayRequest activeRegistrationsRequest = new JsonArrayRequest(Request.Method.GET, activeRegistrationsUrl, null,
                                        activeRegistrationsResponse -> {
                                            try {
                                                // Parse responses
                                                programCourses = parseCourses(programCoursesResponse); // Assign to programCourses
                                                List<Course> completedCourses = parseCourses(completedCoursesResponse);
                                                List<Course> activeRegistrations = parseCourses(activeRegistrationsResponse);
    
                                                // Process and group courses
                                                processCourses(programCourses, completedCourses, activeRegistrations);

                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                Log.e("CoursesFragment", "Error parsing courses data: " + e.getMessage());
                                            }
                                            if (progressBar != null) {
                                                progressBar.setVisibility(View.GONE);
                                            }
                                        },
                                        this::handleError);
    
                                requestQueue.add(activeRegistrationsRequest);
                            },
                            this::handleError);
    
                    requestQueue.add(completedCoursesRequest);
                },
                this::handleError);
    
        requestQueue.add(programCoursesRequest);
    }

    private List<Course> parseCourses(JSONArray response) throws JSONException {
        List<Course> courses = new ArrayList<>();
        Log.d("CoursesFragment", "API Response: " + response.toString()); // Log the raw response
    
        for (int i = 0; i < response.length(); i++) {
            JSONObject courseObj = response.getJSONObject(i);
    
            // Use optString to handle missing keys gracefully
            String courseCode = courseObj.optString("course_code", "Unknown");
            String title = courseObj.optString("course_name", "No Title");
            String campus = courseObj.optString("course_campus", "Unknown");
            String mode = courseObj.optString("course_mode", "Unknown");
            String semester = courseObj.optString("semester", "Unknown");
            String preRequisite = courseObj.optString("pre_requisite", "None"); // Parse the new field
    
            // Log each course object for debugging
            Log.d("CoursesFragment", "Parsed Course: " + courseCode + ", " + title + ", Prerequisite: " + preRequisite);
    
            courses.add(new Course(courseCode, title, campus, mode, semester, preRequisite));
        }
        return courses;
    }

    private void processCourses(List<Course> programCourses, List<Course> completedCourses, List<Course> activeRegistrations) {
        Map<String, List<Course>> coursesByLevel = new HashMap<>();
        Set<String> completedCourseCodes = new HashSet<>();
        Set<String> activeRegistrationCodes = new HashSet<>();
    
        // Map completed and active registration courses
        for (Course course : completedCourses) {
            completedCourseCodes.add(course.getCourseCode());
        }
        for (Course course : activeRegistrations) {
            activeRegistrationCodes.add(course.getCourseCode());
        }
    
        // Filter and group courses by level
        for (Course course : programCourses) {
            String level;
    
            // Derive level from course code
            if (course.getCourseCode().length() >= 3 && Character.isDigit(course.getCourseCode().charAt(2))) {
                level = course.getCourseCode().charAt(2) + "00";
            } else {
                level = "Unknown"; // Fallback for invalid course codes
            }
    
            // Special case for CS001 (categorize under Year 2 Courses)
            if (course.getCourseCode().equals("CS001")) {
                level = "200";
            }
    
            // Skip courses that are already completed or registered
            if (completedCourseCodes.contains(course.getCourseCode()) || activeRegistrationCodes.contains(course.getCourseCode())) {
                Log.d("CoursesFragment", "Skipping course: " + course.getCourseCode() + " (Completed or Registered)");
                continue;
            }
    
            // Add course to the appropriate level group
            coursesByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(course);
        }
    
        // Dynamically create RecyclerViews for each level
        for (Map.Entry<String, List<Course>> entry : coursesByLevel.entrySet()) {
            String level = entry.getKey();
            List<Course> courses = entry.getValue();
    
            Log.d("CoursesFragment", "Creating RecyclerView for level: " + level + " with " + courses.size() + " courses");
            addLevelRecyclerView(level, courses);
        }
    }

    private void handleError(VolleyError error) {
        Log.e("CoursesFragment", "Error fetching courses: " + error.getMessage());
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                Log.e("CoursesFragment", "Response body: " + responseBody);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        progressBar.setVisibility(View.GONE);
    }

    private void addLevelRecyclerView(String level, List<Course> courses) {
        // Create a MaterialCardView for the year
        MaterialCardView cardView = new MaterialCardView(requireContext());
        cardView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        cardView.setCardElevation(4);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.light_gray));
        cardView.setUseCompatPadding(true);
        cardView.setContentPadding(16, 16, 16, 16);
    
        // Create a LinearLayout for the card content
        LinearLayout cardContent = new LinearLayout(requireContext());
        cardContent.setOrientation(LinearLayout.VERTICAL);
        cardContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    
        // Create a LinearLayout for the title and arrow
        LinearLayout titleLayout = new LinearLayout(requireContext());
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        titleLayout.setPadding(16, 16, 16, 16); // Add padding around the title
        titleLayout.setBackgroundColor(getResources().getColor(R.color.blue)); // Set blue background
    
        // Create a TextView for the year title
        TextView levelTitleTextView = new TextView(requireContext());
        levelTitleTextView.setText("Year " + level.charAt(0) + " Courses");
        levelTitleTextView.setTextSize(16);
        levelTitleTextView.setTextColor(getResources().getColor(R.color.white)); // Set white text color
        levelTitleTextView.setTypeface(null, Typeface.BOLD);
        levelTitleTextView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1 // Weight to push the arrow to the right
        ));
    
        // Create an ImageView for the arrow
        ImageView arrowImageView = new ImageView(requireContext());
        arrowImageView.setImageResource(R.drawable.ic_arrow_down);
        arrowImageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    
        // Create a RecyclerView for the courses
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        CourseAdapter adapter = new CourseAdapter(courses, this);
        recyclerView.setAdapter(adapter);
    
        // Assign a unique ID to the RecyclerView based on the level
        recyclerView.setId(View.generateViewId()); // Dynamically generate a unique ID
    
        // Add spacing above the RecyclerView
        recyclerView.setPadding(0, 16, 0, 0); // Add top padding to create space
        recyclerView.setClipToPadding(false);
    
        // Initially hide the RecyclerView
        recyclerView.setVisibility(View.GONE);
    
        // Add a click listener to toggle the visibility of the RecyclerView and change the arrow
        titleLayout.setOnClickListener(v -> {
            if (recyclerView.getVisibility() == View.GONE) {
                recyclerView.setVisibility(View.VISIBLE);
                arrowImageView.setImageResource(R.drawable.ic_arrow_up); // Change arrow to up
            } else {
                recyclerView.setVisibility(View.GONE);
                arrowImageView.setImageResource(R.drawable.ic_arrow_down); // Change arrow to down
            }
        });
    
        // Add the title and arrow to the title layout
        titleLayout.addView(levelTitleTextView);
        titleLayout.addView(arrowImageView);
    
        // Add the title layout and RecyclerView to the card content
        cardContent.addView(titleLayout);
        cardContent.addView(recyclerView);
    
        // Add the card content to the card view
        cardView.addView(cardContent);
    
        // Add the card view to the container
        coursesContainer.addView(cardView);
    }

    @Override
    public void onCourseClick(Course course, boolean isChecked) {
        if (isChecked) {
            selectedCourses.add(course);
        } else {
            selectedCourses.remove(course);
        }
    }

    private void registerSelectedCourses() {
        if (selectedCourses.isEmpty()) {
            Log.d("CoursesFragment", "No courses selected for registration.");
            return;
        }
    
        String studentId = sharedPref.getValue_string("userID");
        String registerCourseUrl = "http://10.0.2.2:5000/api/registerCourse";
    
        progressBar.setVisibility(View.VISIBLE);
    
        for (Course course : selectedCourses) {
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("studentId", studentId);
                requestBody.put("courseCode", course.getCourseCode());
                requestBody.put("semester", course.getSemester());
                requestBody.put("year", 2025); // Hardcoded year, adjust as needed
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
    
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, registerCourseUrl, requestBody,
                    response -> {
                        try {
                            if (response.has("message") && response.getString("message").equals("Course registered successfully")) {
                                Log.d("CoursesFragment", "Successfully registered course: " + course.getCourseCode());
                                // Remove the course from the available list
                                removeCourseFromUI(course);
                                // Optionally refresh active registrations
                                refreshActiveRegistrations();
    
                                // Show a styled Snackbar for successful registration
                                Snackbar snackbar = Snackbar.make(requireView(), "Course " + course.getCourseCode() + " registered successfully!", Snackbar.LENGTH_LONG);
                                View snackbarView = snackbar.getView();
                                snackbarView.setBackgroundColor(getResources().getColor(R.color.green)); // Set background color
                                TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                                textView.setTextColor(getResources().getColor(R.color.white)); // Set text color
                                textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // Center align text
                                snackbar.show();
                            } else {
                                Log.e("CoursesFragment", "Failed to register course: " + course.getCourseCode());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        Log.e("CoursesFragment", "Error registering course: " + course.getCourseCode());
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
    
        progressBar.setVisibility(View.GONE);
        selectedCourses.clear();
    }

    private void removeCourseFromUI(Course course) {
        for (int i = 0; i < coursesContainer.getChildCount(); i++) {
            View cardView = coursesContainer.getChildAt(i);
            if (cardView instanceof MaterialCardView) {
                // Find the RecyclerView inside the card view
                RecyclerView recyclerView = cardView.findViewById(View.generateViewId()); // Use the dynamically generated ID
                if (recyclerView != null) {
                    CourseAdapter adapter = (CourseAdapter) recyclerView.getAdapter();
                    if (adapter != null && adapter.getCourses().contains(course)) {
                        adapter.removeCourse(course);
                        adapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    }
    
    private void refreshActiveRegistrations() {
        RegistrationFragment registrationFragment = (RegistrationFragment) requireActivity()
                .getSupportFragmentManager()
                .findFragmentByTag("RegistrationFragment");
        if (registrationFragment != null) {
            registrationFragment.loadActiveRegistrations(); // Ensure this method is public
        }
    }

    private void loadPrerequisiteFragment() {
        PrerequisiteFragment prerequisiteFragment = new PrerequisiteFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.prerequisiteGraphContainer, prerequisiteFragment)
                .commit();
    }
}