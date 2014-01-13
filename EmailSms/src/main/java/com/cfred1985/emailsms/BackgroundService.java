package com.cfred1985.emailsms;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class BackgroundService extends Service
{
    private static final String TAG = "BackgroundService";
    private static final Object settingsLock = new Object();

    private static final IntentFilter smsReceivedFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
    private static final IntentFilter phoneStateFilter = new IntentFilter("android.intent.action.PHONE_STATE");
    private static final IntentFilter appSettingsFilter = new IntentFilter(AppSettings.IntentFilter);

    private AppSettings settings;
    private SmsReceiver smsReceiver;
    private CallReceiver callReceiver;

    @Override
    public IBinder onBind(Intent arg0)
    {
        Log.d(TAG, "onBind()");
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.d(TAG, "onCreate()");

        // Load settings from saved preferences
        settings = new AppSettings(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand()");

        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(AppSettings.BundleKey))
        {
            AppSettings smsExtra = extras.getParcelable(AppSettings.BundleKey);
            RegisterCallReceiver(!smsExtra.ForwardCalls || !smsExtra.IsForwarding);
            RegisterSmsReceiver(!smsExtra.ForwardSms || !smsExtra.IsForwarding);

            synchronized (settingsLock)
            {
                settings = smsExtra;

                // Save settings
                Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                settings.SaveSettings(editor);
                editor.commit();

                if (settings.IsForwarding)
                {
                    String subject = "Forwarding to " + settings.ForwardEmailAddress;

                    Intent notificationIntent = new Intent(this, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

                    Notification notification = new Notification(R.drawable.ic_launcher, subject,
                                                                 System.currentTimeMillis());
                    notification.setLatestEventInfo(this, getText(R.string.app_name), subject, pendingIntent);
                    notification.flags |= Notification.FLAG_NO_CLEAR;

                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    startForeground(1337, notification);
                }
                else
                {
                    stopForeground(true);
                }
            }
        }

        synchronized (settingsLock)
        {
            settings.Publish(this);

            if (!settings.IsForwarding)
            {
                stopSelf();
            }
        }

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");

        RegisterCallReceiver(true);
        RegisterSmsReceiver(true);
    }

    private void RegisterSmsReceiver(boolean stop)
    {
        Log.d(TAG, "RegisterSmsReceiver: " + stop);

        if (stop && smsReceiver != null)
        {
            unregisterReceiver(smsReceiver);
            smsReceiver = null;
        }
        else if (!stop && smsReceiver == null)
        {
            smsReceiver = new SmsReceiver();
            registerReceiver(smsReceiver, appSettingsFilter);
            registerReceiver(smsReceiver, smsReceivedFilter);
        }
    }

    private void RegisterCallReceiver(boolean stop)
    {
        Log.d(TAG, "RegisterCallReceiver: " + stop);

        if (stop && callReceiver != null)
        {
            unregisterReceiver(callReceiver);
            callReceiver = null;
        }
        else if (!stop && callReceiver == null)
        {
            callReceiver = new CallReceiver();
            registerReceiver(callReceiver, appSettingsFilter);
            registerReceiver(callReceiver, phoneStateFilter);
        }
    }
}
