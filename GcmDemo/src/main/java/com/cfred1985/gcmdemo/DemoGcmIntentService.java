package com.cfred1985.gcmdemo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cfred1985.util.gcm.GcmIntentService;

public class DemoGcmIntentService extends GcmIntentService
{
    static final String TAG = "DemoGcmIntentService";

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;

    public DemoGcmIntentService()
    {
        super("DemoGcmIntentService");
    }

    @Override
    public void onSendErrorMessage(Intent intent, Bundle extras)
    {
        sendNotification("Send error: " + extras.toString());
    }

    @Override
    public void onDeletedMessage(Intent intent, Bundle extras)
    {
        sendNotification("Deleted messages on server: " + extras.toString());
    }

    @Override
    public void onMessage(Intent intent, Bundle extras)
    {
        Log.i(TAG, "Got state change request: " + extras.get("state"));
        for (int i = 0; i < 5; i++)
        {
            Log.i(TAG, "Working... " + (i + 1) + "/5 @ " + SystemClock.elapsedRealtime());
            try
            {
                Thread.sleep(5000);
            }
            catch (InterruptedException e)
            {
            }
        }
        Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
        // Post notification of received message.
        sendNotification("Received: " + extras.toString());
        Log.i(TAG, "Received: " + extras.toString());
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg)
    {
        mNotificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, DemoGcmActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_gcm)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                          .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
