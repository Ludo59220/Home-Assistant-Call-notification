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
import android.telecom.Call;
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

    CallBroadcast callBroadcast;
    SmsBroadcast smsBroadcast;

    preferences pref;
    public String HA_URL_CALL = "?";
    public String HA_URL_SMS = "?";
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
        HA_URL_CALL=pref.getUrl()+ "/api/events/call_event";
        HA_URL_SMS=pref.getUrl()+ "/api/events/sms_event";
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
        if (callBroadcast==null) {
            callBroadcast = new CallBroadcast();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.PHONE_STATE");
            intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
            intentFilter.addAction("android.intent.action.NEW_INCOMING_CALL");
            intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            registerReceiver(callBroadcast, intentFilter);
        }

        if (smsBroadcast==null) {
            smsBroadcast = new SmsBroadcast();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
            intentFilter.addAction("android.provider.Telephony.SMS_BROADCAST");
            intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
            registerReceiver(smsBroadcast, intentFilter);
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
        if (callBroadcast!=null) unregisterReceiver(callBroadcast);
        super.onDestroy();
    }

    private void sendCallEvent(String t,String num){

        if (pref.getWifi()) {
            if(new Utils().isWifiConnected(this)){
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);;
                WifiInfo info = wifiManager.getConnectionInfo ();
                final String ssid  = info.getSSID();
                if (pref.getWifi() && !pref.getWifiName().equals(ssid)) return;
            }
        }

        Map<String, String> params = new HashMap();
        params.put("type",t);
        params.put("number", num);
        params.put("device",pref.getDevice());

        send(this,HA_URL_CALL,params);

    }

    private void sendSmsEvent(String num,String msg){

        if (pref.getWifi()) {
            if(new Utils().isWifiConnected(this)){
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);;
                WifiInfo info = wifiManager.getConnectionInfo ();
                final String ssid  = info.getSSID();
                if (pref.getWifi() && !pref.getWifiName().equals(ssid)) return;
            }
        }

        Map<String, String> params = new HashMap();
        params.put("number",num);
        params.put("msg", msg);
        params.put("device",pref.getDevice());
        send(this,HA_URL_SMS,params);
    }

    private void send(Context context,String url,Map<String, String> params){
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        JSONObject param=new JSONObject(params);


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,url,param, new Response.Listener<JSONObject>() {


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
    class CallBroadcast extends PhoneCallReceiver {

        @Override
        protected void onIncomingCallStarted(final Context ctx, final String number, Date start) {
            Log.d(TAG, "incoming " + number);

            String name=new Utils().getNameByNumber(getApplicationContext(),number);
            sendCallEvent("onIncomingCallStarted",(name!=null)? name : number);

        }

        @Override
        protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
            Log.d(TAG, "Outgoing "+ number);

            String name=new Utils().getNameByNumber(getApplicationContext(),number);
            sendCallEvent("onOutgoingCallStarted",(name!=null)? name : number);

        }

        @Override
        protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d(TAG, "End incoming " + number);

            String name=new Utils().getNameByNumber(getApplicationContext(),number);
            sendCallEvent("onIncomingCallEnded",(name!=null)? name : number);
        }

        @Override
        protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
            Log.d(TAG, "End outgoing "+ number);

            String name=new Utils().getNameByNumber(getApplicationContext(),number);
            sendCallEvent("onOutgoingCallEnded",(name!=null)? name : number);
        }

        @Override
        protected void onMissedCall(Context ctx, String number, Date start) {

            Log.d(TAG, "Missed "+number);

            String name=new Utils().getNameByNumber(getApplicationContext(),number);
            sendCallEvent("onMissedCall",(name!=null)? name : number);
        }
    }

    class SmsBroadcast extends SMSReceiver{
        @Override
        protected void onSmsReceived(String from,String msg){
            String name=new Utils().getNameByNumber(getApplicationContext(),from);
            sendSmsEvent(name,msg);
        }
    }
}
