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
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TrackApplicationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ApplicationsAdapter adapter;
    private List<ApplicationItem> applications = new ArrayList<>();
    private List<ApplicationItem> filteredApplications = new ArrayList<>();
    private SharedPreference sharedPreference;
    private RequestQueue requestQueue;
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
        requestQueue = Volley.newRequestQueue(requireContext());

        recyclerView = view.findViewById(R.id.applications_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ApplicationsAdapter(filteredApplications);
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
        String token = sharedPreference.getValue_string("authToken");
        String url = "http://10.0.2.2:5000/api/applications?studentId=" + studentId;

        JsonArrayRequest request = new JsonArrayRequest(
            Request.Method.GET, url, null,
            response -> {
                applications.clear();
                for (int i = 0; i < response.length(); i++) {
                    JSONObject obj = response.optJSONObject(i);
                    if (obj != null) {
                        applications.add(new ApplicationItem(
                            obj.optInt("applicationId"),
                            obj.optInt("yearApplied"),
                            obj.optString("studentId"),
                            obj.optString("applicationType"),
                            obj.optString("status"),
                            obj.optString("dateApplied")
                        ));
                    }
                }
                setupTypeFilter();
                filterApplications();
            },
            error -> Toast.makeText(requireContext(), "Failed to load applications", Toast.LENGTH_SHORT).show()
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

    // Populate spinner with unique types
    private void setupTypeFilter() {
        List<String> types = new ArrayList<>();
        types.add("All");
        for (ApplicationItem item : applications) {
            if (!types.contains(item.applicationType)) {
                types.add(item.applicationType);
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
        for (ApplicationItem item : applications) {
            boolean matchesId = searchText.isEmpty() || String.valueOf(item.applicationId).contains(searchText);
            boolean matchesType = selectedType == null || selectedType.equals("All") || item.applicationType.equals(selectedType);
            if (matchesId && matchesType) {
                filteredApplications.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showApplicationDetailsDialog(ApplicationItem item) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_application_details, null);

        ((TextView) dialogView.findViewById(R.id.app_id_field)).setText("Application ID: " + item.applicationId);
        ((TextView) dialogView.findViewById(R.id.year_field)).setText("Year Applied: " + item.yearApplied);
        ((TextView) dialogView.findViewById(R.id.student_id_field)).setText("Student ID: " + item.studentId);
        ((TextView) dialogView.findViewById(R.id.type_field)).setText("Type: " + item.applicationType);
        ((TextView) dialogView.findViewById(R.id.status_field)).setText("Status: " + item.status);
        ((TextView) dialogView.findViewById(R.id.date_field)).setText("Date Applied: " + item.dateApplied);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button closeBtn = dialogView.findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // Data class for an application row
    static class ApplicationItem {
        int applicationId;
        int yearApplied;
        String studentId;
        String applicationType;
        String status;
        String dateApplied;

        ApplicationItem(int applicationId, int yearApplied, String studentId, String applicationType, String status, String dateApplied) {
            this.applicationId = applicationId;
            this.yearApplied = yearApplied;
            this.studentId = studentId;
            this.applicationType = applicationType;
            this.status = status;
            this.dateApplied = dateApplied;
        }
    }

    // RecyclerView Adapter
    class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationsAdapter.AppViewHolder> {
        List<ApplicationItem> items;

        ApplicationsAdapter(List<ApplicationItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_application_row, parent, false);
            return new AppViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
            ApplicationItem item = items.get(position);
            holder.appId.setText(String.valueOf(item.applicationId));
            holder.type.setText(item.applicationType);

            holder.viewBtn.setOnClickListener(v -> {
                showApplicationDetailsDialog(item);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class AppViewHolder extends RecyclerView.ViewHolder {
            TextView appId, type;
            Button viewBtn;
            AppViewHolder(View v) {
                super(v);
                appId = v.findViewById(R.id.app_id_text);
                type = v.findViewById(R.id.type_text);
                viewBtn = v.findViewById(R.id.view_btn);
            }
        }
    }
}