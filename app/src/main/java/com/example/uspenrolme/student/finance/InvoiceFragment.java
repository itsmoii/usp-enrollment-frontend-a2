package com.example.uspenrolme.student.finance;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.example.uspenrolme.manager.ManagerDashboardAcitivity;
import com.example.uspenrolme.models.InvoiceModel;
import com.example.uspenrolme.shared.LoginActivity;
import com.example.uspenrolme.student.StudentDashboardActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class InvoiceFragment extends Fragment {

    RecyclerView recyclerView;

    ImageView backBtn;
    TextView emptyText;
    TextView total;
    ProgressBar progressBar;
    ArrayList<InvoiceModel> arrayList;
    SharedPreference sharedPreference;
    String token;
    InvoiceAdapter invoiceAdapter;

    public InvoiceFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_invoice, container, false);


        emptyText = view.findViewById(R.id.empty_invoice);
        total = view.findViewById(R.id.invoice_total);
        progressBar = view.findViewById(R.id.invoice_progress);
        sharedPreference = new SharedPreference(getContext());
        token = sharedPreference.getValue_string("token");
        backBtn = view.findViewById(R.id.invoice_backBtn);

        recyclerView = view.findViewById(R.id.invoice_rc);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.content, new FinanceMenu());
                fragmentTransaction.commit();
            }
        });


        getInvoices();




        return view;
    }

    public void getInvoices(){

        arrayList = new ArrayList<>();
        progressBar.setVisibility(View.VISIBLE);

        String API = "http://10.0.2.2:5000/api/finance/invoices";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, API, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.d("Login", "Response: " + response.toString());

                try{
                    if (response.getBoolean("success")){

                        JSONArray jsonArray = response.getJSONObject("data").getJSONArray("invoices");

                        double totalAmt = response.getJSONObject("data").getInt("total");
                        total.setText(String.format("$%.2f", totalAmt));


                        for(int i=0; i < jsonArray.length(); i++){
                            JSONObject jsonObject = jsonArray.getJSONObject(i);

                            InvoiceModel invoiceModel = new InvoiceModel(
                                    jsonObject.getString("course_name"),
                                    jsonObject.getString("course_code"),
                                    jsonObject.getString("course_mode"),
                                    jsonObject.getString("course_campus"),
                                    jsonObject.getInt("semester"),
                                    jsonObject.getInt("course_level"),
                                    jsonObject.getString("due_date"),
                                    jsonObject.getDouble("price")
                            );

                            arrayList.add(invoiceModel);
                        }

                        invoiceAdapter = new InvoiceAdapter(getActivity(), arrayList);
                        recyclerView.setAdapter(invoiceAdapter);



                    }else{
                        Toast.makeText(getActivity(), "Failed to fetch invoices", Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                }catch (JSONException e){

                    e.printStackTrace();
                    progressBar.setVisibility(View.GONE);

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
                        progressBar.setVisibility(View.GONE);

                    }catch(JSONException | UnsupportedEncodingException je){

                        je.printStackTrace();
                        progressBar.setVisibility(View.GONE);

                    }
                }

                progressBar.setVisibility(View.GONE);

                if (response != null && response.statusCode == 401){
                    Toast.makeText(getActivity(), "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
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