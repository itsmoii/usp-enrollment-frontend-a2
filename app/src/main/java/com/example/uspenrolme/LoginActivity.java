package com.example.uspenrolme;

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
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.UtilityService.UtilService;
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

            if (validate(view)) {
                loginUser(view);
            }
        });
    }

    private void loginUser(View view) {
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

                            sharedPref.setValue_string("token", token);
                            sharedPref.setValue_string("userID", username);

                            Log.d("sharedprefdebug", "Stored UserID: " + sharedPref.getValue_string("userID"));

                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class));
                            finish();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        utilService.showSnackBar(view, "Error parsing login response.");
                    }
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
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
                    }
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

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private boolean validate(View view) {
        if (TextUtils.isEmpty(username)) {
            utilService.showSnackBar(view, "Please enter username");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            utilService.showSnackBar(view, "Please enter password");
            return false;
        }
        return true;
    }


}