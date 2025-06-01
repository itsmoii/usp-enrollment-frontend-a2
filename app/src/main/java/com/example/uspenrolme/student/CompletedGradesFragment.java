package com.example.uspenrolme.student;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;  // Make sure this matches your package name
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.models.GradeItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CompletedGradesFragment extends Fragment {

    private static final String TAG = "CompletedGradesFragment";

    private SharedPreference sharedPref;
    private RequestQueue requestQueue;
    private List<GradeItem> currentGrades = new ArrayList<>();
    private double currentGpa = 0.0; // GPA calculation might need to be handled differently if based on all courses

    private TableLayout gradesTable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_completed_grades, container, false);
        gradesTable = view.findViewById(R.id.gradesTable);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRequestQueue();
        fetchGradesData();
    }

    private void setupRequestQueue() {
        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    private void fetchGradesData() {
        String studentId = sharedPref.getValue_string("userID");
        String gradesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;

        JsonArrayRequest gradesRequest = new JsonArrayRequest(
                Request.Method.GET,
                gradesUrl,
                null,
                this::processGradesResponse,
                error -> Log.e(TAG, "Error fetching grades", error)
        );

        requestQueue.add(gradesRequest);
    }

    private void processGradesResponse(JSONArray response) {
        try {
            currentGrades.clear();
            for (int i = 0; i < response.length(); i++) {
                JSONObject gradeObj = response.getJSONObject(i);
                currentGrades.add(new GradeItem(
                        gradeObj.optString("term", "N/A"),
                        gradeObj.optString("CourseID", "N/A"),
                        gradeObj.optString("title", "N/A"),
                        gradeObj.optString("campus", "N/A"),
                        gradeObj.optString("mode", "N/A"),
                        gradeObj.optString("grade", "N/A")
                ));
            }
            displayGrades(currentGrades);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing grades response", e);
            showErrorToast("Error processing grades data");
        }
    }

    private void displayGrades(List<GradeItem> grades) {
        gradesTable.removeAllViews();
        createTableHeader(gradesTable,
                new String[]{"TERM", "COURSE", "TITLE", "CAMPUS", "MODE", "GRADE"});

        double totalPoints = 0.0;
        int totalCourses = 0;

        for (int i = 0; i < grades.size(); i++) {
            GradeItem grade = grades.get(i);
            TableRow gradeRow = createTableRow(i);

            addCellToRow(gradeRow, grade.getTerm());
            addCellToRow(gradeRow, grade.getCourseCode());
            addCellToRow(gradeRow, grade.getTitle());
            addCellToRow(gradeRow, grade.getCampus());
            addCellToRow(gradeRow, grade.getMode());

            TextView gradeCell = createCell(grade.getGrade());
            gradeCell.setTextColor(getGradeTextColor(grade.getGrade()));
            gradeRow.addView(gradeCell);

            gradesTable.addView(gradeRow);

            double gradePoints = getGradePoints(grade.getGrade());
            if (gradePoints >= 0) {
                totalPoints += gradePoints;
                totalCourses++;
            }
        }

        currentGpa = totalCourses > 0 ? totalPoints / totalCourses : 0.0;
        // GPA TextView is in the parent GradesFragment, so this update won't work directly
        // You'll need a way to communicate the GPA back to the parent fragment if you want to display it there.
        // For now, this calculation is here but not displayed in this fragment.
        // gpaTextView.setText(String.format(Locale.getDefault(), "%.2f", currentGpa));
    }

    private void createTableHeader(TableLayout table, String[] headers) {
        TableRow headerRow = new TableRow(requireContext());
        headerRow.setBackgroundColor(getResources().getColor(R.color.teal_700));

        for (String header : headers) {
            TextView headerCell = new TextView(requireContext());
            headerCell.setText(header);
            headerCell.setTextSize(16);
            headerCell.setTypeface(null, Typeface.BOLD);
            headerCell.setTextColor(Color.WHITE);
            headerCell.setPadding(20, 20, 20, 20);
            headerCell.setGravity(Gravity.CENTER);
            headerRow.addView(headerCell);
        }

        table.addView(headerRow);
    }

    private TableRow createTableRow(int position) {
        TableRow row = new TableRow(requireContext());
        int bgColor = position % 2 == 0 ? Color.WHITE : getResources().getColor(R.color.light_gray);
        row.setBackgroundColor(bgColor);
        return row;
    }

    private void addCellToRow(TableRow row, String text) {
        TextView cell = createCell(text);
        row.addView(cell);
    }

    private TextView createCell(String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setTextSize(14);
        cell.setPadding(20, 20, 20, 20);
        cell.setGravity(Gravity.CENTER);
        return cell;
    }

    private int getGradeTextColor(String grade) {
        return grade.equalsIgnoreCase("F") ? Color.RED : Color.BLACK;
    }

    private double getGradePoints(String grade) {
        switch (grade.toUpperCase()) {
            case "A+": return 4.5;
            case "A": return 4.0;
            case "B+": return 3.5;
            case "B": return 3.0;
            case "C+": return 2.5;
            case "C": return 2.0;
            case "D+": return 1.5;
            case "D": return 1.0;
            case "F": return 0.0;
            default: return -1.0;
        }
    }

     private void showErrorToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}