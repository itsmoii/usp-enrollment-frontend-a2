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

import androidx.appcompat.app.AppCompatActivity;

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
import com.example.uspenrolme.manager.ManagerDashboardAcitivity;
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
    private ProgressBar progressBar;
    private String username, password;

    private UtilService utilService;
    private SharedPreference sharedPref;
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginBtn = findViewById(R.id.loginBtn);
        username_ET = findViewById(R.id.username_et);
        password_ET = findViewById(R.id.password_et);
        progressBar = findViewById(R.id.progress_bar);
        utilService = new UtilService();
        sharedPref = new SharedPreference(this);
        requestQueue = Volley.newRequestQueue(this);

        // Configure password toggle
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

        loginBtn.setOnClickListener(view -> {
            utilService.hideKeyboard(view, LoginActivity.this);
            username = username_ET.getText().toString();
            password = password_ET.getText().toString();

            if (validate()) {
                loginUser();
            }
        });
    }

    // Validating user credentials in backend
    private void loginUser() {
        progressBar.setVisibility(View.VISIBLE);

        HashMap<String, String> params = new HashMap<>();
        params.put("userId", username);
        params.put("password", password);

        String apiKey = "http://10.0.2.2:5000/api/auth/login";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiKey, new JSONObject(params),
                response -> {
                    Log.d("Login", "Response: " + response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            String token = response.getString("token");
                            String role = response.getString("role");

                            sharedPref.setValue_string("token", token);
                            sharedPref.setValue_string("role", role);
                            sharedPref.setValue_string("userID", username);

                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                            if (role.equals("student")) {
                                startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class));
                            } else if (role.equals("manager")) {
                                startActivity(new Intent(LoginActivity.this, ManagerDashboardAcitivity.class));
                            }
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                }, error -> {
                    handleError(error);
                    progressBar.setVisibility(View.GONE);
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        int socketTime = 3000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTime, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(policy);

        requestQueue.add(jsonObjectRequest);
    }

    // Login form validation
    private boolean validate() {
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(LoginActivity.this, "Please enter your username", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Check if stored token is still valid -> direct to dashboard if it is/ login if expired
    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences user_pref = getSharedPreferences("user", MODE_PRIVATE);
        String token = user_pref.getString("token", "");

        if (user_pref.contains("token")) {
            validateToken(token);
        }
    }

    private void validateToken(String token) {
        String apiKey = "http://10.0.2.2:5000/api/auth/verify-token";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiKey, null,
                response -> {
                    try {
                        if (response.getBoolean("valid")) {
                            String role = sharedPref.getValue_string("role");
                            if (role.equals("student")) {
                                startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class));
                            } else if (role.equals("manager")) {
                                startActivity(new Intent(LoginActivity.this, ManagerDashboardAcitivity.class));
                            }
                            finish();
                        } else {
                            sharedPref.clear();
                            Toast.makeText(LoginActivity.this, "Token is invalid. Please log in again.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    handleError(error);
                }) {
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

        requestQueue.add(jsonObjectRequest);
    }

    private void handleError(VolleyError error) {
        Log.e("Login", "Error: " + error.toString());
        NetworkResponse response = error.networkResponse;
        if (error instanceof ServerError && response != null) {
            try {
                String res = new String(response.data, HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                JSONObject obj = new JSONObject(res);
                Toast.makeText(LoginActivity.this, obj.getString("msg"), Toast.LENGTH_SHORT).show();
            } catch (JSONException | UnsupportedEncodingException je) {
                je.printStackTrace();
            }
        } else if (response != null && response.statusCode == 401) {
            Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
        } else if (error instanceof TimeoutError) {
            Toast.makeText(LoginActivity.this, "Network timeout. Please try again.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(LoginActivity.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
        }
    }
}