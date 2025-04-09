package com.example.uspenrolme.student;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.models.Course;

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

        // Fetch and build the prerequisite graph
        fetchCoursePrerequisites();

        return rootLayout;
    }

    private void fetchCoursePrerequisites() {
        String studentId = getStudentId(); // Dynamically fetch the student ID
        String prerequisitesUrl = "http://10.0.2.2:5000/api/course-prerequisites";
        String programCoursesUrl = "http://10.0.2.2:5000/api/program-courses?studentId=" + studentId;
    
        // Fetch program-specific courses
        JsonArrayRequest programCoursesRequest = new JsonArrayRequest(Request.Method.GET, programCoursesUrl, null,
                programCoursesResponse -> {
                    try {
                        // Parse program-specific courses
                        List<Course> programCourses = parseCourses(programCoursesResponse);
    
                        // Fetch all courses with prerequisites
                        JsonArrayRequest prerequisitesRequest = new JsonArrayRequest(Request.Method.GET, prerequisitesUrl, null,
                                prerequisitesResponse -> {
                                    try {
                                        // Parse all courses with prerequisites
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
                                        buildPrerequisiteGraph(filteredCourses);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        Log.e("PrerequisiteFragment", "Error parsing course prerequisites: " + e.getMessage());
                                    }
                                },
                                error -> Log.e("PrerequisiteFragment", "Error fetching course prerequisites: " + error.getMessage()));
    
                        requestQueue.add(prerequisitesRequest);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("PrerequisiteFragment", "Error parsing program courses: " + e.getMessage());
                    }
                },
                error -> Log.e("PrerequisiteFragment", "Error fetching program courses: " + error.getMessage()));
    
        requestQueue.add(programCoursesRequest);
    }
    
    private String getStudentId() {
        // Fetch the student ID from shared preferences
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

    private void buildPrerequisiteGraph(List<Course> programCourses) {
        Map<String, List<Course>> coursesByLevel = new TreeMap<>();
        Map<String, List<String>> prerequisitesMap = new HashMap<>();
        Map<String, View> courseViews = new HashMap<>();

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
            levelLabel.setPadding(0, 16, 0, 8);
            levelsContainer.addView(levelLabel);

            // Create horizontal layout for courses
            LinearLayout levelLayout = new LinearLayout(requireContext());
            levelLayout.setOrientation(LinearLayout.HORIZONTAL);
            levelLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            levelLayout.setPadding(16, 8, 16, 16);

            // Add course blocks
            for (Course course : courses) {
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

                courseContainer.addView(courseBlock);
                courseViews.put(course.getCourseCode(), courseContainer);
                levelLayout.addView(courseContainer);
            }

            levelsContainer.addView(levelLayout);
        }

        // Add the connector view
        connectorView = new PrerequisiteConnectorView(
                requireContext(),
                courseViews,
                prerequisitesMap
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
    
        public PrerequisiteConnectorView(Context context,
                                         Map<String, View> courseViews,
                                         Map<String, List<String>> prerequisitesMap) {
            super(context);
            this.courseViews = courseViews;
            this.prerequisitesMap = prerequisitesMap;
    
            linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(4);
            linePaint.setColor(Color.parseColor("#6200EE"));
    
            arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setStyle(Paint.Style.FILL);
            arrowPaint.setColor(Color.parseColor("#6200EE"));
    
            setWillNotDraw(false);
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
    
                    int[] sourcePos = new int[2];
                    sourceView.getLocationInWindow(sourcePos);
                    int[] targetPos = new int[2];
                    targetView.getLocationInWindow(targetPos);
                    int[] myPos = new int[2];
                    getLocationInWindow(myPos);
    
                    int sourceX = sourcePos[0] - myPos[0] + sourceView.getWidth() / 2;
                    int sourceY = sourcePos[1] - myPos[1] + sourceView.getHeight();
                    int targetX = targetPos[0] - myPos[0] + targetView.getWidth() / 2;
                    int targetY = targetPos[1] - myPos[1];
    
                    // Draw a curved line between source and target
                    drawCurvedLine(canvas, sourceX, sourceY, targetX, targetY);
    
                    // Draw an arrowhead at the target
                    drawArrow(canvas, sourceX, sourceY, targetX, targetY);
                }
            }
        }
    
        private void drawCurvedLine(Canvas canvas, float startX, float startY, float endX, float endY) {
            Path path = new Path();
            float midX = (startX + endX) / 2;
            float midY = (startY + endY) / 2 - 50; // Add curvature by offsetting the midpoint vertically
    
            path.moveTo(startX, startY);
            path.quadTo(midX, midY, endX, endY); // Use quadratic Bezier curve for smoothness
            canvas.drawPath(path, linePaint);
        }
    
        private void drawArrow(Canvas canvas, float startX, float startY, float endX, float endY) {
            float arrowSize = 15f;
            float angle = (float) Math.atan2(endY - startY, endX - startX);
    
            float x1 = (float) (endX - arrowSize * Math.cos(angle - Math.PI / 6));
            float y1 = (float) (endY - arrowSize * Math.sin(angle - Math.PI / 6));
            float x2 = (float) (endX - arrowSize * Math.cos(angle + Math.PI / 6));
            float y2 = (float) (endY - arrowSize * Math.sin(angle + Math.PI / 6));
    
            Path arrowPath = new Path();
            arrowPath.moveTo(endX, endY);
            arrowPath.lineTo(x1, y1);
            arrowPath.lineTo(x2, y2);
            arrowPath.close();
    
            canvas.drawPath(arrowPath, arrowPaint);
        }
    }
}