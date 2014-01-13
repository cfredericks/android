package com.cfred1985.util.gcm;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class GcmActivity extends Activity
{
    static final String TAG = "GcmActivity";

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String projectNumber;
    String serverRegIdPostUrl;
    String registrationId;
    String clientId;

    AtomicInteger msgId = new AtomicInteger();
    GoogleCloudMessaging gcm;
    Context context;

    public GcmActivity(String projectNumber, String serverRegIdPostUrl)
    {
        this.projectNumber = projectNumber;
        this.serverRegIdPostUrl = serverRegIdPostUrl;
    }

    // Methods to override in child
    public void onPreInit(Bundle savedInstanceState) {}
    public void onGooglePlayInitSuccess() {}
    public void onGooglePlayInitFailure() {}
    public void onSendRegistrationIdToServerException(Exception e, String serverRegIdPostUrl, String registrationId, String clientId) {}
    public void onSendRegistrationIdToServerResult(HttpResponse response, String serverRegIdPostUrl, String registrationId, String clientId) {}
    public void onRegistration(String message, String registrationId) {}
    public void onRegistrationIdRecovery(String registrationId) {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        onPreInit(savedInstanceState);
        Init();
    }

    private void Init()
    {
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices())
        {
            gcm = GoogleCloudMessaging.getInstance(this);
            registrationId = getRegistrationId(context);

            if (registrationId.isEmpty())
            {
                registerInBackground();
            }
        }
        else
        {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        checkPlayServices();
    }

    protected void GcmSend(Bundle data) throws IOException
    {
        String id = Integer.toString(msgId.incrementAndGet());
        gcm.send(projectNumber + "@gcm.googleapis.com", id, data);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    protected boolean checkPlayServices()
    {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
        {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                                                      PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }
            else
            {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            onGooglePlayInitFailure();
            return false;
        }
        onGooglePlayInitSuccess();
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     */
    protected void storeRegistrationId(Context context, String regId)
    {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p/>
     * If result is empty, the app needs to register.
     */
    protected String getRegistrationId(Context context)
    {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty())
        {
            Log.i(TAG, "Registration not found.");
            return "";
        }

        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion)
        {
            Log.i(TAG, "App version changed.");
            return "";
        }
        onRegistrationIdRecovery(registrationId);
        Log.i(TAG, "Device already registered, registration ID=" + registrationId);
        return registrationId;
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

                    storeRegistrationId(context, registrationId);
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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    protected static int getAppVersion(Context context)
    {
        try
        {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        }
        catch (PackageManager.NameNotFoundException e)
        {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    protected SharedPreferences getGcmPreferences(Context context)
    {
        return getSharedPreferences(this.getClass().getSimpleName(), Context.MODE_PRIVATE);
    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send messages to your app.
     */
    protected void sendRegistrationIdToBackend()
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
