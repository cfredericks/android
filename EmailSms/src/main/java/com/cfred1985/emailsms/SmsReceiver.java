package com.cfred1985.emailsms;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.cfred1985.util.messaging.ContactHelper;

import org.apache.http.MethodNotSupportedException;

public class SmsReceiver extends BroadcastReceiver
{
    private static final String TAG = "SmsReceiver";
    private static final Object updateLock = new Object();
    private static final ContactHelper contactHelper = new ContactHelper();

    private static final String SMS_EXTRA_NAME = "pdus";
    private static final String SMS_URI = "content://sms";

    private AppSettings settings;

    private String fromEmailAddress = "emailtextmessage.auto.forward";
    private String emailSubject = "New SMS";

    public SmsReceiver()
    {
        settings = new AppSettings();
    }

    public SmsReceiver(String emailSubject, String fromEmailAddress)
    {
        this.emailSubject = emailSubject;
        this.fromEmailAddress = fromEmailAddress;
        settings = new AppSettings();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        synchronized (updateLock)
        {
            // Settings update
            if (extras.containsKey(AppSettings.BundleKey))
            {
                AppSettings smsExtra = extras.getParcelable(AppSettings.BundleKey);
                settings = smsExtra;
            }
            // New SMS
            else if (extras.containsKey(SMS_EXTRA_NAME) && settings.IsForwarding && settings.ForwardSms)
            {
                Object[] smsExtra = (Object[])extras.get(SMS_EXTRA_NAME);
                SendEmailsForSmses(context, smsExtra);
            }
        }
    }

    private void SendEmailsForSmses(Context context, Object[] smses)
    {
        for (int i = 0; i < smses.length; ++i)
        {
            // Extract SMS details
            SmsMessage sms = SmsMessage.createFromPdu((byte[])smses[i]);
            String body = sms.getMessageBody().toString();
            String phoneNum = sms.getOriginatingAddress();
            Date timestamp = new Date(sms.getTimestampMillis());

            // Lookup to see if we have contact details regarding this phone number
            ContactHelper.Contact contact = contactHelper.GetContact(context, phoneNum);
            String from = phoneNum;
            String subject = emailSubject;
            if (contact.name != null)
            {
                // Update email subject and from line to include the contact name
                from = contact.name + " (" + phoneNum + ")";
                subject = emailSubject + " from " + contact.name;
            }

            Log.d(TAG, "Received SMS from: " + from);

            // Send an email if we are logged into an email account
            if (settings.IsLoggedIn())
            {
                String message = HtmlFromSms(body, from, timestamp);
                InputStream[] attachments = contact.image != null
                    ? new InputStream[] { contact.image }
                    : null;
                boolean emailSuccess = false;
                try
                {
                    emailSuccess = settings.SendEmail(fromEmailAddress, subject, message, attachments);
                }
                catch (MethodNotSupportedException e)
                {
                    e.printStackTrace();
                }
                finally
                {
                    if (!emailSuccess)
                    {
                        Toast.makeText(context, "Error sending email on new SMS", Toast.LENGTH_SHORT);
                    }
                }
            }
        }
    }

    private static String HtmlFromSms(String body, String from, Date timestamp)
    {
        final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());
        final String messageTemplate = "<table width=\"300\" border=\"0\" cellspacing=\"0\">"
                + "<tr>"
                + "    <td bgcolor=\"#6699FF\"><center>##IMAGES##</center></td>"
                + "</tr>"
                + "<tr><td bgcolor=\"#6699FF\">&nbsp;</td></td>"
                + "<tr>"
                + "    <td bgcolor=\"#6699FF\"><center><font size=\"4\">##BODY##</font></center></td>"
                + "</tr>"
                + "<tr><td bgcolor=\"#6699FF\">&nbsp;</td></td>"
                + "<tr>"
                + "    <td><i><font size=\"1\">Sender: <b>##SENDER##</b></font></i></td>"
                + "</tr>"
                + "<tr>"
                + "    <td><i><font size=\"1\">Time: <b>##TIME##</b></font></i></td>"
                + "</tr>" + "</table>";

        String timestampFormatted = formatter.format(timestamp);

        return messageTemplate
                .replaceAll("##BODY##", body)
                .replaceAll("##SENDER##", from)
                .replaceAll("##TIME##", timestampFormatted);
    }
}
