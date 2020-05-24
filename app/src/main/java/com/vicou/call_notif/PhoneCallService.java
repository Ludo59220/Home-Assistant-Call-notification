package com.vicou.call_notif;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PhoneCallService extends Service {

    CallReceiver receiver;
    preferences pref;
    public String HA_URL = "?";
    public String HA_TOKEN="?";

    public final String TAG="call_notif";

    public PhoneCallService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {

        pref=new preferences(this);
        HA_URL=pref.getUrl()+ "/api/events/call_notif";
        HA_TOKEN=pref.getToken();

        super.onCreate();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Démarage du service HA call notif.", Toast.LENGTH_LONG).show();

        if (!pref.getFirstStart())
        {
            Toast.makeText(this, "La configuration est incorrecte.\r\nVeuillez vérifier vos paramétres.", Toast.LENGTH_LONG).show();
            stopSelf();
        }
        super.onStartCommand(intent, flags, startId);
        if (receiver==null) {
            receiver = new CallReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PHONE_STATE");
            intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
            intentFilter.addAction("android.intent.action.NEW_INCOMING_CALL");
            registerReceiver(receiver, intentFilter);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String NOTIFICATION_CHANNEL_ID = "com.vicou.call_notif";
            String channelName = "HA calling notification";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("HA call notif . Le service est lancé.")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        }
        else
            startForeground(1, new Notification());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (receiver!=null) unregisterReceiver(receiver);
        super.onDestroy();
    }

    public void sendData(String t,String num){

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (!wifiInfo.isConnected() && pref.getWifi()) return;


        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);;
        WifiInfo info = wifiManager.getConnectionInfo ();
        final String ssid  = info.getSSID();

        if (pref.getWifi() && !pref.getWifiName().equals(ssid)) return;

        RequestQueue requestQueue = Volley.newRequestQueue(this);

        Map<String, String> params = new HashMap();
        params.put("type",t);
        params.put("number", num);
        params.put("device",pref.getDevice());

        JSONObject param=new JSONObject(params);


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,HA_URL,param, new Response.Listener<JSONObject>() {


            @Override
            public void onResponse(JSONObject response) {

                Log.d(TAG,"Response: "+response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

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
                headers.put("Authorization", "Bearer "+HA_TOKEN);
                return headers;
            }

        };

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5*DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, 0, 0));
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(0, 0, 0));
        requestQueue.add(jsonObjectRequest);
    }

    public String getNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }


    class CallReceiver extends PhoneCallReceiver {
        public JSONObject rep = null;


        @Override
        protected void onIncomingCallStarted(final Context ctx, final String number, Date start) {
            Log.d(TAG, "incoming " + number);

            String name=getNameByNumber(number);
            sendData("onIncomingCallStarted",(name!=null)? name : number);

        }

        @Override
        protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
            Log.d(TAG, "Outgoing "+ number);

            String name=getNameByNumber(number);
            sendData("onOutgoingCallStarted",(name!=null)? name : number);

        }

        @Override
        protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d(TAG, "End incoming " + number);

            String name=getNameByNumber(number);
            sendData("onIncomingCallEnded",(name!=null)? name : number);
        }

        @Override
        protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d(TAG, "End outgoing "+ number);

            String name=getNameByNumber(number);
            sendData("onOutgoingCallEnded",(name!=null)? name : number);
        }

        @Override
        protected void onMissedCall(Context ctx, String number, Date start) {

            Log.d(TAG, "Missed "+number);

            String name=getNameByNumber(number);
            sendData("onMissedCall",(name!=null)? name : number);
        }
    }
}
