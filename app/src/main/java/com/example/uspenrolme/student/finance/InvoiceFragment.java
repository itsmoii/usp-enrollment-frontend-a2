package com.example.uspenrolme.student.finance;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.adapters.InvoiceAdapter;
import com.example.uspenrolme.models.InvoiceModel;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import com.stripe.android.paymentsheet.PaymentSheetResultCallback;


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
    TextView totalPayments;
    TextView balance;
    ProgressBar progressBar;
    ArrayList<InvoiceModel> arrayList;
    SharedPreference sharedPreference;
    String token;
    InvoiceAdapter invoiceAdapter;
    Button payBtn;
    double totalAmt;
    double totalPaid;
    double totalBalance;

    LinearLayout paymentLayout;

    PaymentSheet paymentSheet;
    String paymentIntentClientSecret;
    PaymentSheet.CustomerConfiguration configuration;


    public InvoiceFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize paymentSheet here
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_invoice, container, false);


        emptyText = view.findViewById(R.id.empty_invoice);
        total = view.findViewById(R.id.invoice_total);
        totalPayments = view.findViewById(R.id.payments_total);
        balance = view.findViewById(R.id.balance_total);
        progressBar = view.findViewById(R.id.invoice_progress);
        sharedPreference = new SharedPreference(getContext());
        token = sharedPreference.getValue_string("token");
        backBtn = view.findViewById(R.id.invoice_backBtn);
        payBtn = view.findViewById(R.id.pay_btn);
        paymentLayout = view.findViewById(R.id.payment_layout);



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


        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret,
                        new PaymentSheet.Configuration("USP", configuration));
            }
        });


        return view;
    }

    public void fetchAPI(){

        String apiKey ="http://10.0.2.2:5000/api/stripe/payment-sheet";

        JSONObject body = new JSONObject();
        try {
            int amountInCents = (int) (totalBalance * 100);
            body.put("amount", amountInCents);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {

                    configuration = new PaymentSheet.CustomerConfiguration(
                            response.getString("customer"),
                            response.getString("ephemeralKey")
                    );

                    paymentIntentClientSecret = response.getString("paymentIntent");
                    PaymentConfiguration.init(getContext(), response.getString("publishableKey"));

                    if (totalBalance > 0){
                            payBtn.setVisibility(View.VISIBLE);
                    }

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if(error instanceof ServerError && response != null) {
                    try {
                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers,  "utf-8"));
                        JSONObject obj = new JSONObject(res);
                        Toast.makeText(getActivity(), obj.getString("msg"), Toast.LENGTH_SHORT).show();
                    } catch (JSONException | UnsupportedEncodingException je) {
                        je.printStackTrace();
                    }
                }
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


    };



    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult){
        if(paymentSheetResult instanceof PaymentSheetResult.Canceled){
            Toast.makeText(getContext(), "Payment cancelled" ,Toast.LENGTH_SHORT).show();
        }
        if(paymentSheetResult instanceof PaymentSheetResult.Failed){
            Toast.makeText(getContext(), ((PaymentSheetResult.Failed) paymentSheetResult).getError().getMessage() ,Toast.LENGTH_SHORT).show();
        }
        if(paymentSheetResult instanceof PaymentSheetResult.Completed){
            Toast.makeText(getContext(), "Payment successful" ,Toast.LENGTH_SHORT).show();
            getInvoices();
            paymentLayout.setVisibility(View.GONE);
        }
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

                        totalAmt = response.getJSONObject("data").getInt("total");
                        totalPaid = response.getJSONObject("data").getInt("totalPaid");
                        totalBalance = response.getJSONObject("data").getInt("totalPayable");

                        total.setText(String.format("$%.2f", totalAmt));
                        totalPayments.setText(String.format("$%.2f", totalPaid));
                        balance.setText(String.format("$%.2f", totalBalance));

                        fetchAPI();
                        Log.d("Amounttttt", String.valueOf(totalAmt));

                        if (totalBalance > 0){
                            paymentLayout.setVisibility(View.VISIBLE);
                        }



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