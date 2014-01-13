package com.cfred1985.emailsms;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.cfred1985.util.messaging.ContactHelper;

public class CallReceiver extends BroadcastReceiver
{
    private static final String TAG = "CallReceiver";
    private static final ScheduledExecutorService worker = Executors
                    .newSingleThreadScheduledExecutor();

    private static final ContactHelper contactHelper = new ContactHelper();
    private static final Object updateLock = new Object();

    private static final String CALL_LOG_URI = "content://call_log/calls";

    private AppSettings settings;
    private long lastTimestampSeen;

    private String fromEmailAddress = "emailtextmessage.auto.forward";
    private String emailSubject = "Missed Phone Call(s)";

    public CallReceiver()
    {
        settings = new AppSettings();
        lastTimestampSeen = System.currentTimeMillis();
    }

    public CallReceiver(String emailSubject, String fromEmailAddress)
    {
        this.emailSubject = emailSubject;
        this.fromEmailAddress = fromEmailAddress;
        settings = new AppSettings();
        lastTimestampSeen = System.currentTimeMillis();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        synchronized (updateLock)
        {
            // Settings update
            if (extras.containsKey(AppSettings.BundleKey))
            {
                AppSettings smsExtra = extras.getParcelable(AppSettings.BundleKey);
                settings = smsExtra;
            }
            // Call state changed
            else if (settings.IsForwarding && settings.ForwardCalls)
            {
                // Check after a delay for missed calls
                final Context contextCopy = context;
                Runnable task = new Runnable()
                {
                    public void run()
                    {
                        CheckCallLogForMissedCalls(contextCopy);
                    }
                };
                worker.schedule(task, 10, TimeUnit.SECONDS);
            }
        }
    }

    private void CheckCallLogForMissedCalls(Context context)
    {
        final String[] projection = null;
        final String selection = null;
        final String[] selectionArgs = null;
        final String sortOrder = android.provider.CallLog.Calls.DATE + " DESC";

        final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());

        Cursor cursor = null;
        boolean emailSuccess = false;
        try
        {
            cursor = context.getContentResolver().query(Uri.parse(CALL_LOG_URI),
                            projection, selection, selectionArgs, sortOrder);

            List<String> missedCalls = new ArrayList<String>();
            List<InputStream> attachments = new ArrayList<InputStream>();
            long largestTime = 0;
            while (cursor.moveToNext())
            {
                String callNumber = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER));
                String callDate = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.DATE));
                String callDuration = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.DURATION));
                String callType = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE));
                String isCallNew = cursor.getString(cursor.getColumnIndex(android.provider.CallLog.Calls.NEW));

                long timestamp = Long.parseLong(callDate);
                Date date = new Date(timestamp);
                String dateFormatted = dateFormatter.format(date);

                Log.d(TAG, "This timestamp: " + timestamp + ", last: " + lastTimestampSeen);

                if (Integer.parseInt(callType) == android.provider.CallLog.Calls.MISSED_TYPE
                                && Integer.parseInt(isCallNew) > 0 && timestamp > lastTimestampSeen)
                {
                    Log.d(TAG, "Missed call found: " + callNumber);
                    ContactHelper.Contact contact = contactHelper.GetContact(context, callNumber);

                    String from = callNumber;
                    if (contact.name != null)
                        from = contact.name + " (" + from + ")";

                    missedCalls.add("<font size=\"4\">Caller: <b>" + from + "</b></font>"
                                    + "<br><font size=\"4\">Time: <b>" + dateFormatted
                                    + "</b></font>" + "<br><font size=\"4\">Duration: <b>"
                                    + callDuration + " seconds</b></font>\n");

                    if (timestamp > largestTime)
                    {
                        largestTime = timestamp;
                    }
                }
            }

            // Send email if we have new missed calls
            if (settings.IsLoggedIn() && largestTime > 0)
            {
                // Only send new missed calls
                StringBuilder sb = new StringBuilder();
                for (String call : missedCalls)
                {
                    sb.append(call);
                }
                sb.append("<br><br>");

                emailSuccess = settings.SendEmail(fromEmailAddress, emailSubject, sb.toString(),
                                (InputStream[])attachments.toArray());
                lastTimestampSeen = largestTime;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (!emailSuccess)
            {
                Toast.makeText(context, "Error sending email on new missed call", Toast.LENGTH_SHORT);
            }

            cursor.close();
        }
    }
}
