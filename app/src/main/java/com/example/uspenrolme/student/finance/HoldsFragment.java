package com.example.uspenrolme.student.finance;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.example.uspenrolme.adapters.InvoiceAdapter;
import com.example.uspenrolme.models.InvoiceModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class HoldsFragment extends Fragment {

    TextView holdReason;
    TextView holdPlaced;
    TextView holdOverdue;
    TextView holdDue;
    double balance;
    SharedPreference sharedPreference;
    String token;
    boolean onHold;
    LinearLayout studentHold;
    LinearLayout noStudentHold;


    public HoldsFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_holds, container, false);

        holdReason = view.findViewById(R.id.holdReason);
        holdPlaced = view.findViewById(R.id.holdPlaced);
        holdOverdue = view.findViewById(R.id.holdOverdue);
        holdDue = view.findViewById(R.id.holdDue);
        studentHold = view.findViewById(R.id.onhold);
        noStudentHold = view.findViewById(R.id.nohold);
        sharedPreference = new SharedPreference(getContext());
        token = sharedPreference.getValue_string("token");

        checkHold();
        return view;
    }

    public void checkHold(){
        String API = "http://10.0.2.2:5000/api/microservice/check-hold";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, API, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    JSONObject data = response.getJSONObject("hold");

                    onHold = data.getBoolean("hold");

                    if (onHold){
                        studentHold.setVisibility(View.VISIBLE);
                        getBalance();
                    }else{
                        noStudentHold.setVisibility(View.VISIBLE);
                    }

                }catch (JSONException e){
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Failed to check hold", Toast.LENGTH_SHORT).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();


                NetworkResponse response = error.networkResponse;
                if(error instanceof ServerError && response != null){
                    try{

                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                        JSONObject obj = new JSONObject(res);
                        Toast.makeText(getActivity(), obj.getString("msg"), Toast.LENGTH_SHORT).show();

                    }catch(JSONException | UnsupportedEncodingException je){
                        je.printStackTrace();
                    }
                }

                if (response != null && response.statusCode == 401){
                    Toast.makeText(getActivity(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
                }

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + token);

                return headers;
            }
        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        jsonObjectRequest.setRetryPolicy(policy);

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);

    }

    public void getBalance(){
        String API = "http://10.0.2.2:5000/api/finance/invoices";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, API, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    if (response.getBoolean("success")){
                        JSONObject data = response.getJSONObject("data");

                        balance = data.getInt("totalPayable");
                        holdOverdue.setText(String.format("$%.2f", balance));
                        holdPlaced.setText("24/03/25");
                        holdReason.setText("Non payment of fees");
                        holdDue.setText("21/03/25");


                        Log.d("Amounttttt", String.valueOf(balance));

                    }else{
                        Toast.makeText(getActivity(), "Failed to fetch balance", Toast.LENGTH_SHORT).show();
                    }

                }catch (JSONException e){
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();


                NetworkResponse response = error.networkResponse;
                if(error instanceof ServerError && response != null){
                    try{

                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                        JSONObject obj = new JSONObject(res);
                        Toast.makeText(getActivity(), obj.getString("msg"), Toast.LENGTH_SHORT).show();

                    }catch(JSONException | UnsupportedEncodingException je){
                        je.printStackTrace();
                    }
                }

                if (response != null && response.statusCode == 401){
                    Toast.makeText(getActivity(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
                }

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + token);

                return headers;
            }
        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        jsonObjectRequest.setRetryPolicy(policy);

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);

    }











}