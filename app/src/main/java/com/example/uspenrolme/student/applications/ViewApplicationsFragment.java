package com.example.uspenrolme.student.applications;

import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.models.ApplicationsModel;
import com.example.uspenrolme.adapters.ApplicationsAdapter;
import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import androidx.fragment.app.Fragment;
import com.example.uspenrolme.R;

public class ViewApplicationsFragment extends Fragment implements ApplicationsAdapter.OnApplicationClickListener {

    private RecyclerView recyclerView;
    private ApplicationsAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyText;
    private ImageView backBtn;
    private SharedPreference sharedPreference;
    private ArrayList<ApplicationsModel> applicationList = new ArrayList<>();

    public ViewApplicationsFragment(){

        super(R.layout.fragment_applications_view_all);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreference = new SharedPreference(requireContext());

        recyclerView = view.findViewById(R.id.application_rc);
        progressBar = view.findViewById(R.id.application_progress);
        emptyText = view.findViewById(R.id.empty_applications);
        backBtn = view.findViewById(R.id.application_backBtn);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ApplicationsAdapter(applicationList, this);

        recyclerView.setAdapter(adapter);
        backBtn.setOnClickListener(v -> requireActivity().onBackPressed());

        fetchApplications();
    }


    private void fetchApplications() {
        progressBar.setVisibility(View.VISIBLE);

        //SharedPreferences sharedPreference = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        //String studentId = sharedPreference.getString("studentId", null);

        String studentId = sharedPreference.getValue_string("userID");
        Log.d("DEBUG", "Student ID: " + studentId);
        
        //String token = sharedPreference.getValue_string("authToken");

        if (studentId == null) {
            Toast.makeText(requireContext(), "Student ID not found in SharedPreferences", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(requireContext(), "Student ID: " + studentId, Toast.LENGTH_LONG).show();
        }
        if (studentId == null) {
            progressBar.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText("Student ID not found");
            return;
        }

        String url = "http://10.0.2.2:5000/api/applications/view-all-applications/" + studentId;
        Log.d("DEBUG", "Requesting URL: " + url);
        
        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
            Log.d("DEBUG", "Response Received: " + response.length() + "items.");
                    progressBar.setVisibility(View.GONE);

                    applicationList.clear();
                    emptyText.setVisibility(View.GONE);

                    if (response.length() == 0) {
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            ApplicationsModel app = new ApplicationsModel(
                                    obj.getInt("id"),
                                    obj.getString("submitted_at"),
                                    obj.getString("application_type"),
                                    obj.getString("status")
                            );
                            applicationList.add(app);
                        }
                        adapter.notifyDataSetChanged();
                        Log.d("DEBUG", "Application List Size: " + applicationList.size());
                    } catch (Exception e) {
                        Log.e("DEBUG", "Application Failed.");
                        e.printStackTrace();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(), "Failed to load applications", Toast.LENGTH_SHORT).show();
                    Log.e("DEBUG", "Volley error: " + error.toString());
                });

        Volley.newRequestQueue(requireContext()).add(request);


        
    }

    @Override
    public void onApplicationClick(ApplicationsModel model) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_application_details, null);

        ((TextView) dialogView.findViewById(R.id.app_id_field)).setText("Application ID: " + model.getId());
        ((TextView) dialogView.findViewById(R.id.type_field)).setText("Type: " + model.getType());
        ((TextView) dialogView.findViewById(R.id.status_field)).setText("Status: " + model.getStatus());
        ((TextView) dialogView.findViewById(R.id.date_field)).setText("Date Applied: " + model.getDate());

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.close_btn).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

}
