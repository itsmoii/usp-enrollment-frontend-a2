package com.example.uspenrolme.manager;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ManagerDashboardAcitivity extends AppCompatActivity {

    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    SharedPreference sharedPreference;
    private TextView managerName, managerID, managerEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager_dashboard_acitivity);


        drawerLayout = findViewById(R.id.manager_drawer);
        NavigationView navView = findViewById(R.id.managerNavView);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        sharedPreference = new SharedPreference(this);

        View headerView = navView.getHeaderView(0);

        managerName = (TextView) headerView.findViewById(R.id.managerName);
        managerID = (TextView) headerView.findViewById(R.id.manager_id);
        managerEmail = (TextView) headerView.findViewById(R.id.manager_email);

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
        String url = "http://10.0.2.2:5000/api/manager-profile";
        final String token = sharedPreference.getValue_string("token");

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    if(response.getBoolean("success")){

                        JSONObject userObj = response.getJSONObject("user");
                        JSONObject dataObj = userObj.getJSONObject("data");
                        JSONObject managerProfile = dataObj.getJSONObject("managerProfile");

                        managerName.setText(managerProfile.getString("first_name") + " " +  managerProfile.getString("last_name"));
                        managerEmail.setText(managerProfile.getString("email"));
                        managerID.setText(managerProfile.getString("manager_id"));

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Toast.makeText(ManagerDashboardAcitivity.this, "Error: " + error.toString(), Toast.LENGTH_SHORT).show();
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
        ft.replace(R.id.manager_content, new ManagerHomeFragment());
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
        if (itemId == R.id.action_manager_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.manager_content, new ManagerHomeFragment()).commit();
        }else if (itemId == R.id.action_manager_logout){
            sharedPreference.clear();
            startActivity(new Intent(ManagerDashboardAcitivity.this, LoginActivity.class));
            finish();
        }else if (itemId == R.id.action_reg_panel){
            getSupportFragmentManager().beginTransaction().replace(R.id.manager_content, new RegistrationPanelFragment()).commit();
        }else if (itemId == R.id.action_holds_panel){
            getSupportFragmentManager().beginTransaction().replace(R.id.manager_content, new HoldsPanelFragment()).commit();
        }else if (itemId == R.id.action_grades_panel){
            getSupportFragmentManager().beginTransaction().replace(R.id.manager_content, new GradesPanelFragment()).commit();
        }else if (itemId == R.id.action_transcript_panel){
            getSupportFragmentManager().beginTransaction().replace(R.id.manager_content, new TranscriptPanelFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}