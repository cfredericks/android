package com.cfred1985.gcmdemo;

import com.cfred1985.util.gcm.GcmActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.apache.http.HttpResponse;

import java.io.IOException;

public class DemoGcmActivity extends GcmActivity
{
    static final String TAG = "DemoGcmActivity";

    TextView mDisplay;

    public DemoGcmActivity()
    {
        super("153145237843", "http://gcmdemocorey.appspot.com/registration_id_post");
    }

    @Override
    public void onPreInit(Bundle savedInstanceState)
    {
        setContentView(R.layout.main);
        mDisplay = (TextView)findViewById(R.id.display);
    }

    @Override
    public void onSendRegistrationIdToServerException(Exception e, String serverRegIdPostUrl, String registrationId, String clientId)
    {
        Log.e(TAG, "Exception sending registration id (" + serverRegIdPostUrl + "," +
                registrationId + "," + clientId + ") to server: " + e.toString());
    }

    @Override
    public void onSendRegistrationIdToServerResult(HttpResponse response, String serverRegIdPostUrl, String registrationId, String clientId)
    {
        Log.i(TAG, "Response from sending registration id (" + serverRegIdPostUrl + "," +
                registrationId + "," + clientId + ") to server: " + response.toString());
    }


    @Override
    public void onRegistration(String message, String registrationId)
    {
        Log.i(TAG, message);
        mDisplay.append(message + "\n");
    }

    @Override
    public void onRegistrationIdRecovery(String registrationId)
    {
        mDisplay.append("Reusing previous registration id: " + registrationId + "\n");
    }

    // Send an upstream message.
    public void onClick(final View view)
    {
        Log.i(TAG, "Clicked! " + view.toString());
        if (view == findViewById(R.id.send))
        {
            new AsyncTask<Void, Void, String>()
            {
                @Override
                protected String doInBackground(Void... params)
                {
                    String msg;
                    try
                    {
                        Bundle data = new Bundle();
                        data.putString("my_message", "Hello World");
                        data.putString("my_action", "com.cfred1985.gcmdemo.ECHO_NOW");
                        GcmSend(data);
                        msg = "Sent message";
                    }
                    catch (IOException ex)
                    {
                        msg = "Error :" + ex.getMessage();
                    }
                    return msg;
                }

                @Override
                protected void onPostExecute(String msg)
                {
                    mDisplay.append(msg + "\n");
                }
            }.execute(null, null, null);
        }
        else if (view == findViewById(R.id.clear))
        {
            mDisplay.setText("");
        }
        else if (view == findViewById(R.id.reregister))
        {
            registerInBackground();
        }
    }
}
