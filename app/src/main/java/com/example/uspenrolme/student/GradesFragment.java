package com.example.uspenrolme.student;

import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class GradesFragment extends Fragment {

    private SharedPreference sharedPref;
    private RequestQueue requestQueue;
    private TableLayout gradesTable;
    private LinearLayout semesterGpaContainer;
    private ProgressBar loadingIndicator;

    // Navigation
    private Button gradesTabButton, reportsTabButton;
    private LinearLayout gradesSection, reportsSection;

    // Data
    private List<GradeItem> allGrades = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_grades, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        // Initialize UI
        gradesTable = view.findViewById(R.id.gradesTable);
        semesterGpaContainer = view.findViewById(R.id.semesterGpaContainer);

        gradesTabButton = view.findViewById(R.id.gradesTabButton);
        reportsTabButton = view.findViewById(R.id.reportsTabButton);
        gradesSection = view.findViewById(R.id.gradesSection);
        reportsSection = view.findViewById(R.id.reportsSection);

        setupNavigation();
        loadGrades();
    }

    // --- UI Navigation ---
    private void setupNavigation() {
        gradesTabButton.setOnClickListener(v -> showGradesSection());
        reportsTabButton.setOnClickListener(v -> showReportsSection());
        showGradesSection(); // Default view
    }

    private void showGradesSection() {
        gradesTabButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        gradesTabButton.setTextColor(Color.WHITE);
        reportsTabButton.setBackgroundColor(Color.LTGRAY);
        reportsTabButton.setTextColor(Color.BLACK);
        gradesSection.setVisibility(View.VISIBLE);
        reportsSection.setVisibility(View.GONE);
    }

    private void showReportsSection() {
        reportsTabButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        reportsTabButton.setTextColor(Color.WHITE);
        gradesTabButton.setBackgroundColor(Color.LTGRAY);
        gradesTabButton.setTextColor(Color.BLACK);
        gradesSection.setVisibility(View.GONE);
        reportsSection.setVisibility(View.VISIBLE);

        if (!allGrades.isEmpty()) {
            displayReports(allGrades);
        }
    }

    // --- Data Loading ---
    private void loadGrades() {
        fetchGradesData();
    }

    private void fetchGradesData() {
        showLoading(true);
        String studentId = sharedPref.getString("userID", "N/A");
        String gradesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;

        JsonArrayRequest gradesRequest = new JsonArrayRequest(
                Request.Method.GET, gradesUrl, null,
                this::processGradesResponse,
                error -> {
                    Log.e("GradesFragment", "Error fetching grades", error);
                    Toast.makeText(requireContext(), "Failed to load grades", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                }
        );
        requestQueue.add(gradesRequest);
    }

    private void processGradesResponse(JSONArray response) {
        try {
            allGrades.clear();
            for (int i = 0; i < response.length(); i++) {
                JSONObject gradeObj = response.getJSONObject(i);
                allGrades.add(new GradeItem(
                        gradeObj.optString("term", "N/A"),
                        gradeObj.optString("CourseID", "N/A"),
                        gradeObj.optString("title", "N/A"),
                        gradeObj.optString("campus", "N/A"),
                        gradeObj.optString("mode", "N/A"),
                        gradeObj.optString("grade", "N/A")
                ));
            }
            displayGrades(allGrades);
        } catch (JSONException e) {
            Log.e("GradesFragment", "Error parsing grades", e);
            Toast.makeText(requireContext(), "Error processing data", Toast.LENGTH_SHORT).show();
        }
        showLoading(false);
    }

    // --- Display Grades Table ---
    private void displayGrades(List<GradeItem> grades) {
        gradesTable.removeAllViews();

        // Table Header
        TableRow headerRow = new TableRow(requireContext());
        headerRow.setBackgroundColor(getResources().getColor(R.color.teal_700));
        addHeaderCell(headerRow, "TERM");
        addHeaderCell(headerRow, "COURSE");
        addHeaderCell(headerRow, "TITLE");
        addHeaderCell(headerRow, "CAMPUS");
        addHeaderCell(headerRow, "MODE");
        addHeaderCell(headerRow, "GRADE");
        gradesTable.addView(headerRow);

        // Table Rows
        double totalPoints = 0;
        int totalCourses = 0;

        for (int i = 0; i < grades.size(); i++) {
            GradeItem grade = grades.get(i);
            TableRow gradeRow = new TableRow(requireContext());
            gradeRow.setBackgroundColor(i % 2 == 0 ? Color.WHITE : getResources().getColor(R.color.light_gray));

            addGradeCell(gradeRow, grade.getTerm());
            addGradeCell(gradeRow, grade.getCourseCode());
            addGradeCell(gradeRow, grade.getTitle());
            addGradeCell(gradeRow, grade.getCampus());
            addGradeCell(gradeRow, grade.getMode());

            TextView gradeCell = new TextView(requireContext());
            gradeCell.setText(grade.getGrade());
            gradeCell.setTextColor(getGradeColor(grade.getGrade()));
            gradeCell.setGravity(Gravity.CENTER);
            gradeRow.addView(gradeCell);

            gradesTable.addView(gradeRow);

            // GPA Calculation
            double gradePoints = getGradePoints(grade.getGrade());
            if (gradePoints >= 0) {
                totalPoints += gradePoints;
                totalCourses++;
            }
        }

        // Display GPA
        TextView gpaTextView = requireView().findViewById(R.id.gpaTextView);
        gpaTextView.setText(String.format("GPA: %.2f", totalCourses > 0 ? totalPoints / totalCourses : 0.0));
    }

    // --- Reports Section ---
    private void displayReports(List<GradeItem> grades) {
        semesterGpaContainer.removeAllViews();

        // Group by semester
        Map<String, List<GradeItem>> semesterGrades = new HashMap<>();
        for (GradeItem grade : grades) {
            String term = grade.getTerm();
            if (!semesterGrades.containsKey(term)) {
                semesterGrades.put(term, new ArrayList<>());
            }
            semesterGrades.get(term).add(grade);
        }

        // Create report cards
        for (Map.Entry<String, List<GradeItem>> entry : semesterGrades.entrySet()) {
            createSemesterReportCard(entry.getKey(), entry.getValue());
        }
    }

    private void createSemesterReportCard(String semester, List<GradeItem> courses) {
        // Calculate GPA
        double totalPoints = 0;
        int totalCourses = 0;
        for (GradeItem course : courses) {
            double gradePoints = getGradePoints(course.getGrade());
            if (gradePoints >= 0) {
                totalPoints += gradePoints;
                totalCourses++;
            }
        }
        double semesterGpa = totalCourses > 0 ? totalPoints / totalCourses : 0.0;

        // Card Layout
        LinearLayout cardLayout = new LinearLayout(requireContext());
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));
        cardLayout.setPadding(24, 20, 24, 20);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 16);
        cardLayout.setLayoutParams(cardParams);

        // Header
        LinearLayout headerLayout = new LinearLayout(requireContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView semesterText = new TextView(requireContext());
        semesterText.setText("Semester: " + semester);
        semesterText.setTextSize(18);
        semesterText.setTypeface(null, Typeface.BOLD);
        semesterText.setTextColor(getResources().getColor(R.color.teal_700));

        TextView summaryText = new TextView(requireContext());
        summaryText.setText(String.format("Courses: %d | GPA: %.2f", totalCourses, semesterGpa));
        summaryText.setTextSize(14);
        summaryText.setTextColor(Color.GRAY);

        LinearLayout infoLayout = new LinearLayout(requireContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.addView(semesterText);
        infoLayout.addView(summaryText);

        // PDF Button
        Button pdfButton = new Button(requireContext());
        pdfButton.setText("SAVE AS PDF");
        pdfButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        pdfButton.setTextColor(Color.WHITE);
        pdfButton.setOnClickListener(v -> generatePdfReport(semester, courses, semesterGpa));

        headerLayout.addView(infoLayout);
        headerLayout.addView(pdfButton);
        cardLayout.addView(headerLayout);

        // Courses Preview Table
        TableLayout previewTable = new TableLayout(requireContext());
        previewTable.setPadding(0, 16, 0, 0);

        // Table Header
        TableRow previewHeaderRow = new TableRow(requireContext());
        previewHeaderRow.setBackgroundColor(getResources().getColor(R.color.teal_200));
        addPreviewHeaderCell(previewHeaderRow, "Course");
        addPreviewHeaderCell(previewHeaderRow, "Title");
        addPreviewHeaderCell(previewHeaderRow, "Grade");
        previewTable.addView(previewHeaderRow);

        // Table Rows (first 5 courses)
        int limit = Math.min(courses.size(), 5);
        for (int i = 0; i < limit; i++) {
            GradeItem course = courses.get(i);
            TableRow courseRow = new TableRow(requireContext());
            courseRow.setBackgroundColor(i % 2 == 0 ? Color.WHITE : getResources().getColor(R.color.light_gray));

            addPreviewCell(courseRow, course.getCourseCode());
            addPreviewCell(courseRow, course.getTitle().length() > 25 ?
                    course.getTitle().substring(0, 25) + "..." : course.getTitle());

            TextView gradeCell = new TextView(requireContext());
            gradeCell.setText(course.getGrade());
            gradeCell.setTextColor(getGradeColor(course.getGrade()));
            gradeCell.setGravity(Gravity.CENTER);
            courseRow.addView(gradeCell);

            previewTable.addView(courseRow);
        }

        if (courses.size() > 5) {
            TextView moreText = new TextView(requireContext());
            moreText.setText(String.format("... and %d more courses", courses.size() - 5));
            moreText.setTextColor(Color.GRAY);
            moreText.setGravity(Gravity.CENTER);
            cardLayout.addView(moreText);
        }

        cardLayout.addView(previewTable);
        semesterGpaContainer.addView(cardLayout);
    }

    // --- PDF Generation ---
    private void generatePdfReport(String semester, List<GradeItem> courses, double gpa) {
        PdfDocument pdfDocument = new PdfDocument();
        int pageWidth = 595;  // A4 width (72 DPI)
        int pageHeight = 842; // A4 height (72 DPI)

        // Create a page
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Paint for text
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);

        // Title
        paint.setTextSize(18);
        paint.setColor(Color.rgb(0, 105, 92)); // Teal 700
        canvas.drawText("Academic Transcript", 50, 50, paint);

        // Student Info
        paint.setTextSize(12);
        String studentName = sharedPref.getString("userName", "Student");
        String studentId = sharedPref.getString("userID", "N/A");
        canvas.drawText("Student: " + studentName, 50, 80, paint);
        canvas.drawText("ID: " + studentId, 50, 100, paint);
        canvas.drawText("Semester: " + semester, 50, 120, paint);
        canvas.drawText("Date: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), 50, 140, paint);

        // Table Headers
        paint.setFakeBoldText(true);
        paint.setColor(Color.rgb(0, 121, 107)); // Teal 600
        canvas.drawText("Course Code", 50, 180, paint);
        canvas.drawText("Title", 150, 180, paint);
        canvas.drawText("Grade", 450, 180, paint);

        // Table Rows
        paint.setFakeBoldText(false);
        int yPos = 200;
        for (GradeItem course : courses) {
            paint.setColor(Color.BLACK);
            canvas.drawText(course.getCourseCode(), 50, yPos, paint);
            canvas.drawText(course.getTitle(), 150, yPos, paint);
            paint.setColor(getGradeColor(course.getGrade()));
            canvas.drawText(course.getGrade(), 450, yPos, paint);
            yPos += 20;
        }

        // Footer (GPA)
        paint.setFakeBoldText(true);
        paint.setColor(Color.rgb(0, 105, 92));
        canvas.drawText("Semester GPA: " + String.format("%.2f", gpa), 50, yPos + 30, paint);

        // Save PDF
        pdfDocument.finishPage(page);
        String fileName = "Grades_" + semester + "_" + System.currentTimeMillis() + ".pdf";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, fileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(requireContext(), "PDF saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error saving PDF", Toast.LENGTH_SHORT).show();
            Log.e("PDF Error", e.getMessage());
        } finally {
            pdfDocument.close();
        }
    }

    // --- Helper Methods ---
    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private double getGradePoints(String grade) {
        switch (grade.toUpperCase()) {
            case "A+": return 4.5;
            case "A": return 4.0;
            case "A-": return 3.7;
            case "B+": return 3.5;
            case "B": return 3.0;
            case "B-": return 2.7;
            case "C+": return 2.5;
            case "C": return 2.0;
            case "C-": return 1.7;
            case "D+": return 1.5;
            case "D": return 1.0;
            case "F": return 0.0;
            default: return -1.0;
        }
    }

    private int getGradeColor(String grade) {
        switch (grade.toUpperCase()) {
            case "A+":
            case "A": return Color.rgb(0, 105, 92); // Teal 700
            case "A-":
            case "B+":
            case "B": return Color.rgb(0, 150, 136); // Teal 500
            case "B-":
            case "C+":
            case "C": return Color.rgb(255, 152, 0); // Orange
            case "C-":
            case "D+":
            case "D": return Color.rgb(244, 67, 54); // Red
            case "F": return Color.rgb(183, 28, 28); // Dark Red
            default: return Color.BLACK;
        }
    }

    // --- Table Cell Helpers ---
    private void addHeaderCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setTextColor(Color.WHITE);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(16, 16, 16, 16);
        row.addView(cell);
    }

    private void addGradeCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(16, 16, 16, 16);
        row.addView(cell);
    }

    private void addPreviewHeaderCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setTypeface(null, Typeface.BOLD);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(8, 8, 8, 8);
        row.addView(cell);
    }

    private void addPreviewCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(8, 6, 8, 6);
        row.addView(cell);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }

    // --- Data Model ---
    public static class GradeItem {
        private final String term, courseCode, title, campus, mode, grade;

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
