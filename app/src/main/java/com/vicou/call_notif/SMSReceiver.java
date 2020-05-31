package com.vicou.call_notif;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] data = (Object[]) bundle.get("pdus");
            SmsMessage[] msg = new SmsMessage[data.length];

            for (int i = 0; i < data.length; i++)
            {
                // Convertir les PDUs en messages
                msg[i] = SmsMessage.createFromPdu((byte[]) data[i]);
            }

            String number="?";
            String receivesms="?";

            // Enfin, traiter les messages !
            for (SmsMessage message : msg) {
                number = message.getOriginatingAddress();
                receivesms = message.getMessageBody();

            }
            onSmsReceived(number,receivesms);

        }
    }

    protected void onSmsReceived(String from,String msg){}
}
