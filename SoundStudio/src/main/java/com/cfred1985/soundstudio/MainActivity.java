package com.cfred1985.soundstudio;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class MainActivity extends TabActivity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup tabs

        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Reusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        intent = new Intent().setClass(this, AudioActivity.class);
        spec = tabHost.newTabSpec("audio").setIndicator("Audio").setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, UsbActivity.class);
        spec = tabHost.newTabSpec("usb").setIndicator("USB").setContent(intent);
        tabHost.addTab(spec);

        intent = new Intent().setClass(this, MagStripeActivity.class);
        spec = tabHost.newTabSpec("magstripe").setIndicator("MagStripe").setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }
}