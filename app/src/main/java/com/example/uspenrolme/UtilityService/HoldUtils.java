package com.example.uspenrolme.UtilityService;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class HoldUtils {
    public static void checkHold(Context context, String token, String serviceName, Consumer<Boolean> callback){
        String apiKey = "http://10.0.2.2:5000/api/microservice/check-hold";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, apiKey, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try{

                    JSONObject holdObject = response.optJSONObject("hold");
                    JSONObject rulesObject = response.optJSONObject("rules");

                    if(holdObject != null && holdObject.getBoolean("hold")){
                        boolean isServiceBlocked = rulesObject != null && rulesObject.optBoolean(serviceName, false);
                        callback.accept(isServiceBlocked);

                    }else{
                        callback.accept(false);
                    }

                }catch(JSONException e){
                    e.printStackTrace();
                    callback.accept(false);
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                callback.accept(false);

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

        request.setRetryPolicy(policy);

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        requestQueue.add(request);


    }

}
