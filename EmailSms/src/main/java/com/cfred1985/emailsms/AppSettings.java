package com.cfred1985.emailsms;

import java.io.InputStream;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;

import com.cfred1985.util.mail.GMailOauthSender;
import com.cfred1985.util.mail.Mail;

import org.apache.http.MethodNotSupportedException;

public class AppSettings implements Parcelable
{
    public static final String IntentFilter = "com.cfred1985.emailsms.APP_SETTINGS";
    public static final String BundleKey = "AppSettings";

    public boolean ForwardSms;
    public boolean ForwardCalls;
    public boolean IsForwarding;
    public boolean OverrideEmail;
    public String EmailUsername;
    public String EmailPassword;
    public String ForwardEmailAddress;
    public String OAuthEmail;
    public String OAuthToken;

    public Mail EmailHelper;

    public AppSettings()
    {
        Reset();
    }

    public AppSettings(SharedPreferences p)
    {
        Reset();

        if (p == null)
        {
            return;
        }

        ForwardSms = p.getBoolean("ForwardSms", ForwardSms);
        ForwardCalls = p.getBoolean("ForwardCalls", ForwardCalls);
        IsForwarding = p.getBoolean("IsForwarding", IsForwarding);
        OverrideEmail = p.getBoolean("OverrideEmail", OverrideEmail);
        EmailUsername = p.getString("EmailUsername", EmailUsername);
        EmailPassword = p.getString("EmailPassword", EmailPassword);
        ForwardEmailAddress = p.getString("ForwardEmailAddress", ForwardEmailAddress);
        OAuthEmail = p.getString("OAuthEmail", OAuthEmail);
        OAuthToken = p.getString("OAuthToken", OAuthToken);
    }

    public void SaveSettings(SharedPreferences.Editor e)
    {
        if (e == null)
        {
            return;
        }

        e.putBoolean("ForwardSms", ForwardSms);
        e.putBoolean("ForwardCalls", ForwardCalls);
        e.putBoolean("IsForwarding", IsForwarding);
        e.putBoolean("OverrideEmail", OverrideEmail);
        e.putString("EmailUsername", EmailUsername);
        e.putString("EmailPassword", EmailPassword);
        e.putString("ForwardEmailAddress", ForwardEmailAddress);
        e.putString("OAuthEmail", OAuthEmail);
        e.putString("OAuthToken", OAuthToken);
    }

    private void Reset()
    {
        ForwardSms = true;
        ForwardCalls = true;
        IsForwarding = false;
        OverrideEmail = false;
        EmailUsername = null;
        EmailPassword = null;
        ForwardEmailAddress = null;
        EmailHelper = null;
        OAuthEmail = null;
        OAuthToken = null;
    }

    public void Publish(Service sender)
    {
        Intent intent = new Intent(AppSettings.IntentFilter);
        intent.putExtra(AppSettings.BundleKey, this);
        sender.sendBroadcast(intent);
    }

    public boolean IsLoggedIn()
    {
        return (!OverrideEmail && OAuthEmail != null && OAuthToken != null)
            || (OverrideEmail && EmailHelper != null && EmailHelper.IsLoggedIn);
    }

    public boolean Login()
    {
        if (OverrideEmail)
        {
            if (EmailUsername == null || EmailPassword == null || EmailUsername.isEmpty()
                            || EmailPassword.isEmpty())
            {
                return false;
            }

            EmailHelper = new Mail();
            if (!EmailHelper.Login(EmailUsername, EmailPassword))
            {
                EmailHelper = null;
                return false;
            }
        }

        return true;
    }

    public boolean Login(String user, String password)
    {
        EmailUsername = user;
        EmailPassword = password;
        return Login();
    }

    public void LoginAsync()
    {
        new AsyncLogin().execute();
    }

    public void Logout()
    {
        EmailHelper.Logout();
        EmailHelper = null;
    }

    public boolean SendEmail(String from, String subject, String body)
            throws MethodNotSupportedException
    {
        return SendEmail(from, subject, body, null);
    }

    public boolean SendEmail(String from, String subject, String body, InputStream[] attachments)
            throws MethodNotSupportedException
    {
        if (!IsLoggedIn())
            return false;

        if (OverrideEmail)
        {
            // EmailHelper.SendAsync(new String[] { ForwardEmailAddress }, from,
            // subject, body, attachments);
            // return true;
            throw new MethodNotSupportedException("Cannot yet SendEmail while overriding!");
        }
        else
        {
            try
            {
                new AsyncSendEmail().execute(from, subject, body, attachments);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    /* Everything below here is for implementing Parcelable */

    // 99.9% of the time you can just ignore this
    public int describeContents()
    {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags)
    {
        out.writeBooleanArray(new boolean[] { ForwardSms, ForwardCalls, IsForwarding, OverrideEmail });
        out.writeString(EmailUsername);
        out.writeString(EmailPassword);
        out.writeString(ForwardEmailAddress);
        out.writeString(OAuthEmail);
        out.writeString(OAuthToken);
    }

    // this is used to regenerate your object. All Parcelables must have a
    // CREATOR that implements these two methods
    public static final Parcelable.Creator<AppSettings> CREATOR = new Parcelable.Creator<AppSettings>()
    {
        public AppSettings createFromParcel(Parcel in)
        {
            return new AppSettings(in);
        }

        public AppSettings[] newArray(int size)
        {
            return new AppSettings[size];
        }
    };

    private AppSettings(Parcel in)
    {
        boolean[] bools = new boolean[4];
        in.readBooleanArray(bools);
        ForwardSms = bools[0];
        ForwardCalls = bools[1];
        IsForwarding = bools[2];
        OverrideEmail = bools[3];

        EmailUsername = in.readString();
        EmailPassword = in.readString();
        ForwardEmailAddress = in.readString();
        OAuthEmail = in.readString();
        OAuthToken = in.readString();
    }

    private class AsyncLogin extends AsyncTask<Void, Void, Void>
    {
        boolean loginResult;

        @Override
        protected Void doInBackground(Void... unused)
        {
            try
            {
                loginResult = Login();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused)
        {}
    }

    private class AsyncSendEmail extends AsyncTask<Object, Void, Void>
    {
        @Override
        protected Void doInBackground(Object... items)
        {
            try
            {
                String from = (String)items[0];
                String subject = (String)items[1];
                String body = (String)items[2];
                InputStream[] attachments = (InputStream[])items[3];

                new GMailOauthSender().sendMail(from, subject, body, ForwardEmailAddress,
                                OAuthEmail, OAuthToken, attachments);
            }
            catch (Exception e)
            {}

            return null;
        }

        @Override
        protected void onPostExecute(Void unused)
        {}
    }
}
