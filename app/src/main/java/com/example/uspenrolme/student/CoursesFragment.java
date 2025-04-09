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
    
                                                // Create the prerequisite graph
                                                createPrerequisiteGraph(programCourses); // Pass programCourses
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

    private void createPrerequisiteGraph(List<Course> programCourses) {
        FrameLayout graphContainer = requireView().findViewById(R.id.prerequisiteGraphContainer);
        graphContainer.removeAllViews();
    
        // Create a ScrollView to handle large graphs
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
    
        // Create a parent LinearLayout to hold the graph
        LinearLayout parentLayout = new LinearLayout(requireContext());
        parentLayout.setOrientation(LinearLayout.VERTICAL);
        parentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        parentLayout.setPadding(16, 16, 16, 16);
    
        // Data structures for organizing courses
        Map<String, List<Course>> coursesByLevel = new TreeMap<>();
        Map<String, List<String>> prerequisitesMap = new HashMap<>();
        Map<String, View> courseViews = new HashMap<>();
        Map<String, Boolean> courseCompletionStatus = new HashMap<>();
    
        // Get completed courses
        String studentId = sharedPref.getValue_string("userID");
        String completedCoursesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;
        
        // Make API call to get completed courses
        JsonArrayRequest completedCoursesRequest = new JsonArrayRequest(
                Request.Method.GET, completedCoursesUrl, null,
                response -> {
                    try {
                        Set<String> passingGrades = new HashSet<>(Arrays.asList("A+", "A", "B+", "B", "C+", "C", "S"));
                        Set<String> completedCourseCodes = new HashSet<>();
    
                        // Parse completed courses
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject courseObj = response.getJSONObject(i);
                            String courseCode = courseObj.getString("CourseID");
                            String grade = courseObj.getString("grade");
                            if (passingGrades.contains(grade)) {
                                completedCourseCodes.add(courseCode);
                            }
                        }
    
                        // First pass: organize courses by level and build prerequisites map
                        for (Course course : programCourses) {
                            String level = getCourseLevel(course.getCourseCode());
                            
                            // Store completion status
                            boolean isCompleted = completedCourseCodes.contains(course.getCourseCode());
                            courseCompletionStatus.put(course.getCourseCode(), isCompleted);
    
                            // Map prerequisites
                            if (!course.getPreRequisite().equalsIgnoreCase("none")) {
                                prerequisitesMap.put(course.getCourseCode(), 
                                    Arrays.asList(course.getPreRequisite().split("\\s*,\\s*")));
                            } else {
                                prerequisitesMap.put(course.getCourseCode(), new ArrayList<>());
                            }
    
                            // Group by level
                            coursesByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(course);
                        }
    
                        // Second pass: create visual representation
                        for (Map.Entry<String, List<Course>> entry : coursesByLevel.entrySet()) {
                            String level = entry.getKey();
                            List<Course> courses = entry.getValue();


                            // Create horizontal layout for courses
                            LinearLayout levelLayout = new LinearLayout(requireContext());
                            levelLayout.setOrientation(LinearLayout.HORIZONTAL);
                            levelLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            levelLayout.setPadding(16, 16, 16, 16);
    
                            // Add course blocks
                            for (Course course : courses) {
                                boolean isCompleted = Boolean.TRUE.equals(courseCompletionStatus.get(course.getCourseCode()));
    
                                // Create course block container
                                FrameLayout courseContainer = new FrameLayout(requireContext());
                                FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT
                                );
                                containerParams.setMargins(8, 8, 8, 8);
                                courseContainer.setLayoutParams(containerParams);
    
                                // Create course block view
                                TextView courseBlock = new TextView(requireContext());
                                courseBlock.setText(course.getCourseCode());
                                courseBlock.setGravity(Gravity.CENTER);
                                courseBlock.setTextColor(Color.WHITE);
                                courseBlock.setBackgroundResource(R.drawable.course_block_bg);
                                courseBlock.setPadding(16, 16, 16, 16);
                                courseBlock.setTextSize(14);
                                courseBlock.setTypeface(null, Typeface.BOLD);
    
                                // Set background based on completion status
                                GradientDrawable bgShape = (GradientDrawable) courseBlock.getBackground();
                                bgShape.setColor(isCompleted ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));
                                bgShape.setStroke(2, isCompleted ? Color.parseColor("#388E3C") : Color.parseColor("#D32F2F"));
                                
                                // Add lock icon for incomplete prerequisites
                                if (!isCompleted) {
                                    TextView lockIcon = new TextView(requireContext());
                                    lockIcon.setText("🔒");
                                    lockIcon.setTextSize(12);
                                    FrameLayout.LayoutParams lockParams = new FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.WRAP_CONTENT,
                                            FrameLayout.LayoutParams.WRAP_CONTENT,
                                            Gravity.TOP | Gravity.END
                                    );
                                    lockIcon.setLayoutParams(lockParams);
                                    courseContainer.addView(lockIcon);
                                }
    
                                courseContainer.addView(courseBlock);
                                courseViews.put(course.getCourseCode(), courseContainer);
                                levelLayout.addView(courseContainer);
                            }
    
                            parentLayout.addView(levelLayout);
                        }
    
                        // Add a custom view for drawing connections
                        PrerequisiteConnectorView connectorView = new PrerequisiteConnectorView(
                                requireContext(), 
                                parentLayout, 
                                courseViews, 
                                prerequisitesMap,
                                courseCompletionStatus
                        );
                        parentLayout.addView(connectorView);
    
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("CoursesFragment", "Error parsing completed courses", e);
                    }
                },
                error -> {
                    Log.e("CoursesFragment", "Error fetching completed courses", error);
                    // Fallback - show graph without completion status
                    showBasicPrerequisiteGraph(parentLayout, programCourses);
                }
        );
    
        requestQueue.add(completedCoursesRequest);
    
        scrollView.addView(parentLayout);
        graphContainer.addView(scrollView);
    }
    
    private String getCourseLevel(String courseCode) {
        // Special case for CS001
        if (courseCode.equals("CS001")) {
            return "200";
        }
        
        // Regular course codes
        if (courseCode.length() >= 3 && Character.isDigit(courseCode.charAt(2))) {
            return courseCode.charAt(2) + "00";
        }
        return "Unknown";
    }
    
    private void showBasicPrerequisiteGraph(LinearLayout parentLayout, List<Course> programCourses) {
        // Basic implementation without completion status
        Map<String, List<Course>> coursesByLevel = new TreeMap<>();
        Map<String, List<String>> prerequisitesMap = new HashMap<>();
        Map<String, View> courseViews = new HashMap<>();
    
        // Organize courses
        for (Course course : programCourses) {
            String level = getCourseLevel(course.getCourseCode());
            
            if (!course.getPreRequisite().equalsIgnoreCase("none")) {
                prerequisitesMap.put(course.getCourseCode(), 
                    Arrays.asList(course.getPreRequisite().split("\\s*,\\s*")));
            }
            
            coursesByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(course);
        }
    
        // Create visual representation
        for (Map.Entry<String, List<Course>> entry : coursesByLevel.entrySet()) {
            String level = entry.getKey();
            List<Course> courses = entry.getValue();
    
            // Add level title
            TextView levelTitle = new TextView(requireContext());
            levelTitle.setTextSize(18);
            levelTitle.setTypeface(null, Typeface.BOLD);
            levelTitle.setPadding(0, 16, 0, 8);
            parentLayout.addView(levelTitle);
    
            // Create horizontal layout
            LinearLayout levelLayout = new LinearLayout(requireContext());
            levelLayout.setOrientation(LinearLayout.HORIZONTAL);
            levelLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            levelLayout.setPadding(0, 8, 0, 16);
    
            // Add course blocks
            for (Course course : courses) {
                FrameLayout courseContainer = new FrameLayout(requireContext());
                FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                );
                containerParams.setMargins(8, 8, 8, 8);
                courseContainer.setLayoutParams(containerParams);
    
                TextView courseBlock = new TextView(requireContext());
                courseBlock.setText(course.getCourseCode());
                courseBlock.setGravity(Gravity.CENTER);
                courseBlock.setTextColor(Color.WHITE);
                courseBlock.setBackgroundResource(R.drawable.course_block_bg);
                courseBlock.setPadding(16, 16, 16, 16);
                courseBlock.setTextSize(14);
                courseBlock.setTypeface(null, Typeface.BOLD);
    
                courseContainer.addView(courseBlock);
                courseViews.put(course.getCourseCode(), courseContainer);
                levelLayout.addView(courseContainer);
            }
    
            parentLayout.addView(levelLayout);
        }
    
        // Add connector view
        PrerequisiteConnectorView connectorView = new PrerequisiteConnectorView(
                requireContext(), 
                parentLayout, 
                courseViews, 
                prerequisitesMap,
                new HashMap<>() // Empty completion status map
        );
        parentLayout.addView(connectorView);
    }
    
    // Custom View for drawing connections
    private static class PrerequisiteConnectorView extends View {
        private final LinearLayout parentLayout;
        private final Map<String, View> courseViews;
        private final Map<String, List<String>> prerequisitesMap;
        private final Map<String, Boolean> courseCompletionStatus;
        private final Paint linePaint;
        private final Paint arrowPaint;
    
        public PrerequisiteConnectorView(Context context, 
                                       LinearLayout parentLayout, 
                                       Map<String, View> courseViews, 
                                       Map<String, List<String>> prerequisitesMap,
                                       Map<String, Boolean> courseCompletionStatus) {
            super(context);
            this.parentLayout = parentLayout;
            this.courseViews = courseViews;
            this.prerequisitesMap = prerequisitesMap;
            this.courseCompletionStatus = courseCompletionStatus;
    
            // Initialize paints
            linePaint = new Paint();
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(4);
            
            arrowPaint = new Paint();
            arrowPaint.setStyle(Paint.Style.FILL);
        }
    
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            
            // Draw all prerequisite connections
            for (Map.Entry<String, List<String>> entry : prerequisitesMap.entrySet()) {
                String targetCourse = entry.getKey();
                View targetView = courseViews.get(targetCourse);
                
                if (targetView == null) continue;
                
                for (String sourceCourse : entry.getValue()) {
                    View sourceView = courseViews.get(sourceCourse.trim());
                    if (sourceView == null) continue;
                    
                    // Get positions
                    int[] sourcePos = new int[2];
                    sourceView.getLocationOnScreen(sourcePos);
                    int[] targetPos = new int[2];
                    targetView.getLocationOnScreen(targetPos);
                    
                    // Convert to canvas coordinates
                    int[] parentPos = new int[2];
                    parentLayout.getLocationOnScreen(parentPos);
                    sourcePos[0] -= parentPos[0];
                    sourcePos[1] -= parentPos[1];
                    targetPos[0] -= parentPos[0];
                    targetPos[1] -= parentPos[1];
                    
                    // Calculate connection points
                    int sourceX = sourcePos[0] + sourceView.getWidth() / 2;
                    int sourceY = sourcePos[1] + sourceView.getHeight();
                    int targetX = targetPos[0] + targetView.getWidth() / 2;
                    int targetY = targetPos[1];
                    
                    // Check if prerequisite is completed
                    boolean isCompleted = Boolean.TRUE.equals(courseCompletionStatus.get(sourceCourse.trim()));
                    linePaint.setColor(isCompleted ? Color.GREEN : Color.BLUE);
                    arrowPaint.setColor(isCompleted ? Color.GREEN : Color.BLUE);
                    
                    // Draw connecting line
                    Path path = new Path();
                    path.moveTo(sourceX, sourceY);
                    path.lineTo(sourceX, sourceY + 20); // Vertical down from source
                    path.lineTo(targetX, targetY - 20);  // Horizontal to target
                    path.lineTo(targetX, targetY);      // Vertical up to target
                    canvas.drawPath(path, linePaint);
                    
                    // Draw arrowhead
                    Path arrow = new Path();
                    arrow.moveTo(targetX - 8, targetY - 8);
                    arrow.lineTo(targetX, targetY);
                    arrow.lineTo(targetX + 8, targetY - 8);
                    canvas.drawPath(arrow, arrowPaint);
                }
            }
        }
    }
}