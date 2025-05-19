package com.example.uspenrolme.student.applications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.student.finance.FinanceMenu;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ApplicationGradeReCheckFragment extends Fragment{
    private SharedPreference sharedPreference;
    private RequestQueue requestQueue;
    private Spinner completedCoursesSpinner;
    private Button continueBtn, viewApplicationsBtn;
    private LinearLayout courseInfoLayout, succesLayout, newStudentMessageLayout;
    private TextView courseNameTextView, gradeTextView, recieptNumberTextView, courseCodeTextView, courseTermTextView, spinnerInstructionTextView, gradeRecheckInstructionTextView, lecturerNameTextView, reasonTextView;
    private EditText lecturerNameEditText, reasonEditText, recieptNumberEditView;
    private Button submitRecheckBtn, backBtn;

    private List<String> completedCourses = new ArrayList<>();
    private String selectedCourse;
    private String courseGrade;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_application_grade_recheck,container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        sharedPreference = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        completedCoursesSpinner = view.findViewById(R.id.completed_courses_spinner);
        continueBtn = view.findViewById(R.id.continue_btn);
        courseInfoLayout = view.findViewById(R.id.courseInfoLayout);
        courseNameTextView = view.findViewById(R.id.courseNameTextView);
        gradeTextView = view.findViewById(R.id.gradeTextView);
        courseCodeTextView = view.findViewById(R.id.courseCodeTextView);
        courseTermTextView = view.findViewById(R.id.courseTermTextView);
        lecturerNameEditText = view.findViewById(R.id.lecturerNameEditText);
        reasonEditText = view.findViewById(R.id.reasonEditText);
        lecturerNameTextView = view.findViewById(R.id.lecturerNameTextView);
        reasonTextView = view.findViewById(R.id.reasonTextView);
        submitRecheckBtn = view.findViewById(R.id.submitRecheckButton);
        spinnerInstructionTextView = view.findViewById(R.id.spinner_instruction_textview);
        gradeRecheckInstructionTextView = view.findViewById(R.id.grade_recheck_app_instruction_textview);
        succesLayout = view.findViewById(R.id.successLayout);
        backBtn = requireView().findViewById(R.id.go_back_btn);
        newStudentMessageLayout = view.findViewById(R.id.new_student_message_layout);
        viewApplicationsBtn = view.findViewById(R.id.viewDetailsButton);
        recieptNumberTextView = view.findViewById(R.id.recieptNumberTextView);
        recieptNumberEditView =view.findViewById(R.id.recieptNumberEditText);

        newStudentMessageLayout.setVisibility(View.GONE);
        // Fetch completed courses
        fetchCompletedCourses();

        // Set onClickListener for Continue button
        continueBtn.setOnClickListener(v -> {
            Log.d("GradeReCheck", "Continue button clicked");
            Toast.makeText(requireContext(), "Loading course details...", Toast.LENGTH_SHORT).show();
            showCourseInfo();
        });

        succesLayout.setVisibility(View.GONE);
        gradeRecheckInstructionTextView.setVisibility(View.GONE);
        // Set onClickListener for Submit Recheck button
        submitRecheckBtn.setOnClickListener(v -> submitRecheckRequest());
    }

    private void fetchCompletedCourses() {
        String studentId = sharedPreference.getValue_string("userID");
        String url = "http://10.0.2.2:5000/api/completed-courses?studentId=" + studentId;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, url, null,
                this::processCompletedCoursesResponse,
                error -> Log.e("GradeReCheck", "Error fetching completed courses", error)
        );

        requestQueue.add(request);
    }

    private void processCompletedCoursesResponse(JSONArray response) {
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject courseObj = response.getJSONObject(i);
                String courseCode = courseObj.optString("CourseID", "N/A");
                completedCourses.add(courseCode);
            }
            if(completedCourses.isEmpty()){
                completedCoursesSpinner.setVisibility(View.GONE);
                continueBtn.setVisibility(View.GONE);
                spinnerInstructionTextView.setVisibility(View.GONE);
                newStudentMessageLayout.setVisibility(View.VISIBLE);

                backBtn.setOnClickListener(v -> {
                    // Replace with your actual fragment transaction code
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content, new StudentApplicationsFragment()).commit();
                });
            }

            // Set adapter for Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, completedCourses);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            completedCoursesSpinner.setAdapter(adapter);
        } catch (JSONException e) {
            Log.e("GradeReCheck", "Error parsing completed courses", e);
        }
    }

    private void openFragment(Fragment fragment){

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .addToBackStack(null)
                .commit();

    }

    private void showCourseInfo(){
        if(completedCoursesSpinner.getSelectedItem() == null){
            Toast.makeText(requireContext(), "Please select a course", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedCourse = completedCoursesSpinner.getSelectedItem().toString();
        String studentId = sharedPreference.getValue_string("userID");
        String courseUrl = "http://10.0.2.2:5000/api/course-details/" + studentId + "/" + selectedCourse;
        Log.d("GradeReCheck", "Course URL: " + courseUrl);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, courseUrl, null,
                response -> {
                    // Extract values safely
                    String courseCode = response.optString("course_code", "N/A");
                    String courseName = response.optString("course_name", "N/A");
                    String courseTerm = response.optString("term", "N/A");
                    courseGrade = response.optString("grade", "N/A");

                    // Set data to text views
                    courseCodeTextView.setText("Course Code: " + courseCode);
                    courseNameTextView.setText("Course Name: " + courseName);
                    courseTermTextView.setText("Term: " + courseTerm);
                    gradeTextView.setText("Grade: " + courseGrade);

                    // Show course info layout
                    spinnerInstructionTextView.setVisibility(View.GONE);
                    gradeRecheckInstructionTextView.setVisibility(View.VISIBLE);
                    courseInfoLayout.setVisibility(View.VISIBLE);
                    completedCoursesSpinner.setVisibility(View.GONE);
                    continueBtn.setVisibility(View.GONE);
                    Log.d("GradeReCheck", "API Response: " + response.toString());
                },
                error -> {

                    Log.e("GradeReCheck", "Error fetching course details", error);
                    Toast.makeText(requireContext(), "Failed to load course details", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(request);
    }

private void submitRecheckRequest() {
    String lecturerName = lecturerNameEditText.getText().toString().trim();
    String reason = reasonEditText.getText().toString().trim();
    String recieptNumber = recieptNumberEditView.getText().toString().trim();
    String termText = courseTermTextView.getText().toString().trim();
    String term = termText.replace("Term: ", "").trim();

    if (lecturerName.isEmpty() || reason.isEmpty() || recieptNumber.isEmpty()) {
        Toast.makeText(requireContext(), "Please provide all details", Toast.LENGTH_SHORT).show();
        return;
    }

    if (!recieptNumber.matches("^\\d{13}$")) { //ensure that recipt number only contains 13 digits adn no extra characters
        Toast.makeText(requireContext(), "Receipt number must be exactly 13 digits & not other special characters", Toast.LENGTH_SHORT).show();
        return;
    }

    String studentId = sharedPreference.getValue_string("userID");
    String token = sharedPreference.getValue_string("authToken"); // Make sure this was saved during login

//    if (token == null || token.isEmpty()) {
//        Toast.makeText(requireContext(), "Authentication token missing. Please login again.", Toast.LENGTH_LONG).show();
//        return;
//    }

    String recheckUrl = "http://10.0.2.2:5000/api/submit-recheck";
    JSONObject recheckData = new JSONObject();

    try {
        recheckData.put("studentId", studentId);
        recheckData.put("courseCode", selectedCourse);
        recheckData.put("lecturerName", lecturerName);
        recheckData.put("reason", reason);
        recheckData.put("term", term);
        recheckData.put("recieptNumber", recieptNumber);
    } catch (JSONException e) {
        Log.e("GradeReCheck", "Error creating recheck data", e);
        Toast.makeText(requireContext(), "Error creating request data", Toast.LENGTH_SHORT).show();
        return;
    }

    JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.POST,
            recheckUrl,
            recheckData,
            response -> {
                Log.d("GradeReCheck", "Recheck submitted: " + response.toString());
                Toast.makeText(requireContext(), "Recheck request submitted", Toast.LENGTH_SHORT).show();
                gradeRecheckInstructionTextView.setVisibility(View.GONE);
                courseInfoLayout .setVisibility(View.GONE);
                succesLayout.setVisibility(View.VISIBLE);

                viewApplicationsBtn.setOnClickListener(v -> {
                    // Replace with your actual fragment transaction code
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.content, new ViewApplicationsFragment()).commit();

                });
            },
            error -> {
                Log.e("GradeReCheck", "Error submitting recheck", error);
                Toast.makeText(requireContext(), "Failed to submit recheck request", Toast.LENGTH_SHORT).show();
            }
    ) {
        @Override
        public java.util.Map<String, String> getHeaders() {
            java.util.Map<String, String> headers = new java.util.HashMap<>();
            headers.put("Authorization", "Bearer " + token); // Attach JWT
            headers.put("Content-Type", "application/json");
            return headers;
        }
    };

    requestQueue.add(request);
}

}
