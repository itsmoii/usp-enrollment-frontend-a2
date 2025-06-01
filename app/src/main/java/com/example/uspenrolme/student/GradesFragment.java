package com.example.uspenrolme.student;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.example.uspenrolme.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.HoldUtils;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.adapters.GradesPrintDocumentAdapter;
import com.example.uspenrolme.shared.ErrorFragment;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;

public class GradesFragment extends Fragment {

    private static final String TAG = "GradesFragment";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private SharedPreference sharedPref;
    private RequestQueue requestQueue;
    private List<GradeItem> currentGrades = new ArrayList<>();
    private List<RegisteredCourseItem> currentRegisteredCourses = new ArrayList<>();
    private double currentGpa = 0.0;

    // UI Components
    private TableLayout gradesTable;
    private TableLayout registeredCoursesTable;
    private TextView gpaTextView;
    private TextView registeredCountTextView;

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
        initializeViews(view);
        setupRequestQueue();
        checkForHolds();
        setupButtonListeners(view);
    }

    private void initializeViews(View view) {
        gradesTable = view.findViewById(R.id.gradesTable);
        registeredCoursesTable = view.findViewById(R.id.registeredCoursesTable);
        gpaTextView = view.findViewById(R.id.gpaTextView);
        registeredCountTextView = view.findViewById(R.id.registeredCountTextView);
    }

    private void setupRequestQueue() {
        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    private void checkForHolds() {
        String token = sharedPref.getValue_string("token");
        HoldUtils.checkHold(requireContext(), token, "grades", isBlocked -> {
            if (isBlocked) {
                showHoldPage();
            } else {
                fetchGradesData();
                fetchRegisteredCoursesData();
            }
        });
    }

    private void setupButtonListeners(View view) {
        Button printButton = view.findViewById(R.id.printButton);
        Button savePdfButton = view.findViewById(R.id.savePdfButton);

        printButton.setOnClickListener(v -> printGrades());
        savePdfButton.setOnClickListener(v -> saveAsPdf());
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

    private void fetchRegisteredCoursesData() {
        String studentId = sharedPref.getValue_string("userID");
        String registeredUrl = "http://10.0.2.2:5000/api/registered-courses?studentId=" + studentId;

        JsonArrayRequest registeredRequest = new JsonArrayRequest(
                Request.Method.GET,
                registeredUrl,
                null,
                this::processRegisteredCoursesResponse,
                error -> Log.e(TAG, "Error fetching registered courses", error)
        );

        requestQueue.add(registeredRequest);
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
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing registered courses response", e);
            showErrorToast("Error processing registered courses data");
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
        gpaTextView.setText(String.format(Locale.getDefault(), "%.2f", currentGpa));
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

        registeredCountTextView.setText(String.format(Locale.getDefault(),
                "%d courses", courses.size()));
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

    private int getStatusTextColor(String status) {
        return status.equalsIgnoreCase("Failed") ? Color.RED : Color.BLACK;
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

    private void showHoldPage() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, new ErrorFragment())
                .commit();
    }

    private void printGrades() {
        try {
            PrintManager printManager = (PrintManager) requireActivity().getSystemService(Context.PRINT_SERVICE);
            String jobName = "Grades Report - " + sharedPref.getValue_string("userID");
            printManager.print(jobName,
                    new GradesPrintDocumentAdapter(requireContext(), currentGrades, currentRegisteredCourses, currentGpa),
                    null);
        } catch (Exception e) {
            Log.e(TAG, "Error printing grades", e);
            showErrorToast("Error printing grades");
        }
    }

    private void saveAsPdf() {
        PdfDocument document = new PdfDocument();
        try {
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            drawPdfContent(canvas);
            document.finishPage(page);

            File file = createPdfFile();
            saveDocumentToFile(document, file);
        } catch (Exception e) {
            Log.e(TAG, "Error generating PDF", e);
            showErrorToast("Error generating PDF");
        } finally {
            document.close();
        }
    }

    private void drawPdfContent(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);

        String studentId = sharedPref.getValue_string("userID");
        String studentName = sharedPref.getValue_string("username");

        // Draw header
        canvas.drawText("GRADES REPORT", 50, 50, paint);
        paint.setTextSize(10);
        canvas.drawText("Student: " + studentName, 50, 70, paint);
        canvas.drawText("ID: " + studentId, 50, 85, paint);
        canvas.drawText("Date: " + getCurrentDate(), 50, 100, paint);

        // Draw completed courses
        drawSectionTitle(canvas, paint, "COMPLETED COURSES", 130);
        drawCourseList(canvas, paint, currentGrades, 150);

        // Draw registered courses
        float yPosition = 150 + (currentGrades.size() * 20) + 50;
        drawSectionTitle(canvas, paint, "REGISTERED COURSES", yPosition);
        drawRegisteredCourseList(canvas, paint, currentRegisteredCourses, yPosition + 20);

        // Draw GPA
        yPosition += (currentRegisteredCourses.size() * 20) + 30;
        drawGpa(canvas, paint, yPosition);
    }

    private void drawSectionTitle(Canvas canvas, Paint paint, String title, float y) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(title, 50, y, paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
    }

    private void drawCourseList(Canvas canvas, Paint paint, List<GradeItem> courses, float startY) {
        float y = startY;
        for (GradeItem grade : courses) {
            canvas.drawText(grade.getTerm() + " - " + grade.getCourseCode() + " - " +
                    grade.getTitle() + " (" + grade.getGrade() + ")", 50, y, paint);
            y += 20;
        }
    }

    private void drawRegisteredCourseList(Canvas canvas, Paint paint, List<RegisteredCourseItem> courses, float startY) {
        float y = startY;
        for (RegisteredCourseItem course : courses) {
            canvas.drawText(course.getTerm() + " - " + course.getCourseCode() + " - " +
                    course.getTitle() + " (" + course.getStatus() + ")", 50, y, paint);
            y += 20;
        }
    }

    private void drawGpa(Canvas canvas, Paint paint, float y) {
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Cumulative GPA: " + String.format(Locale.getDefault(), "%.2f", currentGpa), 50, y, paint);
    }

    private File createPdfFile() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String fileName = "Grades_" + sharedPref.getValue_string("userID") + "_" + System.currentTimeMillis() + ".pdf";
        return new File(downloadsDir, fileName);
    }

    private void saveDocumentToFile(PdfDocument document, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            document.writeTo(fos);
            showSuccessToast("PDF saved to Downloads");
        }
    }

    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void showErrorToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccessToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Data model classes
    public static class GradeItem {
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

    public static class RegisteredCourseItem {
        private final String term;
        private final String courseCode;
        private final String title;
        private final String campus;
        private final String mode;
        private final String status;

        public RegisteredCourseItem(String term, String courseCode, String title, String campus, String mode, String status) {
            this.term = term;
            this.courseCode = courseCode;
            this.title = title;
            this.campus = campus;
            this.mode = mode;
            this.status = status;
        }

        public String getTerm() { return term; }
        public String getCourseCode() { return courseCode; }
        public String getTitle() { return title; }
        public String getCampus() { return campus; }
        public String getMode() { return mode; }
        public String getStatus() { return status; }
    }
}