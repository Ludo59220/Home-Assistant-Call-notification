package com.vicou.call_notif;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.android.volley.Request.Method.POST;
import static com.vicou.call_notif.BootReceiver.MY_PERMISSIONS_REQUEST_PROCESS_OUTGOING_CALLS;
import static com.vicou.call_notif.BootReceiver.MY_PERMISSIONS_REQUEST_READ_PHONE_STATE;

public class MainActivity extends AppCompatActivity {
    private String TAG="HA call notif";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissionAndRequest(Manifest.permission.READ_PHONE_STATE,0);
        setContentView(R.layout.activity_main);

        final preferences pref=new preferences(this);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);;
        WifiInfo info = wifiManager.getConnectionInfo ();
        final String ssid  = info.getSSID();

        TextView wifi=findViewById(R.id.txtWifi);
        final EditText ha_url=findViewById(R.id.ha_url);
        ha_url.setText(pref.getUrl());

        final EditText ha_token=findViewById(R.id.ha_token);
        ha_token.setText(pref.getToken());

        final EditText device=findViewById(R.id.device);
        device.setText(pref.getDevice());

        final Switch wifiSwitch=findViewById(R.id.switch1);
        wifiSwitch.setChecked(pref.getWifi());


        wifi.setText(ssid);

        Button btnTest=findViewById(R.id.btnTest);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData(ha_url.getText().toString() + "/api/events/call_notif",ha_token.getText().toString(),"onIncomingCallStarted","***test***",device.getText().toString(),getApplicationContext());
            }
        });

        final Button btnStart=findViewById(R.id.btnStart);
        btnStart.setEnabled(false);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pref.setUrl(ha_url.getText().toString());
                pref.setToken(ha_token.getText().toString());
                pref.setWifi(wifiSwitch.isChecked());
                pref.setWifiName(ssid);
                pref.setDevice(device.getText().toString());
                pref.setFirstStart(true);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                              startForegroundService(new Intent(getApplicationContext(),PhoneCallService.class));
                }else startService(new Intent(getApplicationContext(),PhoneCallService.class));
                btnStart.setEnabled(false);
            }
        });

    }

    public void sendData(String url, final String token, String t, String num, String device, Context ctx){
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        Map<String, String> params = new HashMap();
        params.put("type",t);
        params.put("number", num);
        params.put("device",device);

        JSONObject param=new JSONObject(params);


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,url,param, new Response.Listener<JSONObject>() {


            @Override
            public void onResponse(JSONObject response) {

                Toast.makeText(getApplicationContext(),
                        "response-"+response, Toast.LENGTH_LONG).show();
                Log.d(TAG,""+response);

                try {
                    if (response.getString("message").equals("Event call_notif fired.")) ((Button)findViewById(R.id.btnStart)).setEnabled(true);

                }catch (JSONException e){

                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                ((Button)findViewById(R.id.btnStart)).setEnabled(false);

                Toast.makeText(getApplicationContext(),
                        "error"+error.toString(), Toast.LENGTH_LONG).show();
                Log.d(TAG,"Volley error: "+error);
                VolleyLog.d(TAG, "Volley Error message: " + error.getMessage());

                NetworkResponse networkResponse = error.networkResponse;
                if (networkResponse != null && networkResponse.data != null) {
                    Log.e(TAG, String.valueOf(networkResponse.data));
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer "+token);
                return headers;
            }

        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5*DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 0));
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));
        requestQueue.add(jsonObjectRequest);
    }

    public void checkPermissionAndRequest(String perm,int code){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                perm) //Manifest.permission.READ_PHONE_STATE
                != PackageManager.PERMISSION_GRANTED) {
            // We do not have this permission. Let's ask the user
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{perm},code
                    );
        }
    }

    public void showError(){
        Toast.makeText(getApplicationContext(),"L'application ne peux pas fonctionner sans cette autorisation",Toast.LENGTH_LONG).show();
        finish();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermissionAndRequest(Manifest.permission.READ_CALL_LOG,1);
                } else {

                    showError();
                }
                break;
            }
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkPermissionAndRequest(Manifest.permission.READ_CONTACTS,2);
                } else {
                    showError();
                }
                break;
            }
            case 2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkPermissionAndRequest(Manifest.permission.ACCESS_FINE_LOCATION,2);
                } else {
                    showError();
                }
                break;
            }
            case 3: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    checkPermissionAndRequest(Manifest.permission.INTERNET,3);
                } else {
                    showError();
                }
                break;
            }
        }
    }
}
