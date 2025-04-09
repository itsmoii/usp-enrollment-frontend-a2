package com.example.uspenrolme.student;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.graphics.Typeface;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class GradesFragment extends Fragment {

    private SharedPreference sharedPref;
    private RequestQueue requestQueue;
    private TableLayout gradesTable;

    public GradesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grades, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());
        gradesTable = view.findViewById(R.id.gradesTable);

        fetchGradesData();
    }

    private void fetchGradesData() {
        String studentId = sharedPref.getValue_string("userID");
        String gradesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;

        JsonArrayRequest gradesRequest = new JsonArrayRequest(
                Request.Method.GET, gradesUrl, null,
                this::processGradesResponse,
                error -> Log.e("GradesFragment", "Error fetching grades", error)
        );

        requestQueue.add(gradesRequest);
    }

    private void processGradesResponse(JSONArray response) {
        try {
            List<GradeItem> grades = new ArrayList<>();

            for (int i = 0; i < response.length(); i++) {
                JSONObject gradeObj = response.getJSONObject(i);
                String term = gradeObj.optString("term", "N/A");
                String courseCode = gradeObj.optString("CourseID", "N/A");
                String title = gradeObj.optString("title", "N/A");
                String campus = gradeObj.optString("campus", "N/A");
                String mode = gradeObj.optString("mode", "N/A");
                String grade = gradeObj.optString("grade", "N/A");

                grades.add(new GradeItem(term, courseCode, title, campus, mode, grade));
            }

            displayGrades(grades);
        } catch (JSONException e) {
            Log.e("GradesFragment", "Error parsing grades response", e);
        }
    }

    private void displayGrades(List<GradeItem> grades) {
        // Clear existing views
        gradesTable.removeAllViews();
    
        // Create table header
        TableRow headerRow = new TableRow(requireContext());
        headerRow.setBackgroundColor(getResources().getColor(R.color.teal_700));
    
        addHeaderCell(headerRow, "TERM");
        addHeaderCell(headerRow, "COURSE");
        addHeaderCell(headerRow, "TITLE");
        addHeaderCell(headerRow, "CAMPUS");
        addHeaderCell(headerRow, "MODE");
        addHeaderCell(headerRow, "GRADE");
    
        gradesTable.addView(headerRow);
    
        // Add grade rows
        double totalPoints = 0.0;
        int totalCourses = 0;
    
        for (int i = 0; i < grades.size(); i++) {
            GradeItem grade = grades.get(i);
    
            TableRow gradeRow = new TableRow(requireContext());
    
            // Alternate row colors
            if (i % 2 == 0) {
                gradeRow.setBackgroundColor(Color.WHITE); // White for even rows
            } else {
                gradeRow.setBackgroundColor(getResources().getColor(R.color.light_gray)); // Light gray for odd rows
            }
    
            addGradeCell(gradeRow, grade.getTerm());
            addGradeCell(gradeRow, grade.getCourseCode());
            addGradeCell(gradeRow, grade.getTitle());
            addGradeCell(gradeRow, grade.getCampus());
            addGradeCell(gradeRow, grade.getMode());
    
            // Grade cell
            TextView gradeCell = new TextView(requireContext());
            gradeCell.setText(grade.getGrade());
            gradeCell.setTextSize(14);
            gradeCell.setPadding(16, 8, 16, 8);
            gradeCell.setGravity(Gravity.CENTER);
            gradeCell.setTextColor(Color.BLACK); // Set grade text color to black
            gradeRow.addView(gradeCell);
    
            gradesTable.addView(gradeRow);
    
            // Calculate GPA
            double gradePoints = getGradePoints(grade.getGrade());
            if (gradePoints >= 0) {
                totalPoints += gradePoints;
                totalCourses++;
            }
        }
    
        // Calculate and display GPA
        double gpa = totalCourses > 0 ? totalPoints / totalCourses : 0.0;
        TextView gpaTextView = requireView().findViewById(R.id.gpaTextView);
        gpaTextView.setText(String.format("%.2f", gpa));
    }
    
    private double getGradePoints(String grade) {
        switch (grade.toUpperCase()) {
            case "A+":
                return 4.5;
            case "A":
                return 4.0;
            case "B+":
                return 3.5;
            case "B":
                return 3.0;
            case "C+":
                return 2.5;
            case "C":
                return 2.0;
            case "D+":
                return 1.5;
            case "D":
                return 1.0;
            case "F":
                return 0.0;
            default:
                return -1.0; // Invalid grade
        }
    }

    private void addHeaderCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setTextSize(16);
        cell.setTypeface(null, Typeface.BOLD);
        cell.setTextColor(Color.WHITE);
        cell.setPadding(20, 20, 20, 20); // Increased padding for height
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        row.addView(cell);
    }
    
    private void addGradeCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setTextSize(14);
        cell.setPadding(20, 20, 20, 20); // Increased padding for height
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        row.addView(cell);
    }

    private boolean isPassingGrade(String grade) {
        // Define which grades are considered passing
        String[] passingGrades = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "S"};
        for (String passingGrade : passingGrades) {
            if (passingGrade.equalsIgnoreCase(grade)) {
                return true;
            }
        }
        return false;
    }

    private static class GradeItem {
        private final String term;
        private final String courseCode;
        private final String title;
        private final String campus;
        private final String mode;
        private final String grade;

        public GradeItem(String term, String courseCode, String title, String campus, String mode, String grade) {
            this.term = term;
            this.courseCode = courseCode;
            this.title = title;
            this.campus = campus;
            this.mode = mode;
            this.grade = grade;
        }

        public String getTerm() { return term; }
        public String getCourseCode() { return courseCode; }
        public String getTitle() { return title; }
        public String getCampus() { return campus; }
        public String getMode() { return mode; }
        public String getGrade() { return grade; }
    }
}