package com.cfred1985.util.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.cfred1985.util.R;

public class LogActivity extends Activity
{
    private static final String LogCatCommand = "logcat ActivityManager:I *:S";
    private static final String ClearLogCatCommand = "logcat -c";

    private ArrayList<String> logList = new ArrayList<String>();
    private ArrayAdapter<String> messageListAdapter;
    private String logCatFilter = "";

    public LogActivity()
    {
    }

    public LogActivity(String logCatFilter)
    {
        this.logCatFilter = logCatFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        ListView lstLog = (ListView)findViewById(R.id.lstLog);
        messageListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, logList);
        lstLog.setAdapter(messageListAdapter);

        Thread mThread = new MonitorLogThread();
        mThread.start();
    }

    private class MonitorLogThread extends Thread
    {
        BufferedReader br;

        public MonitorLogThread()
        {}

        @Override
        public void run()
        {
            try
            {
                Process process;
                Runtime.getRuntime().exec(ClearLogCatCommand);
                process = Runtime.getRuntime().exec(LogCatCommand);
                br = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                // Check if it matches the pattern
                while (((line = br.readLine()) != null) && !this.isInterrupted())
                {
                    // Filter for your app-line
                    if (logCatFilter.length() == 0 || line.contains(logCatFilter))
                    {
                        logList.add(line);

                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                messageListAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
