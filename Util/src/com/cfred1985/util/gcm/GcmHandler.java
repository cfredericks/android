package com.cfred1985.util.gcm;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class GcmHandler
{
    static final String TAG = "GcmHandler";

    String projectNumber;
    String serverRegIdPostUrl;
    String registrationId;
    String clientId;

    AtomicInteger msgId = new AtomicInteger();
    GoogleCloudMessaging gcm;
    Context context;

    // Methods to override in child
    public boolean checkPlayServices() { return false; }
    public void onGooglePlayInitSuccess() {}
    public void onGooglePlayInitFailure(String message) {}
    public void onSendRegistrationIdToServerException(Exception e, String serverRegIdPostUrl, String registrationId, String clientId) {}
    public void onSendRegistrationIdToServerResult(HttpResponse response, String serverRegIdPostUrl, String registrationId, String clientId) {}
    public void onRegistration(String message, String registrationId) {}
    public void storeRegistrationId(String regId) {}
    public String getRegistrationId() { return ""; }

    public GcmHandler(String projectNumber, String serverRegIdPostUrl, Context context)
    {
        this.projectNumber = projectNumber;
        this.serverRegIdPostUrl = serverRegIdPostUrl;
        this.context = context;
    }

    public void Init(Activity activity)
    {
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices())
        {
            onGooglePlayInitSuccess();

            gcm = GoogleCloudMessaging.getInstance(activity);
            registrationId = getRegistrationId();

            if (registrationId.isEmpty())
            {
                registerInBackground();
            }
        }
        else
        {
            onGooglePlayInitFailure("No valid Google Play Services APK found.");
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    protected void GcmSend(Bundle data) throws IOException
    {
        String id = Integer.toString(msgId.incrementAndGet());
        gcm.send(projectNumber + "@gcm.googleapis.com", id, data);
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    protected void registerInBackground()
    {
        new AsyncTask<Void, Void, String>()
        {
            @Override
            protected String doInBackground(Void... params)
            {
                String msg;
                try
                {
                    if (gcm == null)
                    {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    registrationId = gcm.register(projectNumber);
                    msg = "Device registered, registration ID=" + registrationId;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    storeRegistrationId(registrationId);
                }
                catch (IOException ex)
                {
                    msg = "Error :" + ex.toString();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg)
            {
                onRegistration(msg, registrationId);
            }
        }.execute(null, null, null);
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send messages to your app.
     */
    public void sendRegistrationIdToBackend()
    {
        HttpClient httpClient = new DefaultHttpClient();

        try
        {
            // Create message
            JSONObject json = new JSONObject();
            json.put("registration_id", registrationId);
            json.put("client_id", clientId);
            StringEntity params = new StringEntity(json.toString());

            // Send
            HttpPost request = new HttpPost(serverRegIdPostUrl);
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            request.setEntity(params);
            HttpResponse response = httpClient.execute(request);

            onSendRegistrationIdToServerResult(response, serverRegIdPostUrl, registrationId, clientId);
        }
        catch (Exception e)
        {
            onSendRegistrationIdToServerException(e, serverRegIdPostUrl, registrationId, clientId);
        }
        finally
        {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
