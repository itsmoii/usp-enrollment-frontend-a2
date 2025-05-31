package com.example.uspenrolme.student;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import android.graphics.Typeface;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.HoldUtils;
import com.example.uspenrolme.models.Course;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.shared.ErrorFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;

public class ProgramOutlineFragment extends Fragment {

    private LinearLayout programOutlineContainer;
    private SharedPreference sharedPref;
    private RequestQueue requestQueue;

    public ProgramOutlineFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_program_outline, container, false);

        // Initialize UI components
        programOutlineContainer = view.findViewById(R.id.dynamicContentContainer);
        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        String token = sharedPref.getValue_string("token");

        HoldUtils.checkHold(requireContext(), token, "audit", isBlocked -> {
            if(isBlocked){
                showHoldPage();
            } else{

                // Fetch and display the student's program name
                fetchProgramName();

                // Fetch and display the student's profile
                fetchStudentProfile(view);

                // Fetch and process data
                fetchProgramOutlineData();

            }
        });

        return view;
    }

    private void fetchStudentProfile(View view) {
        String studentId = sharedPref.getValue_string("userID");
        String token = sharedPref.getValue_string("token"); // Retrieve the token
        String url = "http://10.0.2.2:5000/api/profile";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d("fetchStudentProfile", "Response: " + response.toString());
                    try {
                        JSONObject userObj = response.getJSONObject("user");
                        JSONObject dataObj = userObj.getJSONObject("data"); // Adjusted to match the correct structure
                        JSONObject studentProfile = dataObj.getJSONObject("studentProfile");

                        TextView studentIdTextView = view.findViewById(R.id.studentIdTextView);
                        TextView studentNameTextView = view.findViewById(R.id.studentNameTextView);
                        TextView emailTextView = view.findViewById(R.id.emailTextView);

                        studentIdTextView.setText(studentProfile.optString("student_id", "N/A"));
                        studentNameTextView.setText(studentProfile.optString("first_name", "N/A") + " " +
                                studentProfile.optString("last_name", "N/A"));
                        emailTextView.setText(studentProfile.optString("email", "N/A"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("fetchStudentProfile", "Error parsing profile data: " + e.getMessage());
                    }
                },
                error -> {
                    error.printStackTrace();
                    Log.e("fetchStudentProfile", "Error fetching profile: " + error.getMessage());
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token); // Add Authorization header
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void fetchProgramName() {
        String studentId = sharedPref.getValue_string("userID");
        if (studentId == null || studentId.isEmpty()) {
            Log.e("ProgramOutlineFragment", "Error: Student ID is missing.");
            TextView programTitleView = requireView().findViewById(R.id.programTitle);
            programTitleView.setText("Student ID Missing");
            return;
        }

        // Use 10.0.2.2 for localhost on Android emulator
        String programsUrl = "http://10.0.2.2:5000/api/programs?studentId=" + studentId;

        JsonObjectRequest programRequest = new JsonObjectRequest(Request.Method.GET, programsUrl, null,
                response -> {
                    try {
                        String programName = response.optString("program_name", "Unknown Program");
                        TextView programTitleView = requireView().findViewById(R.id.programTitle);
                        programTitleView.setText("Program: " + programName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        TextView programTitleView = requireView().findViewById(R.id.programTitle);
                        programTitleView.setText("Program Name Not Available");
                    }
                },
                error -> {
                    Log.e("ProgramOutlineFragment", "Error fetching program name: " + error.getMessage());
                    TextView programTitleView = requireView().findViewById(R.id.programTitle);
                    programTitleView.setText("Program Name Not Available");
                });

        requestQueue.add(programRequest);
    }

    private void addProgramTitle(String title) {
        TextView programTitle = new TextView(requireContext());
        programTitle.setText(title);
        programTitle.setTextSize(20);
        programTitle.setTypeface(null, Typeface.BOLD);
        programTitle.setPadding(16, 16, 16, 8);
        programOutlineContainer.addView(programTitle, 0); // Add at the top of the container
    }

    private void fetchProgramOutlineData() {
        String studentId = sharedPref.getValue_string("userID");
        String programCoursesUrl = "http://10.0.2.2:5000/api/program-courses?studentId=" + studentId;
        String completedCoursesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;

        // Fetch program courses
        JsonArrayRequest programCoursesRequest = new JsonArrayRequest(Request.Method.GET, programCoursesUrl, null,
                programCoursesResponse -> {
                    try {
                        List<Course> programCourses = parseCourses(programCoursesResponse);

                        // Fetch completed courses
                        JsonArrayRequest completedCoursesRequest = new JsonArrayRequest(Request.Method.GET, completedCoursesUrl, null,
                                completedCoursesResponse -> {
                                    try {
                                        Set<String> completedCourseCodes = parseCompletedCourses(completedCoursesResponse);

                                        // Fetch currently registered courses
                                        fetchCurrentlyRegisteredCourses(studentId, programCourses, completedCourseCodes);

                                    } catch (JSONException e) {
                                        Log.e("ProgramOutlineFragment", "Error parsing completed courses", e);
                                    }
                                },
                                error -> handleError("completed courses", error));

                        requestQueue.add(completedCoursesRequest);

                    } catch (JSONException e) {
                        Log.e("ProgramOutlineFragment", "Error parsing program courses", e);
                    }
                },
                error -> handleError("program courses", error));

        requestQueue.add(programCoursesRequest);
    }

    private void fetchCurrentlyRegisteredCourses(String studentId, List<Course> programCourses, Set<String> completedCourseCodes) {
        String url = "http://10.0.2.2:5000/api/active-registrations?studentId=" + studentId;

        Log.d("fetchCurrentlyRegisteredCourses", "Fetching currently registered courses from URL: " + url);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        Set<String> currentlyRegisteredCourses = new HashSet<>();
                        if (response != null) {
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                currentlyRegisteredCourses.add(obj.getString("course_code"));
                            }
                        }

                        // Process and display data
                        Map<String, Map<String, List<String>>> groupedCourses = processCourses(programCourses, completedCourseCodes, currentlyRegisteredCourses);
                        displayProgramOutline(groupedCourses);

                    } catch (JSONException e) {
                        Log.e("ProgramOutlineFragment", "Error parsing currently registered courses", e);
                    }
                },
                error -> {
                    Log.e("ProgramOutlineFragment", "Error fetching currently registered courses: " + error.getMessage());
                    handleError("currently registered courses", error);
                });

        requestQueue.add(request);
    }

    private List<Course> parseCourses(JSONArray response) throws JSONException {
        List<Course> courses = new ArrayList<>();

        for (int i = 0; i < response.length(); i++) {
            JSONObject courseObj = response.getJSONObject(i);

            String courseCode = courseObj.optString("course_code", "Unknown");
            String title = courseObj.optString("course_name", "No Title");
            String campus = courseObj.optString("course_campus", "Unknown");
            String mode = courseObj.optString("course_mode", "Unknown");
            String semester = courseObj.optString("semester", "Unknown");
            String preRequisite = courseObj.optString("pre_requisite", "None");

            courses.add(new Course(courseCode, title, campus, mode, semester, preRequisite));
        }

        return courses;
    }

    private Set<String> parseCompletedCourses(JSONArray response) throws JSONException {
        Set<String> completedCourseCodes = new HashSet<>();
        Set<String> passingGrades = new HashSet<>(List.of("A+", "A", "B+", "B", "C+", "C", "S"));

        for (int i = 0; i < response.length(); i++) {
            JSONObject courseObj = response.getJSONObject(i);
            String courseCode = courseObj.getString("CourseID");
            String grade = courseObj.getString("grade");
            if (passingGrades.contains(grade)) {
                completedCourseCodes.add(courseCode);
            }
        }

        return completedCourseCodes;
    }

    private Map<String, Map<String, List<String>>> processCourses(List<Course> programCourses, Set<String> completedCourseCodes, Set<String> currentlyRegisteredCourses) {
        // Use TreeMap with a custom comparator to sort levels numerically
        Map<String, Map<String, List<String>>> groupedCourses = new TreeMap<>((level1, level2) -> {
            int num1 = Integer.parseInt(level1.split(" ")[0]); // Extract the numeric part (e.g., "200" from "200 Level")
            int num2 = Integer.parseInt(level2.split(" ")[0]);
            return Integer.compare(num1, num2);
        });

        for (Course course : programCourses) {
            String level = getCourseLevel(course.getCourseCode());
            boolean isCompleted = completedCourseCodes.contains(course.getCourseCode());
            boolean isCurrentlyRegistered = currentlyRegisteredCourses.contains(course.getCourseCode());

            groupedCourses.putIfAbsent(level, new HashMap<>());
            groupedCourses.get(level).putIfAbsent("Completed", new ArrayList<>());
            groupedCourses.get(level).putIfAbsent("Currently Registered", new ArrayList<>());
            groupedCourses.get(level).putIfAbsent("Pending Courses", new ArrayList<>());

            if (isCompleted) {
                groupedCourses.get(level).get("Completed").add(course.getCourseCode());
            } else if (isCurrentlyRegistered) {
                groupedCourses.get(level).get("Currently Registered").add(course.getCourseCode());
            } else {
                groupedCourses.get(level).get("Pending Courses").add(course.getCourseCode());
            }
        }

        return groupedCourses;
    }

    private String getCourseLevel(String courseCode) {
        if (courseCode.equals("CS001")) {
            return "200 Level";
        }
        if (courseCode.length() >= 3 && Character.isDigit(courseCode.charAt(2))) {
            return courseCode.charAt(2) + "00 Level";
        }
        return "Unknown Level";
    }

    private void displayProgramOutline(Map<String, Map<String, List<String>>> groupedCourses) {
        // Clear the container before adding new content
        programOutlineContainer.removeAllViews();

        // Create a header row for the table
        LinearLayout headerRow = new LinearLayout(requireContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        headerRow.setPadding(8, 8, 8, 8);
        headerRow.setBackgroundColor(getResources().getColor(R.color.teal_700));

        // Add header columns
        addTableHeader(headerRow, "Year Level"); // New column for Year Level
        addTableHeader(headerRow, "Completed");
        addTableHeader(headerRow, "Currently Registered");
        addTableHeader(headerRow, "Pending Courses");

        // Add the header row to the container
        programOutlineContainer.addView(headerRow);

        // Iterate through levels and add rows to the table
        for (Map.Entry<String, Map<String, List<String>>> levelEntry : groupedCourses.entrySet()) {
            String level = levelEntry.getKey();
            Map<String, List<String>> categories = levelEntry.getValue();

            // Create a row for the level
            LinearLayout levelRow = new LinearLayout(requireContext());
            levelRow.setOrientation(LinearLayout.HORIZONTAL);
            levelRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            levelRow.setPadding(8, 8, 8, 8);
            levelRow.setBackgroundColor(getResources().getColor(R.color.light_gray));

            // Add Year Level column
            addTableCell(levelRow, level);

            // Add columns for each category
            addTableCell(levelRow, formatCoursesVertically(categories.get("Completed")));
            addTableCell(levelRow, formatCoursesVertically(categories.get("Currently Registered")));
            addTableCell(levelRow, formatCoursesVertically(categories.get("Pending Courses")));

            // Add the level row to the container
            programOutlineContainer.addView(levelRow);
        }
    }

    // Helper method to format courses vertically
    private String formatCoursesVertically(List<String> courses) {
        if (courses == null || courses.isEmpty()) {
            return ""; // Return empty string if no courses
        }
        return String.join("\n", courses); // Join courses with a newline for vertical stacking
    }

    // Helper method to add a header cell
    private void addTableHeader(LinearLayout row, String text) {
        TextView headerCell = new TextView(requireContext());
        headerCell.setText(text);
        headerCell.setTextSize(16);
        headerCell.setTypeface(null, Typeface.BOLD);
        headerCell.setTextColor(getResources().getColor(R.color.white));
        headerCell.setPadding(8, 8, 8, 8);
        headerCell.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1 // Equal weight for all columns
        ));
        row.addView(headerCell);
    }

    // Helper method to add a table cell
    private void addTableCell(LinearLayout row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text.isEmpty() ? "" : text); // Removed "N/A"
        cell.setTextSize(14);
        cell.setPadding(8, 8, 8, 8);
        cell.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1 // Equal weight for all columns
        ));
        row.addView(cell);
    }

    private void handleError(String dataType, VolleyError error) {
        Log.e("ProgramOutlineFragment", "Error fetching " + dataType + ": " + error.getMessage());
    }

    private void showHoldPage(){
        Log.d("YourFragment", "Showing hold page now...");
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, new ErrorFragment())
                .commit();
    }


}