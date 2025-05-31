package com.example.uspenrolme.student.applications;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.database.Cursor;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CompassionateAegrotatApplicationFragment extends Fragment {
    private SharedPreference sharedPreference;
    private RequestQueue requestQueue;

    private EditText studentIdEditText, nameEditText, emailEditText, telephoneEditText, dobEditText, postalAddressEditText;
    private EditText reasonEditText, evidenceEditText, signatureEditText, dateEditText;
    private CheckBox declarationCheckbox;
    private Button submitBtn, uploadEvidenceBtn;
    private LinearLayout courseContainer;
    private Button addCourseBtn;
    private final List<View> courseRows = new ArrayList<>();
    private static final int MAX_COURSES = 4;
    private static final int PICK_FILE_REQUEST_CODE = 101;
    private Uri selectedFileUri = null;
    private String uploadedEvidenceUrl = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compassionate_aegrotat_application, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreference = new SharedPreference(requireContext());
        requestQueue = Volley.newRequestQueue(requireContext());

        // Section A
        studentIdEditText = view.findViewById(R.id.student_id_edittext);
        nameEditText = view.findViewById(R.id.name_edittext);
        emailEditText = view.findViewById(R.id.email_edittext);
        telephoneEditText = view.findViewById(R.id.telephone_edittext);
        dobEditText = view.findViewById(R.id.dob_edittext);
        postalAddressEditText = view.findViewById(R.id.postal_address_edittext);

        // Section B - dynamic
        courseContainer = view.findViewById(R.id.course_container);
        addCourseBtn = view.findViewById(R.id.add_course_btn);
        addCourseBtn.setOnClickListener(v -> addCourseRow());
        addCourseRow(); // Add the first row by default

        reasonEditText = view.findViewById(R.id.reason_edittext);
        evidenceEditText = view.findViewById(R.id.evidence_edittext);
        signatureEditText = view.findViewById(R.id.signature_edittext);
        dateEditText = view.findViewById(R.id.date_edittext);
        declarationCheckbox = view.findViewById(R.id.declaration_checkbox);
        submitBtn = view.findViewById(R.id.submit_compassionate_btn);
        uploadEvidenceBtn = view.findViewById(R.id.attach_documents_btn);

        uploadEvidenceBtn.setOnClickListener(v -> pickFile());

        submitBtn.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Submit clicked", Toast.LENGTH_SHORT).show();
            submitApplicationsByType();
        });
    }

    private void addCourseRow() {
        if (courseRows.size() >= MAX_COURSES) return;
        View row = getLayoutInflater().inflate(R.layout.item_missed_exam_row, courseContainer, false);

        Spinner spinner = row.findViewById(R.id.applying_for_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.applying_for_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Button removeBtn = row.findViewById(R.id.remove_course_btn);
        removeBtn.setVisibility(courseRows.size() > 0 ? View.VISIBLE : View.GONE);

        removeBtn.setOnClickListener(v -> {
            courseContainer.removeView(row);
            courseRows.remove(row);
            addCourseBtn.setEnabled(true);
            for (int i = 0; i < courseRows.size(); i++) {
                View r = courseRows.get(i);
                Button btn = r.findViewById(R.id.remove_course_btn);
                btn.setVisibility(courseRows.size() > 1 ? View.VISIBLE : View.GONE);
            }
        });

        courseContainer.addView(row);
        courseRows.add(row);

        if (courseRows.size() == MAX_COURSES) {
            addCourseBtn.setEnabled(false);
        }

        for (int i = 0; i < courseRows.size(); i++) {
            View r = courseRows.get(i);
            Button btn = r.findViewById(R.id.remove_course_btn);
            btn.setVisibility(courseRows.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    // File picker
    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Select Evidence File"), PICK_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            uploadFileToServer(selectedFileUri);
        }
    }

    private void uploadFileToServer(Uri fileUri) {
        if (fileUri == null) return;
        try {
            String fileName = getFileName(fileUri);
            InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri);
            byte[] fileBytes = getBytes(inputStream);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(fileBytes, MediaType.parse(requireContext().getContentResolver().getType(fileUri))))
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                .url("http://10.0.2.2:5000/api/files/upload")
                .post(requestBody)
                .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "File upload failed", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String resp = response.body().string();
                            JSONObject obj = new JSONObject(resp);
                            uploadedEvidenceUrl = obj.getString("fileUrl");
                            requireActivity().runOnUiThread(() -> {
                                evidenceEditText.setText(uploadedEvidenceUrl);
                                Toast.makeText(requireContext(), "File uploaded", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), "Upload error", Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "File upload failed", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "File error", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void submitApplicationsByType() {
    String token = sharedPreference.getValue_string("authToken");

    String studentId = studentIdEditText.getText().toString().trim();
    String reason = reasonEditText.getText().toString().trim();
    String evidence = evidenceEditText.getText().toString().trim();
    String signature = signatureEditText.getText().toString().trim();
    String date = dateEditText.getText().toString().trim();
    boolean declarationChecked = declarationCheckbox.isChecked();

    if (studentId.isEmpty() || reason.isEmpty() || signature.isEmpty() || date.isEmpty() || !declarationChecked) {
        Toast.makeText(requireContext(), "Please fill all required fields and check the declaration.", Toast.LENGTH_SHORT).show();
        return;
    }

    boolean atLeastOne = false;

    for (View row : courseRows) {
        EditText codeEdit = row.findViewById(R.id.course_code_edit);
        EditText dateEdit = row.findViewById(R.id.exam_date_edit);
        EditText timeEdit = row.findViewById(R.id.exam_time_edit);
        Spinner spinner = row.findViewById(R.id.applying_for_spinner);

        String code = codeEdit.getText().toString().trim();
        String examDate = dateEdit.getText().toString().trim();
        String examTime = timeEdit.getText().toString().trim();
        String applyingFor = spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "";

        int applicationTypeId = -1;
        String endpoint = null;
        if (applyingFor.equalsIgnoreCase("Compassionate Pass")) {
            endpoint = "http://10.0.2.2:5000/api/applications/compassionate";
            applicationTypeId = 3;
        } else if (applyingFor.equalsIgnoreCase("Aegrotat Pass")) {
            endpoint = "http://10.0.2.2:5000/api/applications/aegrotat";
            applicationTypeId = 4;
        } else if (applyingFor.equalsIgnoreCase("Special Exam")) {
            endpoint = "http://10.0.2.2:5000/api/applications/special-exam";
            applicationTypeId = 5;
        } else {
            continue;
        }

        if (!code.isEmpty() && !examDate.isEmpty() && !examTime.isEmpty() && applicationTypeId != -1) {
            atLeastOne = true;

            JSONObject data = new JSONObject();
            try {
                data.put("studentId", studentId);
                data.put("reason", reason);
                data.put("courseId", code);
                data.put("examDate", examDate);
                data.put("examTime", examTime);
                data.put("supportingDocsUrl", evidence);
                data.put("applicationTypeId", applicationTypeId); // <-- ADD THIS LINE
            } catch (JSONException e) {
                Toast.makeText(requireContext(), "Error creating request", Toast.LENGTH_SHORT).show();
                continue;
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    endpoint,
                    data,
                    response -> Toast.makeText(requireContext(), "Application submitted for " + applyingFor, Toast.LENGTH_SHORT).show(),
                    error -> {
                        // If the backend actually succeeded, but response parsing failed, treat as success
                        if (error.networkResponse != null && 
                            (error.networkResponse.statusCode == 200 || error.networkResponse.statusCode == 201)) {
                            Toast.makeText(requireContext(), "Application submitted for " + applyingFor, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Failed to submit " + applyingFor + " application", Toast.LENGTH_SHORT).show();
                        }
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
    }

    if (!atLeastOne) {
        Toast.makeText(requireContext(), "Please add at least one valid missed exam.", Toast.LENGTH_SHORT).show();
    }
}
}