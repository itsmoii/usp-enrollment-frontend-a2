package com.example.uspenrolme.student;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PathMeasure;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.models.Course;
import com.android.volley.toolbox.JsonObjectRequest; // For JsonObjectRequest
import java.util.HashSet; // For HashSet
import java.util.Set; // For Set


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PrerequisiteFragment extends Fragment {

    private RequestQueue requestQueue;
    private FrameLayout graphContainer;
    private PrerequisiteConnectorView connectorView;
    private SharedPreference sharedPref;
    private TextView programTitleView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FrameLayout rootLayout = new FrameLayout(requireContext());
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout contentLayout = new LinearLayout(requireContext());
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentLayout.setPadding(16, 16, 16, 16);

        // Add program title view
        programTitleView = new TextView(requireContext());
        programTitleView.setTextSize(20);
        programTitleView.setTypeface(null, Typeface.BOLD);
        programTitleView.setGravity(Gravity.CENTER);
        programTitleView.setPadding(0, 16, 0, 16);
        contentLayout.addView(programTitleView);

        // Create a container for the graph
        graphContainer = new FrameLayout(requireContext());
        graphContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        contentLayout.addView(graphContainer);
        scrollView.addView(contentLayout);
        rootLayout.addView(scrollView);

        // Initialize the request queue
        requestQueue = Volley.newRequestQueue(requireContext());

        // Initialize shared preferences
        sharedPref = new SharedPreference(requireContext());

        // Fetch and build the prerequisite graph
        fetchProgramName();
        fetchCoursePrerequisites();

        return rootLayout;
    }

    private void fetchProgramName() {
        String studentId = sharedPref.getValue_string("userID");
        if (studentId == null || studentId.isEmpty()) {
            Log.e("PrerequisiteFragment", "Error: Student ID is missing.");
            programTitleView.setText("Student ID Missing");
            return;
        }
    
        // Use 10.0.2.2 for localhost on Android emulator
        String programsUrl = "http://10.0.2.2:5000/api/programs?studentId=" + studentId;
    
        JsonObjectRequest programRequest = new JsonObjectRequest(Request.Method.GET, programsUrl, null,
                response -> {
                    try {
                        String programName = response.optString("program_name", "Unknown Program");
                        programTitleView.setText(programName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        programTitleView.setText("Program Name Not Available");
                    }
                },
                error -> {
                    Log.e("PrerequisiteFragment", "Error fetching program name: " + error.getMessage());
                    programTitleView.setText("Program Name Not Available");
                });
    
        requestQueue.add(programRequest);
    }

    private void fetchCoursePrerequisites() {
        String studentId = sharedPref.getValue_string("userID");
        String prerequisitesUrl = "http://10.0.2.2:5000/api/course-prerequisites";
        String programCoursesUrl = "http://10.0.2.2:5000/api/program-courses?studentId=" + studentId;
        String completedCoursesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;
        String activeRegistrationsUrl = "http://10.0.2.2:5000/api/active-registrations?studentId=" + studentId;
    
        // Fetch program-specific courses
        JsonArrayRequest programCoursesRequest = new JsonArrayRequest(Request.Method.GET, programCoursesUrl, null,
                programCoursesResponse -> {
                    JsonArrayRequest completedCoursesRequest = new JsonArrayRequest(Request.Method.GET, completedCoursesUrl, null,
                            completedCoursesResponse -> {
                                JsonArrayRequest activeRegistrationsRequest = new JsonArrayRequest(Request.Method.GET, activeRegistrationsUrl, null,
                                        activeRegistrationsResponse -> {
                                            try {
                                                List<Course> programCourses = parseCourses(programCoursesResponse);
                                                List<Course> completedCourses = parseCourses(completedCoursesResponse);
                                                List<Course> activeRegistrations = parseCourses(activeRegistrationsResponse);
    
                                                // Fetch all courses with prerequisites
                                                JsonArrayRequest prerequisitesRequest = new JsonArrayRequest(Request.Method.GET, prerequisitesUrl, null,
                                                        prerequisitesResponse -> {
                                                            try {
                                                                List<Course> allCourses = parseCourses(prerequisitesResponse);
    
                                                                // Filter courses to include only those in the student's program
                                                                List<Course> filteredCourses = new ArrayList<>();
                                                                for (Course course : allCourses) {
                                                                    for (Course programCourse : programCourses) {
                                                                        if (course.getCourseCode().equals(programCourse.getCourseCode())) {
                                                                            filteredCourses.add(course);
                                                                            break;
                                                                        }
                                                                    }
                                                                }
    
                                                                // Build the prerequisite graph with filtered courses
                                                                buildPrerequisiteGraph(filteredCourses, completedCourses, activeRegistrations);
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                                Log.e("PrerequisiteFragment", "Error parsing course prerequisites: " + e.getMessage());
                                                            }
                                                        },
                                                        error -> Log.e("PrerequisiteFragment", "Error fetching course prerequisites: " + error.getMessage()));
    
                                                requestQueue.add(prerequisitesRequest);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                Log.e("PrerequisiteFragment", "Error parsing courses data: " + e.getMessage());
                                            }
                                        },
                                        error -> Log.e("PrerequisiteFragment", "Error fetching active registrations: " + error.getMessage()));
    
                                requestQueue.add(activeRegistrationsRequest);
                            },
                            error -> Log.e("PrerequisiteFragment", "Error fetching completed courses: " + error.getMessage()));
    
                    requestQueue.add(completedCoursesRequest);
                },
                error -> Log.e("PrerequisiteFragment", "Error fetching program courses: " + error.getMessage()));
    
        requestQueue.add(programCoursesRequest);
    }

    private String getStudentId() {
        SharedPreference sharedPref = new SharedPreference(requireContext());
        return sharedPref.getValue_string("userID");
    }

    private List<Course> parseCourses(JSONArray response) throws JSONException {
        List<Course> courses = new ArrayList<>();
        Log.d("PrerequisiteFragment", "API Response: " + response.toString());

        for (int i = 0; i < response.length(); i++) {
            JSONObject courseObj = response.getJSONObject(i);

            String courseCode = courseObj.optString("course_code", "Unknown");
            String title = courseObj.optString("course_name", "No Title");
            String campus = courseObj.optString("course_campus", "Unknown");
            String mode = courseObj.optString("course_mode", "Unknown");
            String semester = courseObj.optString("semester", "Unknown");
            String preRequisite = courseObj.optString("pre_requisite", "None");

            Log.d("PrerequisiteFragment", "Parsed Course: " + courseCode + ", Prerequisite: " + preRequisite);

            courses.add(new Course(courseCode, title, campus, mode, semester, preRequisite));
        }
        return courses;
    }

    private void buildPrerequisiteGraph(List<Course> programCourses, List<Course> completedCourses, List<Course> activeRegistrations) {
        Map<String, List<Course>> coursesByLevel = new TreeMap<>();
        Map<String, List<String>> prerequisitesMap = new HashMap<>();
        Map<String, View> courseViews = new HashMap<>();
        Set<String> activeRegistrationCodes = new HashSet<>();
    
        // Map completed courses with grades
        Map<String, String> completedCourseGrades = new HashMap<>();
        for (Course course : completedCourses) {
            completedCourseGrades.put(course.getCourseCode(), course.getGrade());
        }
        Log.d("PrerequisiteFragment", "Completed Course Grades: " + completedCourseGrades.toString());
    
        // Map active registration courses
        for (Course course : activeRegistrations) {
            activeRegistrationCodes.add(course.getCourseCode());
        }
    
        // Define passing grades
        Set<String> passingGrades = new HashSet<>(Arrays.asList("A+", "A", "B+", "B", "C+", "C", "S"));
    
        // Organize courses by level and map prerequisites
        for (Course course : programCourses) {
            String level = getCourseLevel(course.getCourseCode());
    
            // Map prerequisites
            if (!course.getPreRequisite().equalsIgnoreCase("none")) {
                prerequisitesMap.put(course.getCourseCode(),
                        Arrays.asList(course.getPreRequisite().split("\\s*,\\s*")));
            } else {
                prerequisitesMap.put(course.getCourseCode(), new ArrayList<>());
            }
    
            // Assign grades to program courses
            if (completedCourseGrades.containsKey(course.getCourseCode())) {
                course.setGrade(completedCourseGrades.get(course.getCourseCode()));
                Log.d("PrerequisiteFragment", "Set grade for course: " + course.getCourseCode() + ", Grade: " + course.getGrade());
            }
    
            // Group by level
            coursesByLevel.computeIfAbsent(level, k -> new ArrayList<>()).add(course);
        }
    
        // Create a container for course levels
        LinearLayout levelsContainer = new LinearLayout(requireContext());
        levelsContainer.setOrientation(LinearLayout.VERTICAL);
        levelsContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    
        // Create visual representation
        for (Map.Entry<String, List<Course>> entry : coursesByLevel.entrySet()) {
            String level = entry.getKey();
            List<Course> courses = entry.getValue();
    
            // Add level label
            TextView levelLabel = new TextView(requireContext());
            levelLabel.setText("Level " + level);
            levelLabel.setTextSize(16);
            levelLabel.setTypeface(null, Typeface.BOLD);
            levelLabel.setPadding(28, 28, 28, 28);
            levelsContainer.addView(levelLabel);
    
            // Create horizontal layout for courses
            LinearLayout levelLayout = new LinearLayout(requireContext());
            levelLayout.setOrientation(LinearLayout.HORIZONTAL);
            levelLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            levelLayout.setPadding(16, 8, 16, 16);
            levelLayout.setGravity(Gravity.CENTER_HORIZONTAL);
    
            // Add course blocks
            for (Course course : courses) {
                // Create course block container
                LinearLayout courseContainer = new LinearLayout(requireContext());
                courseContainer.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                        dpToPx(120), // Fixed width for better alignment
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                containerParams.setMargins(8, 8, 8, 8);
                courseContainer.setLayoutParams(containerParams);
                courseContainer.setGravity(Gravity.CENTER);
    
                // Create course code view
                TextView courseCodeView = new TextView(requireContext());
                courseCodeView.setText(course.getCourseCode());
                courseCodeView.setGravity(Gravity.CENTER);
                courseCodeView.setTextColor(Color.WHITE);
                courseCodeView.setPadding(8, 8, 8, 8);
                courseCodeView.setTextSize(14);
                courseCodeView.setTypeface(null, Typeface.BOLD);
    
                // Create course title view
                TextView courseTitleView = new TextView(requireContext());
                String title = course.getTitle();
                if (title.length() > 20) {
                    title = title.substring(0, 17) + "...";
                }
                courseTitleView.setText(title);
                courseTitleView.setGravity(Gravity.CENTER);
                courseTitleView.setTextColor(Color.WHITE);
                courseTitleView.setPadding(8, 0, 8, 8);
                courseTitleView.setTextSize(12);
                courseTitleView.setMaxLines(2);
    
                // Set background for the container
                GradientDrawable shape = new GradientDrawable();
                shape.setCornerRadius(dpToPx(8));
    
                String grade = course.getGrade();
                if (grade != null && passingGrades.contains(grade)) {
                    Log.d("PrerequisiteFragment", "Course Passed: " + course.getCourseCode());
                    shape.setColor(Color.parseColor("#4CAF50")); // Green for passed courses
                } else if (activeRegistrationCodes.contains(course.getCourseCode())) {
                    Log.d("PrerequisiteFragment", "Course Active Registration: " + course.getCourseCode());
                    shape.setColor(Color.parseColor("#2196F3")); // Blue for active registration
                } else {
                    Log.d("PrerequisiteFragment", "Course Not Registered or Failed: " + course.getCourseCode());
                    shape.setColor(Color.parseColor("#F44336")); // Red for not registered
                }
                courseContainer.setBackground(shape);
    
                courseContainer.addView(courseCodeView);
                courseContainer.addView(courseTitleView);
                courseViews.put(course.getCourseCode(), courseContainer);
                levelLayout.addView(courseContainer);
            }
    
            levelsContainer.addView(levelLayout);
        }
    
        // Add the connector view
        connectorView = new PrerequisiteConnectorView(
                requireContext(),
                courseViews,
                prerequisitesMap,
                sharedPref // Pass sharedPref to the connector view
        );
        connectorView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
    
        // Add views to container
        graphContainer.addView(levelsContainer);
        graphContainer.addView(connectorView);
    
        // Wait for layout to complete before drawing connections
        graphContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                graphContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                connectorView.invalidate();
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private boolean isCoursePassed(Course course) {
        // Implement your logic to check if course is passed
        return false;
    }

    private boolean isCourseRegistered(Course course) {
        // Implement your logic to check if course is registered
        return false;
    }

    private String getCourseLevel(String courseCode) {
        if (courseCode.equals("CS001")) {
            return "200";
        }
        if (courseCode.length() >= 3 && Character.isDigit(courseCode.charAt(2))) {
            return courseCode.charAt(2) + "00";
        }
        return "Unknown";
    }

    private static class PrerequisiteConnectorView extends View {
        private final Map<String, View> courseViews;
        private final Map<String, List<String>> prerequisitesMap;
        private final Paint linePaint;
        private final Paint arrowPaint;
        private final SharedPreference sharedPref; // Add sharedPref as a member variable

    
        public PrerequisiteConnectorView(Context context,
                                         Map<String, View> courseViews,
                                         Map<String, List<String>> prerequisitesMap, 
                                         SharedPreference sharedPref) {
            super(context);
            this.courseViews = courseViews;
            this.prerequisitesMap = prerequisitesMap;
            this.sharedPref = sharedPref; 
    
            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(dpToPx(2));
            linePaint.setColor(Color.BLUE); // Default color for lines
    
            arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setStyle(Paint.Style.FILL);
            arrowPaint.setColor(Color.BLUE); // Default color for arrowheads
        }
    
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
    
            for (Map.Entry<String, List<String>> entry : prerequisitesMap.entrySet()) {
                String targetCourse = entry.getKey();
                View targetView = courseViews.get(targetCourse);
    
                if (targetView == null) continue;
    
                for (String sourceCourse : entry.getValue()) {
                    View sourceView = courseViews.get(sourceCourse.trim());
                    if (sourceView == null) continue;
    
                    // Get positions relative to this view
                    int[] sourcePos = new int[2];
                    sourceView.getLocationOnScreen(sourcePos);
                    int[] targetPos = new int[2];
                    targetView.getLocationOnScreen(targetPos);
                    int[] myPos = new int[2];
                    this.getLocationOnScreen(myPos);
    
                    // Calculate relative positions
                    float sourceX = sourcePos[0] - myPos[0] + sourceView.getWidth() / 2f;
                    float sourceY = sourcePos[1] - myPos[1] + sourceView.getHeight();
                    float targetX = targetPos[0] - myPos[0] + targetView.getWidth() / 2f;
                    float targetY = targetPos[1] - myPos[1];
    
                    // Set line and arrow colors dynamically
                    if (isCoursePassed(sourceCourse)) {
                        linePaint.setColor(Color.GREEN); // Green for completed prerequisites
                        arrowPaint.setColor(Color.GREEN);
                    } else {
                        linePaint.setColor(Color.BLUE); // Blue for incomplete prerequisites
                        arrowPaint.setColor(Color.BLUE);
                    }
    
                    // Draw a curved line between source and target
                    drawCurvedArrow(canvas, sourceX, sourceY, targetX, targetY);
                }
            }
        }
    
        private void drawCurvedArrow(Canvas canvas, float startX, float startY, float endX, float endY) {
            Path path = new Path();
        
            if (Math.abs(startY - endY) < dpToPx(10)) {
                // Case: Same level courses - connect top to top
                float verticalOffset = dpToPx(10); // Offset to draw the line slightly above the courses
        
                // Move to the starting point (top of the source course)
                path.moveTo(startX, startY - verticalOffset);
        
                // Draw a straight horizontal line to the target course
                path.lineTo(endX, endY - verticalOffset);
        
                // Draw small vertical lines to indicate connection points
                canvas.drawLine(startX, startY, startX, startY - verticalOffset, linePaint);
                canvas.drawLine(endX, endY, endX, endY - verticalOffset, linePaint);
            } else if (startY < endY) {
                // Case: Source is above target - create smooth curve
                float midY = (startY + endY) / 2;
                path.moveTo(startX, startY);
                path.cubicTo(
                    startX, midY,        // Control point 1
                    endX, midY,         // Control point 2
                    endX, endY          // End point
                );
            } else {
                // Case: Source is below target - create S-curve to avoid crossing
                float offsetX = dpToPx(60); // Horizontal offset to avoid crossing
                path.moveTo(startX, startY);
                path.cubicTo(
                    startX + offsetX, startY,           // First curve right
                    startX + offsetX, endY - dpToPx(30), // Then down
                    endX, endY                         // Then to target
                );
            }
        
            // Draw the path
            canvas.drawPath(path, linePaint);
        
            // Draw arrowhead
            drawArrowhead(canvas, endX, endY, path);
        }
        
        private void drawArrowhead(Canvas canvas, float x, float y, Path path) {
            PathMeasure pm = new PathMeasure(path, false);
            float[] pos = new float[2];
            float[] tan = new float[2];
            pm.getPosTan(pm.getLength() - dpToPx(5), pos, tan); // Get position near end
            
            float angle = (float) Math.atan2(tan[1], tan[0]);
            float arrowSize = dpToPx(8);
            
            Path arrowPath = new Path();
            arrowPath.moveTo(x, y);
            arrowPath.lineTo(
                x - arrowSize * (float) Math.cos(angle - Math.PI / 6),
                y - arrowSize * (float) Math.sin(angle - Math.PI / 6)
            );
            arrowPath.lineTo(
                x - arrowSize * (float) Math.cos(angle + Math.PI / 6),
                y - arrowSize * (float) Math.sin(angle + Math.PI / 6)
            );
            arrowPath.close();
            
            canvas.drawPath(arrowPath, arrowPaint);
        }
    
        private boolean isCoursePassed(String courseCode) {
            // Replace with actual logic to check if the course is passed
            return sharedPref.getValue_bool(courseCode + "_passed");
        }
    
        private int dpToPx(int dp) {
            float density = getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }
    }
}