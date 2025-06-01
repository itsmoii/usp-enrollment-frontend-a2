package com.example.uspenrolme.student.applications;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.models.ApplicationsModel;
import com.example.uspenrolme.adapters.ApplicationsAdapter;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TrackApplicationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApplicationsAdapter adapter;
    private ArrayList<ApplicationsModel> applications = new ArrayList<>();
    private ArrayList<ApplicationsModel> filteredApplications = new ArrayList<>();
    private SharedPreference sharedPreference;
    private EditText searchEditText;
    private Spinner typeFilterSpinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track_applications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreference = new SharedPreference(requireContext());

        recyclerView = view.findViewById(R.id.applications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ApplicationsAdapter(filteredApplications, this::showApplicationDetailsDialog);
        recyclerView.setAdapter(adapter);

        searchEditText = view.findViewById(R.id.search_edit_text);
        typeFilterSpinner = view.findViewById(R.id.type_filter_spinner);

        fetchApplications();

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApplications();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter functionality
        typeFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterApplications();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchApplications() {
        String studentId = sharedPreference.getValue_string("userID");
        if (studentId == null) return;

        String url = "http://10.0.2.2:5000/api/applications/view-all-applications/" + studentId;

        JsonArrayRequest request = new JsonArrayRequest(
            Request.Method.GET, url, null,
            response -> {
                applications.clear();
                for (int i = 0; i < response.length(); i++) {
                    JSONObject obj = response.optJSONObject(i);
                    if (obj != null) {
                        ApplicationsModel app = new ApplicationsModel(
                            obj.optInt("id"),
                            obj.optString("submitted_at"),
                            obj.optString("type"),
                            obj.optString("status")
                        );
                        applications.add(app);
                    }
                }
                setupTypeFilter();
                filterApplications();
            },
            error -> Toast.makeText(requireContext(), "Failed to load applications", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    // Populate spinner with unique types
    private void setupTypeFilter() {
        List<String> types = new ArrayList<>();
        types.add("All");
        for (ApplicationsModel item : applications) {
            if (!types.contains(item.getType())) {
                types.add(item.getType());
            }
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, types);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeFilterSpinner.setAdapter(spinnerAdapter);
    }

    // Filter applications based on search and spinner
    private void filterApplications() {
        String searchText = searchEditText.getText().toString().trim();
        String selectedType = (String) typeFilterSpinner.getSelectedItem();
        filteredApplications.clear();
        for (ApplicationsModel item : applications) {
            boolean matchesId = searchText.isEmpty() || String.valueOf(item.getId()).contains(searchText);
            boolean matchesType = selectedType == null || selectedType.equals("All") || item.getType().equals(selectedType);
            if (matchesId && matchesType) {
                filteredApplications.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    // THIS IS THE DIALOG THAT SHOWS WHEN "VIEW" IS CLICKED
    private void showApplicationDetailsDialog(ApplicationsModel item) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_application_details, null);

        ((TextView) dialogView.findViewById(R.id.app_id_field)).setText("Application ID: " + item.getId());
        ((TextView) dialogView.findViewById(R.id.type_field)).setText("Type: " + item.getType());
        ((TextView) dialogView.findViewById(R.id.status_field)).setText("Status: " + item.getStatus());
        ((TextView) dialogView.findViewById(R.id.date_field)).setText("Date Applied: " + item.getDate());

        // If you have year and student ID, set them here as well:
        // ((TextView) dialogView.findViewById(R.id.year_field)).setText("Year Applied: " + item.getYear());
        // ((TextView) dialogView.findViewById(R.id.student_id_field)).setText("Student ID: " + item.getStudentId());

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button closeBtn = dialogView.findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}