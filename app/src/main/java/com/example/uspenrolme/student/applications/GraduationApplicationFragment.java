package com.example.uspenrolme.student.applications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.CheckBox;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.UtilityService.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class GraduationApplicationFragment extends Fragment {
    private SharedPreference sharedPreference;
    private RequestQueue requestQueue;
    private Button submitBtn;

    // Declare EditTexts and other views
    private EditText studentIdEditText, nameEditText, emailEditText, telephoneEditText, dobEditText, postalAddressEditText;
    private EditText programmeEditText, major1EditText, major2EditText, minorEditText;
    private EditText signatureEditText, dateEditText;
    private RadioGroup programmeTypeRadioGroup;
    private CheckBox declarationCheckbox;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graduation_application, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreference = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        // Bind all views
        studentIdEditText = view.findViewById(R.id.student_id_edittext);
        nameEditText = view.findViewById(R.id.name_edittext);
        emailEditText = view.findViewById(R.id.email_edittext);
        telephoneEditText = view.findViewById(R.id.telephone_edittext);
        dobEditText = view.findViewById(R.id.dob_edittext);
        postalAddressEditText = view.findViewById(R.id.postal_address_edittext);

        programmeTypeRadioGroup = view.findViewById(R.id.programme_type_radiogroup);
        programmeEditText = view.findViewById(R.id.programme_edittext);
        major1EditText = view.findViewById(R.id.major1_edittext);
        major2EditText = view.findViewById(R.id.major2_edittext);
        minorEditText = view.findViewById(R.id.minor_edittext);

        signatureEditText = view.findViewById(R.id.signature_edittext);
        dateEditText = view.findViewById(R.id.date_edittext);
        declarationCheckbox = view.findViewById(R.id.declaration_checkbox);

        submitBtn = view.findViewById(R.id.submit_graduation_btn);

        submitBtn.setOnClickListener(v -> submitGraduationApplication());

        // Auto-fill personal details
        fetchAndFillStudentProfile();
    }

    private void fetchAndFillStudentProfile() {
        String token = sharedPreference.getValue_string("authToken");
        String url = "http://10.0.2.2:5000/api/profile";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
            response -> {
                try {
                    // Log the response to debug structure
                    Log.d("GRAD_APP_PROFILE", "Profile response: " + response.toString());

                    // If your backend returns { profile: { ... } }
                    JSONObject profile = response.has("profile") ? response.getJSONObject("profile") : response;

                    studentIdEditText.setText(profile.optString("student_id", ""));
                    String fullName = profile.optString("first_name", "") + " " + profile.optString("last_name", "");
                    nameEditText.setText(fullName.trim());
                    emailEditText.setText(profile.optString("email", ""));
                    telephoneEditText.setText(profile.optString("phone", ""));
                    dobEditText.setText(profile.optString("dob", ""));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(requireContext(), "Failed to parse profile", Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                error.printStackTrace();
                Toast.makeText(requireContext(), "Failed to fetch profile", Toast.LENGTH_SHORT).show();
            }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void submitGraduationApplication() {
        // Collect data from fields
        String studentId = studentIdEditText.getText().toString().trim();
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String telephone = telephoneEditText.getText().toString().trim();
        String dob = dobEditText.getText().toString().trim();
        String postalAddress = postalAddressEditText.getText().toString().trim();

        String programmeType = "";
        int selectedProgrammeTypeId = programmeTypeRadioGroup.getCheckedRadioButtonId();
        if (selectedProgrammeTypeId != -1) {
            RadioButton selectedRadio = getView().findViewById(selectedProgrammeTypeId);
            programmeType = selectedRadio.getText().toString();
        }

        String programme = programmeEditText.getText().toString().trim();
        String major1 = major1EditText.getText().toString().trim();
        String major2 = major2EditText.getText().toString().trim();
        String minor = minorEditText.getText().toString().trim();

        String signature = signatureEditText.getText().toString().trim();
        String date = dateEditText.getText().toString().trim();
        boolean declarationChecked = declarationCheckbox.isChecked();

        // Basic validation
        if (studentId.isEmpty() || name.isEmpty() || email.isEmpty() || telephone.isEmpty() ||
                dob.isEmpty() || programme.isEmpty() || major1.isEmpty() ||
                signature.isEmpty() || date.isEmpty() || !declarationChecked) {
            Toast.makeText(requireContext(), "Please fill all required fields and check the declaration.", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sharedPreference.getValue_string("authToken");

        String url = "http://10.0.2.2:5000/api/applications/graduation";
        JSONObject data = new JSONObject();
        JSONObject applicationData = new JSONObject();
        try {
            // Put all application fields in applicationData
            applicationData.put("name", name);
            applicationData.put("email", email);
            applicationData.put("telephone", telephone);
            applicationData.put("dob", dob);
            applicationData.put("postalAddress", postalAddress);
            applicationData.put("programmeType", programmeType);
            applicationData.put("programme", programme);
            applicationData.put("major1", major1);
            applicationData.put("major2", major2);
            applicationData.put("minor", minor);
            applicationData.put("signature", signature);
            applicationData.put("date", date);

            // Top-level object
            data.put("studentId", studentId);
            data.put("applicationData", applicationData);
        } catch (JSONException e) {
            Toast.makeText(requireContext(), "Error creating request", Toast.LENGTH_SHORT).show();
            return;
        }

        Logger.logEvent("GraduationApp", "Submitting graduation application for student: " + studentId);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                data,
                response -> {
                    Toast.makeText(requireContext(), "Graduation application submitted", Toast.LENGTH_SHORT).show();
                    Logger.logEvent("GraduationApp", "Graduation application submitted successfully for student: " + studentId);
                    sendGraduationEmailNotification(studentId);
                },
                error -> {
                    Toast.makeText(requireContext(), "Failed to submit application", Toast.LENGTH_SHORT).show();
                    Logger.logEvent("GraduationApp", "Failed to submit graduation application for student: " + studentId);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void sendGraduationEmailNotification(String studentId) {
        // Simulate sending email notification and log it (AOP)
        Logger.logEvent("Notification", "Email notification sent for graduation application: student=" + studentId);
    }
}