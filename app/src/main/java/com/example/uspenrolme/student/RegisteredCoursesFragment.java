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
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.models.GradeItem;
import com.example.uspenrolme.models.RegisteredCourseItem;
import com.example.uspenrolme.student.GradesFragment.GradesDataProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RegisteredCoursesFragment extends Fragment implements GradesDataProvider {
    private static final String TAG = "RegisteredCoursesFragment";

    private SharedPreference sharedPref;
    private RequestQueue requestQueue;
    private List<RegisteredCourseItem> currentRegisteredCourses = new ArrayList<>();

    private TableLayout registeredCoursesTable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registered_courses, container, false);
        registeredCoursesTable = view.findViewById(R.id.registeredCoursesTable);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRequestQueue();
        fetchRegisteredCoursesData();
    }

    private void setupRequestQueue() {
        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    private void fetchRegisteredCoursesData() {
        String studentId = sharedPref.getValue_string("userID");
        String registeredUrl = "http://10.0.2.2:5000/api/active-registrations?studentId=" + studentId;

        JsonArrayRequest registeredRequest = new JsonArrayRequest(
                Request.Method.GET,
                registeredUrl,
                null,
                this::processRegisteredCoursesResponse,
                error -> Log.e(TAG, "Error fetching registered courses", error)
        );

        requestQueue.add(registeredRequest);
    }

    private void processRegisteredCoursesResponse(JSONArray response) {
        try {
            currentRegisteredCourses.clear();
            for (int i = 0; i < response.length(); i++) {
                JSONObject courseObj = response.getJSONObject(i);
                currentRegisteredCourses.add(new RegisteredCourseItem(
                        courseObj.optString("term", "N/A"),
                        courseObj.optString("CourseID", "N/A"),
                        courseObj.optString("title", "N/A"),
                        courseObj.optString("campus", "N/A"),
                        courseObj.optString("mode", "N/A"),
                        courseObj.optString("status", "N/A")
                ));
            }
            displayRegisteredCourses(currentRegisteredCourses);
            if (getParentFragment() instanceof GradesFragment) {
                ((GradesFragment) getParentFragment()).updateGpaAndCountDisplay();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing registered courses response", e);
            showErrorToast("Error processing registered courses data");
        }
    }

    private void displayRegisteredCourses(List<RegisteredCourseItem> courses) {
        registeredCoursesTable.removeAllViews();
        createTableHeader(registeredCoursesTable,
                new String[]{"TERM", "COURSE", "TITLE", "CAMPUS", "MODE", "STATUS"});

        for (int i = 0; i < courses.size(); i++) {
            RegisteredCourseItem course = courses.get(i);
            TableRow courseRow = createTableRow(i);

            addCellToRow(courseRow, course.getTerm());
            addCellToRow(courseRow, course.getCourseCode());
            addCellToRow(courseRow, course.getTitle());
            addCellToRow(courseRow, course.getCampus());
            addCellToRow(courseRow, course.getMode());

            TextView statusCell = createCell(course.getStatus());
            statusCell.setTextColor(getStatusTextColor(course.getStatus()));
            courseRow.addView(statusCell);

            registeredCoursesTable.addView(courseRow);
        }
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

    private int getStatusTextColor(String status) {
        return status.equalsIgnoreCase("Failed") ? Color.RED : Color.BLACK;
    }

    private void showErrorToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public List<GradeItem> getCompletedGradesData() {
        return new ArrayList<>();
    }

    @Override
    public List<RegisteredCourseItem> getRegisteredCoursesData() {
        return currentRegisteredCourses;
    }

    @Override
    public double getCalculatedGpa() {
        return 0.0;
    }

    @Override
    public int getRegisteredCourseCount() {
        return currentRegisteredCourses.size();
    }
}