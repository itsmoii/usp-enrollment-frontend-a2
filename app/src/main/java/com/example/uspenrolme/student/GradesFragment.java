package com.example.uspenrolme.student;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.graphics.Typeface;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.Button;
import android.print.PrintManager;
import android.print.PrintDocumentAdapter;
import android.print.PrintAttributes;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Context;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.ProgressBar;

public class GradesFragment extends Fragment {

    private SharedPreference sharedPref;
    private RequestQueue requestQueue;
    private TableLayout gradesTable;
    private LinearLayout semesterGpaContainer;
    private ProgressBar loadingIndicator;

    // Navigation components
    private Button gradesTabButton;
    private Button reportsTabButton;
    private LinearLayout gradesSection;
    private LinearLayout reportsSection;

    // Data storage
    private List<GradeItem> allGrades = new ArrayList<>();

    public GradesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_grades, container, false);
        gradesTable = view.findViewById(R.id.gradesTable);
        
        loadGrades();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPref = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        initializeViews(view);
        setupNavigation();
    }

    private void initializeViews(View view) {
        // Navigation buttons
        gradesTabButton = view.findViewById(R.id.gradesTabButton);
        reportsTabButton = view.findViewById(R.id.reportsTabButton);

        // Sections
        gradesSection = view.findViewById(R.id.gradesSection);
        reportsSection = view.findViewById(R.id.reportsSection);

        // Existing components
        gradesTable = view.findViewById(R.id.gradesTable);
        semesterGpaContainer = view.findViewById(R.id.semesterGpaContainer);
    }

    private void setupNavigation() {
        gradesTabButton.setOnClickListener(v -> showGradesSection());
        reportsTabButton.setOnClickListener(v -> showReportsSection());

        // Show grades section by default
        showGradesSection();
    }

    private void showGradesSection() {
        // Update button styles
        gradesTabButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        gradesTabButton.setTextColor(Color.WHITE);
        reportsTabButton.setBackgroundColor(Color.LTGRAY);
        reportsTabButton.setTextColor(Color.BLACK);

        // Show/hide sections
        gradesSection.setVisibility(View.VISIBLE);
        reportsSection.setVisibility(View.GONE);
    }

    private void showReportsSection() {
        // Update button styles
        reportsTabButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        reportsTabButton.setTextColor(Color.WHITE);
        gradesTabButton.setBackgroundColor(Color.LTGRAY);
        gradesTabButton.setTextColor(Color.BLACK);

        // Show/hide sections
        gradesSection.setVisibility(View.GONE);
        reportsSection.setVisibility(View.VISIBLE);

        // Generate reports if we have data
        if (!allGrades.isEmpty()) {
            displayReports(allGrades);
        }
    }

    private void loadGrades() {
        fetchGradesData();
    }

    private void fetchGradesData() {
        showLoading(true);
        String studentId = sharedPref.getString("userID", "N/A");
        String gradesUrl = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;

        JsonArrayRequest gradesRequest = new JsonArrayRequest(
                Request.Method.GET, gradesUrl, null,
                response -> {
                    processGradesResponse(response);
                    showLoading(false);
                },
                error -> {
                    Log.e("GradesFragment", "Error fetching grades", error);
                    Toast.makeText(requireContext(), "Failed to load grades", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                }
        );

        requestQueue.add(gradesRequest);
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void processGradesResponse(JSONArray response) {
        try {
            allGrades.clear();

            for (int i = 0; i < response.length(); i++) {
                JSONObject gradeObj = response.getJSONObject(i);
                String term = gradeObj.optString("term", "N/A");
                String courseCode = gradeObj.optString("CourseID", "N/A");
                String title = gradeObj.optString("title", "N/A");
                String campus = gradeObj.optString("campus", "N/A");
                String mode = gradeObj.optString("mode", "N/A");
                String grade = gradeObj.optString("grade", "N/A");

                allGrades.add(new GradeItem(term, courseCode, title, campus, mode, grade));
            }

            displayGrades(allGrades);
        } catch (JSONException e) {
            Log.e("GradesFragment", "Error parsing grades response", e);
            Toast.makeText(requireContext(), "Error processing grades data", Toast.LENGTH_SHORT).show();
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
                gradeRow.setBackgroundColor(Color.WHITE);
            } else {
                gradeRow.setBackgroundColor(getResources().getColor(R.color.light_gray));
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
            gradeCell.setTextColor(Color.BLACK);
            gradeRow.addView(gradeCell);

            gradesTable.addView(gradeRow);

            // Calculate overall GPA
            double gradePoints = getGradePoints(grade.getGrade());
            if (gradePoints >= 0) {
                totalPoints += gradePoints;
                totalCourses++;
            }
        }

        // Calculate and display overall GPA
        double gpa = totalCourses > 0 ? totalPoints / totalCourses : 0.0;
        TextView gpaTextView = requireView().findViewById(R.id.gpaTextView);
        gpaTextView.setText(String.format("%.2f", gpa));
    }

    private void displayReports(List<GradeItem> grades) {
        // Clear existing semester GPA views
        semesterGpaContainer.removeAllViews();

        // Group grades by semester
        Map<String, List<GradeItem>> semesterGrades = new HashMap<>();
        for (GradeItem grade : grades) {
            String term = grade.getTerm();
            if (!semesterGrades.containsKey(term)) {
                semesterGrades.put(term, new ArrayList<>());
            }
            semesterGrades.get(term).add(grade);
        }

        // Create semester report cards
        for (Map.Entry<String, List<GradeItem>> entry : semesterGrades.entrySet()) {
            String semester = entry.getKey();
            List<GradeItem> semesterCourses = entry.getValue();

            createSemesterReportCard(semester, semesterCourses);
        }
    }

    private void createSemesterReportCard(String semester, List<GradeItem> courses) {
        // Calculate semester GPA
        double totalPoints = 0.0;
        int totalCourses = 0;

        for (GradeItem course : courses) {
            double gradePoints = getGradePoints(course.getGrade());
            if (gradePoints >= 0) {
                totalPoints += gradePoints;
                totalCourses++;
            }
        }

        double semesterGpa = totalCourses > 0 ? totalPoints / totalCourses : 0.0;

        // Create card layout
        LinearLayout cardLayout = new LinearLayout(requireContext());
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(Color.WHITE);
        cardLayout.setPadding(24, 20, 24, 20);

        // Add border effect
        cardLayout.setBackground(getResources().getDrawable(android.R.drawable.dialog_holo_light_frame));

        // Add margin to the card
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 16);
        cardLayout.setLayoutParams(cardParams);

        // Header section
        LinearLayout headerLayout = new LinearLayout(requireContext());
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Semester info layout
        LinearLayout infoLayout = new LinearLayout(requireContext());
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        // Semester title
        TextView semesterText = new TextView(requireContext());
        semesterText.setText("Semester: " + semester);
        semesterText.setTextSize(18);
        semesterText.setTypeface(null, Typeface.BOLD);
        semesterText.setTextColor(getResources().getColor(R.color.teal_700));

        // Course count and GPA
        TextView summaryText = new TextView(requireContext());
        summaryText.setText(String.format("Courses: %d | GPA: %.2f", totalCourses, semesterGpa));
        summaryText.setTextSize(14);
        summaryText.setTextColor(Color.GRAY);

        infoLayout.addView(semesterText);
        infoLayout.addView(summaryText);

        // Print button
        Button printButton = new Button(requireContext());
        printButton.setText("PRINT");
        printButton.setTextSize(12);
        printButton.setBackgroundColor(getResources().getColor(R.color.teal_700));
        printButton.setTextColor(Color.WHITE);
        printButton.setPadding(20, 8, 20, 8);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        printButton.setLayoutParams(buttonParams);

        printButton.setOnClickListener(v -> printSemesterGrades(semester, courses, semesterGpa));

        headerLayout.addView(infoLayout);
        headerLayout.addView(printButton);

        // Courses preview table
        TableLayout previewTable = new TableLayout(requireContext());
        previewTable.setPadding(0, 16, 0, 0);

        // Preview header
        TableRow previewHeaderRow = new TableRow(requireContext());
        previewHeaderRow.setBackgroundColor(getResources().getColor(R.color.teal_200));

        addPreviewHeaderCell(previewHeaderRow, "Course");
        addPreviewHeaderCell(previewHeaderRow, "Title");
        addPreviewHeaderCell(previewHeaderRow, "Grade");

        previewTable.addView(previewHeaderRow);

        // Add course rows (limit to first 5 for preview)
        int courseLimit = Math.min(courses.size(), 5);
        for (int i = 0; i < courseLimit; i++) {
            GradeItem course = courses.get(i);
            TableRow courseRow = new TableRow(requireContext());

            if (i % 2 == 0) {
                courseRow.setBackgroundColor(Color.WHITE);
            } else {
                courseRow.setBackgroundColor(getResources().getColor(R.color.light_gray));
            }

            addPreviewCell(courseRow, course.getCourseCode());
            addPreviewCell(courseRow, course.getTitle().length() > 25 ?
                    course.getTitle().substring(0, 25) + "..." : course.getTitle());

            TextView gradeCell = new TextView(requireContext());
            gradeCell.setText(course.getGrade());
            gradeCell.setTextSize(12);
            gradeCell.setPadding(8, 6, 8, 6);
            gradeCell.setGravity(Gravity.CENTER);
            gradeCell.setTypeface(null, Typeface.BOLD);
            gradeCell.setTextColor(getGradeColor(course.getGrade()));
            courseRow.addView(gradeCell);

            previewTable.addView(courseRow);
        }

        // Show more indicator if there are more courses
        if (courses.size() > 5) {
            TextView moreText = new TextView(requireContext());
            moreText.setText(String.format("... and %d more courses", courses.size() - 5));
            moreText.setTextSize(12);
            moreText.setTextColor(Color.GRAY);
            moreText.setGravity(Gravity.CENTER);
            moreText.setPadding(0, 8, 0, 0);
            cardLayout.addView(moreText);
        }

        cardLayout.addView(headerLayout);
        cardLayout.addView(previewTable);

        semesterGpaContainer.addView(cardLayout);
    }

    private int getGradeColor(String grade) {
        switch (grade.toUpperCase()) {
            case "A+":
            case "A":
                return getResources().getColor(R.color.teal_700);
            case "A-":
            case "B+":
            case "B":
                return getResources().getColor(R.color.teal_500);
            case "B-":
            case "C+":
            case "C":
                return Color.parseColor("#FF9800"); // Orange
            case "C-":
            case "D+":
            case "D":
                return Color.parseColor("#F44336"); // Red
            case "F":
                return Color.parseColor("#B71C1C"); // Dark Red
            default:
                return Color.BLACK;
        }
    }

    private void printSemesterGrades(String semester, List<GradeItem> courses, double gpa) {
        // Create HTML content for printing
        String studentName = sharedPref.getString("userName", "Student");
        String studentId = sharedPref.getString("userID", "N/A");

        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><head><title>Semester Grades Report</title>");
        htmlContent.append("<style>");
        htmlContent.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        htmlContent.append("h1 { color: #00695C; text-align: center; }");
        htmlContent.append("h2 { color: #00796B; }");
        htmlContent.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        htmlContent.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: center; }");
        htmlContent.append("th { background-color: #00796B; color: white; }");
        htmlContent.append("tr:nth-child(even) { background-color: #f9f9f9; }");
        htmlContent.append(".gpa-section { background-color: #e0f2f1; padding: 15px; margin: 20px 0; border-radius: 5px; }");
        htmlContent.append(".student-info { margin-bottom: 20px; }");
        htmlContent.append("</style></head><body>");

        // Header
        htmlContent.append("<h1>Academic Transcript</h1>");
        htmlContent.append("<div class='student-info'>");
        htmlContent.append("<p><strong>Student Name:</strong> ").append(studentName).append("</p>");
        htmlContent.append("<p><strong>Student ID:</strong> ").append(studentId).append("</p>");
        htmlContent.append("<p><strong>Semester:</strong> ").append(semester).append("</p>");
        htmlContent.append("<p><strong>Print Date:</strong> ").append(new java.util.Date().toString()).append("</p>");
        htmlContent.append("</div>");

        // Grades table
        htmlContent.append("<h2>Course Grades</h2>");
        htmlContent.append("<table>");
        htmlContent.append("<tr><th>Course Code</th><th>Course Title</th><th>Campus</th><th>Mode</th><th>Grade</th></tr>");

        for (GradeItem course : courses) {
            htmlContent.append("<tr>");
            htmlContent.append("<td>").append(course.getCourseCode()).append("</td>");
            htmlContent.append("<td>").append(course.getTitle()).append("</td>");
            htmlContent.append("<td>").append(course.getCampus()).append("</td>");
            htmlContent.append("<td>").append(course.getMode()).append("</td>");
            htmlContent.append("<td><strong>").append(course.getGrade()).append("</strong></td>");
            htmlContent.append("</tr>");
        }

        htmlContent.append("</table>");

        // GPA section
        htmlContent.append("<div class='gpa-section'>");
        htmlContent.append("<h2>Semester Summary</h2>");
        htmlContent.append("<p><strong>Total Courses:</strong> ").append(courses.size()).append("</p>");
        htmlContent.append("<p><strong>Semester GPA:</strong> ").append(String.format("%.2f", gpa)).append("</p>");
        htmlContent.append("</div>");

        htmlContent.append("</body></html>");

        // Create WebView for printing
        WebView webView = new WebView(requireContext());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                createWebPrintJob(view, semester);
            }
        });

        webView.loadDataWithBaseURL(null, htmlContent.toString(), "text/html", "UTF-8", null);
    }

    private void createWebPrintJob(WebView webView, String semester) {
        PrintManager printManager = (PrintManager) requireContext().getSystemService(Context.PRINT_SERVICE);
        String jobName = "Semester_Grades_" + semester;

        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);

        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);

        if (printManager != null) {
            printManager.print(jobName, printAdapter, builder.build());
        } else {
            Toast.makeText(requireContext(), "Print service not available", Toast.LENGTH_SHORT).show();
        }
    }

    public double getGradePoints(String grade) {
        switch (grade.toUpperCase()) {
            case "A+":
                return 4.5;
            case "A":
                return 4.0;
            case "A-":
                return 3.7;
            case "B+":
                return 3.5;
            case "B":
                return 3.0;
            case "B-":
                return 2.7;
            case "C+":
                return 2.5;
            case "C":
                return 2.0;
            case "C-":
                return 1.7;
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
        cell.setPadding(20, 20, 20, 20);
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
        cell.setPadding(20, 20, 20, 20);
        cell.setGravity(Gravity.CENTER);
        cell.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));
        row.addView(cell);
    }

    private void addPreviewHeaderCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setTextSize(12);
        cell.setTypeface(null, Typeface.BOLD);
        cell.setTextColor(Color.BLACK);
        cell.setPadding(8, 8, 8, 8);
        cell.setGravity(Gravity.CENTER);
        row.addView(cell);
    }

    private void addPreviewCell(TableRow row, String text) {
        TextView cell = new TextView(requireContext());
        cell.setText(text);
        cell.setTextSize(11);
        cell.setPadding(8, 6, 8, 6);
        cell.setGravity(Gravity.CENTER);
        row.addView(cell);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }

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
}
