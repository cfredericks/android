package com.cfred1985.util.messaging;

import java.io.InputStream;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactHelper
{
    public class Contact
    {
        public String number;
        public String name;
        public String id;
        public InputStream image;

        public Contact()
        {
            number = null;
            name = null;
            id = null;
            image = null;
        }
    }

    public Contact GetContact(Context context, String number)
    {
        Log.v("ContactHelper", "Started GetContact...");

        Contact contact = new Contact();
        contact.number = number;

        // define the columns I want the query to return
        String[] projection = new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup._ID };

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(number));

        // query time
        Cursor cursor = context.getContentResolver()
                        .query(contactUri, projection, null, null, null);

        if (cursor.moveToFirst())
        {
            // Get values from contacts database:
            contact.id = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
            contact.name = cursor.getString(cursor
                            .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));

            // Get photo of contactId as input stream:
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                            Long.parseLong(contact.id));
            contact.image = ContactsContract.Contacts.openContactPhotoInputStream(
                            context.getContentResolver(), uri);

        }

        return contact;
    }
}
