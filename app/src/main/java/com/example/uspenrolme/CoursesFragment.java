package com.example.uspenrolme;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.adapters.CourseAdapter;
import com.example.uspenrolme.models.Course;
import com.example.uspenrolme.UtilityService.SharedPreference;

import com.google.android.material.card.MaterialCardView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CoursesFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private LinearLayout coursesContainer;
    private Button registerButton;
    private ProgressBar progressBar;
    private List<Course> selectedCourses;
    private RequestQueue requestQueue;
    private Map<String, CourseAdapter> adaptersByLevel;
    private SharedPreference sharedPref;

    public CoursesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        // Initialize UI components
        coursesContainer = view.findViewById(R.id.coursesContainer);
        registerButton = view.findViewById(R.id.registerButton);
        progressBar = view.findViewById(R.id.progressBar); // Ensure this is initialized
        selectedCourses = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(requireContext());
        adaptersByLevel = new HashMap<>();
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
                                                List<Course> programCourses = parseCourses(programCoursesResponse);
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
                                        error -> handleError(error));
    
                                requestQueue.add(activeRegistrationsRequest);
                            },
                            error -> handleError(error));
    
                    requestQueue.add(completedCoursesRequest);
                },
                error -> handleError(error));
    
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
            String level = course.getCourseCode().substring(2, 3) + "00";
    
            // Special case for CS001 (categorize under Year 2 Courses)
            if (course.getCourseCode().equals("CS001")) {
                level = "200";
            }
    
            // Skip courses that are already completed or registered
            if (completedCourseCodes.contains(course.getCourseCode()) || activeRegistrationCodes.contains(course.getCourseCode())) {
                continue;
            }
    
            // Add course to the appropriate level group
            if (!coursesByLevel.containsKey(level)) {
                coursesByLevel.put(level, new ArrayList<>());
            }
            coursesByLevel.get(level).add(course);
        }
    
        // Dynamically create RecyclerViews for each level
        for (Map.Entry<String, List<Course>> entry : coursesByLevel.entrySet()) {
            String level = entry.getKey();
            List<Course> courses = entry.getValue();
    
            addLevelRecyclerView(level, courses);
        }
    }

    private void handleError(VolleyError error) {
        Log.e("CoursesFragment", "Error fetching courses: " + error.getMessage());
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "utf-8");
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

    private void setupPrerequisiteGraph() {
        String studentId = sharedPref.getValue_string("userID");
        String programCoursesUrl = "http://10.0.2.2:5000/api/program-courses?studentId=" + studentId;
        String completedCoursesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;
        String prerequisitesUrl = "http://10.0.2.2:5000/api/course-prerequisites";
    
        FrameLayout graphContainer = requireView().findViewById(R.id.prerequisiteGraphContainer);
    
        // Show progress bar while loading
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        
    
        // Fetch program courses, completed courses, and prerequisites
        JsonArrayRequest programCoursesRequest = new JsonArrayRequest(Request.Method.GET, programCoursesUrl, null,
                programCoursesResponse -> {
                    JsonArrayRequest completedCoursesRequest = new JsonArrayRequest(Request.Method.GET, completedCoursesUrl, null,
                            completedCoursesResponse -> {
                                JsonArrayRequest prerequisitesRequest = new JsonArrayRequest(Request.Method.GET, prerequisitesUrl, null,
                                        prerequisitesResponse -> {
                                            try {
                                                // Parse responses
                                                List<Course> programCourses = parseCourses(programCoursesResponse);
                                                List<Course> completedCourses = parseCourses(completedCoursesResponse);
                                                List<Course> allCourses = parseCourses(prerequisitesResponse);
    
                                                Log.d("CoursesFragment", "Program Courses: " + programCourses.size());
                                                Log.d("CoursesFragment", "Completed Courses: " + completedCourses.size());
                                                Log.d("CoursesFragment", "All Courses: " + allCourses.size());
                                                for (Course course : programCourses) {
                                                    Log.d("CoursesFragment", "Program Course: " + course.getCourseCode() + ", Prerequisite: " + course.getPreRequisite());
                                                }
    
                                                // Build the graph
                                                buildPrerequisiteGraph(programCourses, completedCourses, allCourses, graphContainer);
    
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                                Log.e("CoursesFragment", "Error parsing courses data: " + e.getMessage());
                                            }
                                            if (progressBar != null) {
                                                progressBar.setVisibility(View.GONE);
                                            }
                                        },
                                        error -> handleError(error));
    
                                requestQueue.add(prerequisitesRequest);
                            },
                            error -> handleError(error));
    
                    requestQueue.add(completedCoursesRequest);
                },
                error -> handleError(error));
    
        requestQueue.add(programCoursesRequest);
    }
    
    private void buildPrerequisiteGraph(List<Course> programCourses, List<Course> completedCourses, List<Course> allCourses, FrameLayout graphContainer) {

        Log.d("GraphDebug", "Program courses count: " + programCourses.size());
        Log.d("GraphDebug", "Completed courses count: " + completedCourses.size());
        Log.d("GraphDebug", "All courses count: " + allCourses.size());


        GraphView graphView = new GraphView(requireContext());
        graphView.getViewport().setScalable(true); // Enable zooming
        graphView.getViewport().setScrollable(true); // Enable scrolling
    
        // Create nodes and edges
        List<DataPoint> nodes = new ArrayList<>();
        List<DataPoint[]> edges = new ArrayList<>();
        Map<String, DataPoint> nodePositions = new HashMap<>();
        Set<String> completedCourseCodes = new HashSet<>();
    
        // Populate completed courses
        for (Course course : completedCourses) {
            completedCourseCodes.add(course.getCourseCode());
        }
    
        int x = 0, y = 0; // Initial positions for nodes
        int spacingX = 200; // Horizontal spacing
        int spacingY = 150; // Vertical spacing
        int maxColumns = 5; // Maximum nodes per row
    
        for (Course course : programCourses) {
            String courseCode = course.getCourseCode();
            String prerequisites = course.getPreRequisite();
    
            // Create a node for the course
            DataPoint node = new DataPoint(x, y);
            nodes.add(node);
            nodePositions.put(courseCode, node);
    
            // Adjust positions for the next node
            x += spacingX;
            if (x >= maxColumns * spacingX) {
                x = 0;
                y += spacingY;
            }
    
            // Create edges for prerequisites
            if (!prerequisites.equalsIgnoreCase("None") && !prerequisites.isEmpty()) {
                String[] prereqArray = prerequisites.split(",");
                for (String prereq : prereqArray) {
                    prereq = prereq.trim();
                    if (!prereq.isEmpty() && nodePositions.containsKey(prereq)) {
                        edges.add(new DataPoint[]{nodePositions.get(prereq), node});
                    } else {
                        Log.w("CoursesFragment", "Prerequisite not found in node positions: " + prereq);
                    }
                }
            }

            if (programCourses.isEmpty() || allCourses.isEmpty()) {
                // Show placeholder text
                TextView placeholder = new TextView(getContext());
                placeholder.setText("No prerequisite data available");
                placeholder.setGravity(Gravity.CENTER);
                graphContainer.removeAllViews();
                graphContainer.addView(placeholder);
                return;
            }
    
            // Special case for CS400
            if (courseCode.equals("CS400")) {
                boolean allLevelsCompleted = true;
                for (String level : new String[]{"100", "200", "300", "400"}) {
                    boolean levelCompleted = programCourses.stream()
                        .filter(c -> c.getCourseCode().startsWith(level))
                        .allMatch(c -> completedCourseCodes.contains(c.getCourseCode()));
                    if (!levelCompleted) {
                        allLevelsCompleted = false;
                        break;
                    }
                }
                if (allLevelsCompleted && completedCourseCodes.contains("CS001")) {
                    edges.add(new DataPoint[]{nodePositions.get("CS001"), node});
                } else {
                    Log.w("CoursesFragment", "CS400 prerequisites not met.");
                }
            }
        }
    
        Log.d("CoursesFragment", "Nodes created: " + nodes.size());
        for (DataPoint node : nodes) {
            Log.d("CoursesFragment", "Node: " + node.toString());
        }
    
        Log.d("CoursesFragment", "Edges created: " + edges.size());
        for (DataPoint[] edge : edges) {
            Log.d("CoursesFragment", "Edge: " + edge[0].toString() + " -> " + edge[1].toString());
        }
    
        // Add nodes to the graph
        PointsGraphSeries<DataPoint> nodeSeries = new PointsGraphSeries<>(nodes.toArray(new DataPoint[0]));
        nodeSeries.setShape(PointsGraphSeries.Shape.POINT);
        nodeSeries.setColor(Color.BLUE);
    
        // Add edges to the graph
        for (DataPoint[] edge : edges) {
            LineGraphSeries<DataPoint> edgeSeries = new LineGraphSeries<>(edge);
            edgeSeries.setColor(Color.GREEN);
            graphView.addSeries(edgeSeries);
        }
    
        // Add node series last to ensure nodes are on top of edges
        graphView.addSeries(nodeSeries);
    
        // Add the graph to the container
        graphContainer.removeAllViews();
        graphContainer.addView(graphView);
    
        Log.d("CoursesFragment", "Graph rendered successfully");
    }
    
    private void renderGraph(FrameLayout graphContainer, List<DataPoint> nodes, List<DataPoint[]> edges) {
        Log.d("CoursesFragment", "renderGraph called with " + nodes.size() + " nodes and " + edges.size() + " edges");
    
        GraphView graphView = new GraphView(requireContext());
        graphView.getViewport().setScalable(true); // Enable zooming
        graphView.getViewport().setScrollable(true); // Enable scrolling
    
        // Add nodes to the graph
        PointsGraphSeries<DataPoint> nodeSeries = new PointsGraphSeries<>(nodes.toArray(new DataPoint[0]));
        nodeSeries.setShape(PointsGraphSeries.Shape.POINT);
        nodeSeries.setColor(Color.BLUE);
    
        // Add edges to the graph
        LineGraphSeries<DataPoint> edgeSeries = new LineGraphSeries<>();
        for (DataPoint[] edge : edges) {
            edgeSeries.appendData(edge[0], true, edges.size());
            edgeSeries.appendData(edge[1], true, edges.size());
        }
        edgeSeries.setColor(Color.GREEN);
    
        // Add series to the graph
        graphView.addSeries(nodeSeries);
        graphView.addSeries(edgeSeries);
    
        // Add the graph to the container
        graphContainer.removeAllViews();
        graphContainer.addView(graphView);
    
        Log.d("CoursesFragment", "Graph rendered successfully");
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
        // Handle course registration logic here
        for (Course course : selectedCourses) {
            Log.d("CoursesFragment", "Registering course: " + course.getCourseCode());
        }
    }

}