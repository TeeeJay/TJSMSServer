package com.tjproductions.tinyjavasmsserver.service;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;


public class TJSMSServer extends BroadcastReceiver {

    public static final String SMS ="pdus";

    @Override
    public void onReceive(Context context, Intent intent) {
        /**
         * Test SMS by connecting to emulator in a telnet session:
         * telnet
         * o localhost 5554
         * sms send <nr> <text>
         */

        if(this.smsServerIsEnabled(context) && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Bundle extras = intent.getExtras();
            SmsMessage[] messages;

            if ( extras != null ) {
                Object[] rawSMSes = (Object[]) extras.get( SMS );
                messages = new SmsMessage[rawSMSes.length];

                for ( int i = 0; i < rawSMSes.length; ++i )
                {
                    SmsMessage message = SmsMessage.createFromPdu((byte[])rawSMSes[i]);

                    String body = message.getMessageBody().toString();
                    String phoneNumber = message.getOriginatingAddress();
                    messages[i] = message;

                    String key = this.getKey(context);

                    if (    this.doesSMSContainKeyword(key, body)
                        &&  this.isNumberInAddressBook(context, phoneNumber)) {

                        this.sendReply(context, phoneNumber, this.getCorrespondingValueForKey(context, key));
                    }



                }

            }

        }

    }

    private String getCorrespondingValueForKey(Context context, String key) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String value = sharedPreferences.getString("wifipass","");
        return value;

    }

    private String getKey(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String key = sharedPreferences.getString("wifikey","");
        return key;
    }

    private boolean smsServerIsEnabled(Context context) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean automaticResponseEnabled = sharedPreferences.getBoolean("automatic_response_checkbox", false);
        return automaticResponseEnabled;

    }

    private boolean doesSMSContainKeyword(String key, String text) {

        if(text.trim().toLowerCase().equals(key.trim().toLowerCase())) {
            return true;
        }

        return false;
    }

    private boolean isNumberInAddressBook(Context context, String phoneNumber) {

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        ContentResolver contentResolver = context.getContentResolver();

        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {

                //contactLookup.moveToNext();

                return true;
            }

        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return false;
    }

    private void sendReply(Context context, String phoneNumber, String reply) {

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, reply, null, null);

    }


}
