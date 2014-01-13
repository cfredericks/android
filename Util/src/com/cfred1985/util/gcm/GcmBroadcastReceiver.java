package com.cfred1985.util.gcm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver
{
    Class intentServiceType;

    public GcmBroadcastReceiver(Class intentServiceType)
    {
        this.intentServiceType = intentServiceType;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        ComponentName comp = new ComponentName(context.getPackageName(), intentServiceType.getName());
        startWakefulService(context, intent.setComponent(comp));
        setResultCode(Activity.RESULT_OK);
    }
}
