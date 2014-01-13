package com.cfred1985.util.messaging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.database.Cursor;

public class SmsMessage
{
    public static final String ADDRESS = "address";
    public static final String PERSON = "person";
    public static final String DATE = "date";
    public static final String READ = "read";
    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String BODY = "body";
    public static final String SEEN = "seen";

    public static final int MESSAGE_TYPE_INBOX = 1;
    public static final int MESSAGE_TYPE_SENT = 2;

    public static final int MESSAGE_IS_NOT_READ = 0;
    public static final int MESSAGE_IS_READ = 1;

    public static final int MESSAGE_IS_NOT_SEEN = 0;
    public static final int MESSAGE_IS_SEEN = 1;

    private String phoneNumber;
    private String timestamp;
    private String messageBody;

    public SmsMessage(Cursor cursor)
    {
        int indexBody = cursor.getColumnIndex(BODY);
        int indexPhoneNum = cursor.getColumnIndex(ADDRESS);
        int indexTime = cursor.getColumnIndex(DATE);

        SetPhoneNumber(cursor.getString(indexPhoneNum));
        SetMessage(cursor.getString(indexBody));
        SetTimestamp(cursor.getString(indexTime));
    }

    public String GetPhoneNumber()
    {
        return phoneNumber;
    }

    public String GetTimestamp()
    {
        return timestamp;
    }

    public String GetMessage()
    {
        return messageBody;
    }

    public void SetPhoneNumber(String number)
    {
        phoneNumber = number;
    }

    public void SetTimestamp(String time)
    {
        Date date = new Date(Long.parseLong(time));
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());
        timestamp = formatter.format(date);
    }

    public void SetMessage(String message)
    {
        messageBody = message;
    }
}
