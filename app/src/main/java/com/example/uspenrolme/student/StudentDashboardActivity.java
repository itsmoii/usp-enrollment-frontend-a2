package com.example.uspenrolme.student;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;


import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.uspenrolme.shared.LoginActivity;
import com.example.uspenrolme.R;
import com.example.uspenrolme.UtilityService.SharedPreference;
import com.example.uspenrolme.student.finance.FinanceMenu;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentDashboardActivity extends AppCompatActivity {


    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    SharedPreference sharedPreference;

    private TextView username, userID, userEmail;

    private CircleImageView userImage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_dashboard);


        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sharedPreference = new SharedPreference(this);

        View headerView = navView.getHeaderView(0);

        username = (TextView) headerView.findViewById(R.id.studentName);
        userID = (TextView) headerView.findViewById(R.id.user_id);
        userEmail = (TextView) headerView.findViewById(R.id.user_email);
        userImage = (CircleImageView) headerView.findViewById(R.id.avatar);

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                setDrawerClick(item.getItemId());
                item.setChecked(true);
                drawerLayout.closeDrawers();

                return true;
            }
        });

        initDrawer();
        getUserProfile();

    }

    private void getUserProfile(){
        String url = "http://10.0.2.2:5000/api/profile";
        final String token = sharedPreference.getValue_string("token");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    if(response.getBoolean("success")){

                        JSONObject userObj = response.getJSONObject("user");
                        JSONObject dataObj = userObj.getJSONObject("data");
                        JSONObject studentProfile = dataObj.getJSONObject("studentProfile");

                        username.setText(studentProfile.getString("first_name") + " " +  studentProfile.getString("last_name"));
                        userEmail.setText(studentProfile.getString("email"));
                        userID.setText(studentProfile.getString("student_id"));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(StudentDashboardActivity.this, "Error: " + error.toString(), Toast.LENGTH_SHORT).show();
                Log.d("ProfileDebug", "Retrieval error: " + error.toString());
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Bearer " + token);
                return headers;
            }
        };

        int socketTimeout = 10000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void initDrawer(){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        ft.replace(R.id.content, new HomeFragment());
        ft.commit();
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close){
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerToggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.white));
        drawerLayout.addDrawerListener(drawerToggle);
    }

    @Override
    public void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    private void setDrawerClick(int itemId){
        if (itemId == R.id.action_financeMenu) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content, new FinanceMenu()).commit();
        } else if (itemId == R.id.action_logout){
            sharedPreference.clear();
            startActivity(new Intent(StudentDashboardActivity.this, LoginActivity.class));
            finish();
        }else if (itemId == R.id.action_home){
            getSupportFragmentManager().beginTransaction().replace(R.id.content, new HomeFragment()).commit();
        }else if (itemId == R.id.action_grades){
            getSupportFragmentManager().beginTransaction().replace(R.id.content, new GradesFragment()).commit();
        }else if (itemId == R.id.action_registration){
            getSupportFragmentManager().beginTransaction().replace(R.id.content, new RegistrationFragment()).commit();
        }else if (itemId == R.id.action_programOutline){
            getSupportFragmentManager().beginTransaction().replace(R.id.content, new ProgramOutlineFragment()).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}