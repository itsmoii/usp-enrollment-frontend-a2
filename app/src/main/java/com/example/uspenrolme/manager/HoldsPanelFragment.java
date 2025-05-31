package com.example.uspenrolme.manager;

import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class HoldsPanelFragment extends Fragment {

    ImageView backBtn;
    Button holdBtn;
    SwitchCompat holdReg;
    SwitchCompat holdGrades;
    SwitchCompat holdAudit;
    SwitchCompat holdRecheck;
    SwitchCompat holdGraduation;

    SharedPreference sharedPreference;
    String token;


    public HoldsPanelFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_holds_panel, container, false);

        fetchRules();

        holdBtn = view.findViewById(R.id.applyhold_btn);
        holdReg = view.findViewById(R.id.hold_registration);
        holdGrades = view.findViewById(R.id.hold_grades);
        holdAudit = view.findViewById(R.id.hold_audit);
        holdRecheck = view.findViewById(R.id.hold_recheck);
        holdGraduation = view.findViewById(R.id.hold_graduation);
        backBtn = view.findViewById(R.id.holds_backBtn);
        sharedPreference = new SharedPreference(getContext());
        token = sharedPreference.getValue_string("token");

        holdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateRules();
                applyHold();
            }
        });

        return view;
    }

    // Fetch the hold rules stored in microservice. frontend -> backend -> microservice
    public void fetchRules(){

        String apiKey = "http://10.0.2.2:5000/api/microservice/rules";


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiKey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    holdReg.setChecked(response.getBoolean("registration"));
                    holdGrades.setChecked(response.getBoolean("grades"));
                    holdAudit.setChecked(response.getBoolean("audit"));
                    holdRecheck.setChecked(response.getBoolean("recheck"));
                    holdGraduation.setChecked(response.getBoolean("graduation"));

                }catch(JSONException e){
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Error parsing response", Toast.LENGTH_SHORT).show();

                }
                Toast.makeText(getActivity(), "Rules loaded!", Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message;
                if (error.networkResponse != null) {
                    message = "Error code: " + error.networkResponse.statusCode;
                } else {
                    message = error.getMessage();
                }
                Toast.makeText(getActivity(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                return headers;
            }

        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        // request add
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);

    }

    // Update rules
    public void updateRules(){

        String apiKey = "http://10.0.2.2:5000/api/microservice/rules";

        JSONObject body = new JSONObject();

        try{
            body.put("registration", holdReg.isChecked());
            body.put("grades", holdGrades.isChecked());
            body.put("audit", holdAudit.isChecked());
            body.put("recheck", holdRecheck.isChecked());
            body.put("graduation", holdGraduation.isChecked());

        }catch(JSONException e){
            e.printStackTrace();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(getActivity(), "Rules updated!", Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message;
                if (error.networkResponse != null) {
                    message = "Error code: " + error.networkResponse.statusCode;
                } else {
                    message = error.getMessage();
                }
                Toast.makeText(getActivity(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                return headers;
            }

        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        // request add
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);

    }

    public void applyHold(){

        String apiKey = "http://10.0.2.2:5000/api/finance/place-hold";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(getActivity(), "Hold applied", Toast.LENGTH_SHORT).show();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message;
                if (error.networkResponse != null) {
                    message = "Error code: " + error.networkResponse.statusCode;
                } else {
                    message = error.getMessage();
                }
                Toast.makeText(getActivity(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + token);

                return headers;
            }

        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        // request add
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);


    }








}