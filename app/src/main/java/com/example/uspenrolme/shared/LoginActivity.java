package com.example.uspenrolme.shared;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.UtilityService.UtilService;
import com.example.uspenrolme.manager.ManagerDashboardActivity;
import com.example.uspenrolme.student.StudentDashboardActivity;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private Button loginBtn;
    private EditText username_ET, password_ET;
    ProgressBar progressBar;
    private String username, password;

    UtilService utilService;

    SharedPreference sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loginBtn = findViewById(R.id.loginBtn);
        username_ET = findViewById(R.id.username_et);
        password_ET = findViewById(R.id.password_et);
        progressBar = findViewById(R.id.progress_bar);
        utilService = new UtilService();
        sharedPref = new SharedPreference(this);

        TextInputLayout passwordInputLayout = findViewById(R.id.password_input_layout);
        passwordInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        passwordInputLayout.setEndIconDrawable(R.drawable.visibility_off);

        passwordInputLayout.setEndIconOnClickListener(new View.OnClickListener() {
            boolean isPasswordVisible = false;

            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    password_ET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    passwordInputLayout.setEndIconDrawable(R.drawable.visibility_off);
                } else {
                    password_ET.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    passwordInputLayout.setEndIconDrawable(R.drawable.visibility_on);
                }
                isPasswordVisible = !isPasswordVisible;
                password_ET.setSelection(password_ET.getText().length());
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                utilService.hideKeyboard(view, LoginActivity.this);
                username = username_ET.getText().toString();
                password = password_ET.getText().toString();

                if(validate(view)){

                    loginUser(view);

                }

            }
        });

    }

    // Validating user credentials in backend
    private  void loginUser(View view){

        progressBar.setVisibility(View.VISIBLE);


        HashMap<String, String> params = new HashMap<>();
        params.put("userId", username);
        params.put("password", password);

        String apiKey = "http://10.0.2.2:5000/api/auth/login";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                Log.d("Login", "Response: " + response.toString());

                try{
                    if (response.getBoolean("success")){
                        String token = response.getString("token");
                        String role = response.getString("role");

                        sharedPref.setValue_string("token", token);
                        sharedPref.setValue_string("role", role);
                        sharedPref.setValue_string("userID", username);

                        Log.d("sharedprefdebug", "storedUserID" +  sharedPref.getValue_string("userID"));

                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                        if (role.equals("student")){
                            startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class));
                        }else if (role.equals("manager")){
                            startActivity(new Intent(LoginActivity.this, ManagerDashboardActivity.class));
                        }


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
                Log.e("Login", "Error: " + error.toString()); // Log the error
                NetworkResponse response = error.networkResponse;
                if(error instanceof ServerError && response != null){
                    try{

                        String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));

                        JSONObject obj = new JSONObject(res);
                        Toast.makeText(LoginActivity.this, obj.getString("msg"), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);

                    }catch(JSONException |UnsupportedEncodingException je){

                        je.printStackTrace();
                        progressBar.setVisibility(View.GONE);

                    }
                }

                if (response != null && response.statusCode == 401){
                    Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                return params;
            }
        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        jsonObjectRequest.setRetryPolicy(policy);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);


    }

    // Login form validation
    private boolean validate(View view){
        boolean isValid;

        if(!TextUtils.isEmpty(username)){
            if(!TextUtils.isEmpty((password))){
                isValid = true;
            }else{
                Toast.makeText(LoginActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                isValid = false;
            }

        }else{

            Toast.makeText(LoginActivity.this, "Please enter your username", Toast.LENGTH_SHORT).show();
            isValid = false;

        }

        return isValid;

    }


    // Check if stored token is still valid -> direct to dashboard if it is/ login if expired
    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences user_pref = getSharedPreferences("user", MODE_PRIVATE);
        String token = user_pref.getString("token", "");

        if (user_pref.contains("token")) {
            validateToken(token);
            finish();
        }
    }


    private void validateToken(String token){

        String ApiKey = "http://10.0.2.2:5000/api/auth/verify-token";

        SharedPreferences user_pref = getSharedPreferences("user", MODE_PRIVATE);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, ApiKey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{
                    if(response.getBoolean("valid")){
                        String role = user_pref.getString("role", "");

                        if(role.equals("student")){
                            startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class));
                        }else if(role.equals("manager")){
                            startActivity(new Intent(LoginActivity.this, ManagerDashboardActivity.class));
                        }

                        finish();

                    }else{
                        user_pref.edit().remove("token").apply();
                        Toast.makeText(LoginActivity.this, "Token is invalid. Login Again", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                        finish();
                    }

                }catch (JSONException e){
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();

                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                if(error instanceof ServerError && response != null){
                    Log.e("TokenCheck", "ServerError Status Code: " + response.statusCode + ", Body: " + new String(response.data));
                    Toast.makeText(LoginActivity.this, "Token check failed", Toast.LENGTH_SHORT).show();
                }else if (error instanceof TimeoutError){
                    Toast.makeText(LoginActivity.this, "Network timeout. Please Try again", Toast.LENGTH_SHORT).show();
                }else if(error.networkResponse != null && error.networkResponse.statusCode == 403){
                    user_pref.edit().remove("token").apply();
                    startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Network Error. Please check your connection", Toast.LENGTH_SHORT).show();
                }

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);

    }
}