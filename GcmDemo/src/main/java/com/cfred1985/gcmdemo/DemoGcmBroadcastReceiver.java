package com.cfred1985.gcmdemo;

import com.cfred1985.util.gcm.GcmBroadcastReceiver;

public class DemoGcmBroadcastReceiver extends GcmBroadcastReceiver
{
    public DemoGcmBroadcastReceiver()
    {
        super(DemoGcmIntentService.class);
    }
}
