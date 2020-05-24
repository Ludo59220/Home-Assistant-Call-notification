package com.vicou.call_notif;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.os.Build;
import android.util.Log;
import android.widget.Toast;


public class BootReceiver extends BroadcastReceiver {

    public static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0;
    public static final int MY_PERMISSIONS_REQUEST_PROCESS_OUTGOING_CALLS = 1;

    Context ctx;
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i("BOOT", "Boot completed");

        ctx=context;

        Intent service=new Intent(context,PhoneCallService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }

    }

}
