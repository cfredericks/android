package com.cfred1985.emailsms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.cfred1985.util.mail.OAuthAccountManager;

public class MainActivity extends Activity
{
    private static final String TAG = "MainActivity";
    private static final Object settingsLock = new Object();

    private AppSettings settings;

    private BroadcastReceiver serviceResponseBroadcastListener;

    private Account selectedAccount;

    private Button buttonForward;
    private EditText txtEmail;
    private TextView lblEmail;
    private TextView lblNotifications;
    private Button buttonSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonForward = (Button)findViewById(R.id.btnStartForwarding);
        txtEmail = (EditText)findViewById(R.id.txtEmail);
        lblEmail = (TextView)findViewById(R.id.lblEmail);
        lblNotifications = (TextView)findViewById(R.id.lblNotifications);
        buttonSettings = (Button)findViewById(R.id.btnSettings);

        // If there is saved state, load the settings
        if (savedInstanceState != null && savedInstanceState.containsKey(AppSettings.BundleKey))
        {
            AppSettings recoveredSettings = (AppSettings)savedInstanceState.get(AppSettings.BundleKey);
            if (recoveredSettings != null)
            {
                Log.i(TAG, "Loading saved settings...");

                settings = recoveredSettings;
                txtEmail.setText(settings.ForwardEmailAddress);
                UpdateForwardButtonText();

                UpdateSelectedAccount(getApplicationContext(), settings.OAuthEmail);
            }
        }

        // Set GUI listeners
        txtEmail.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                {
                    synchronized (settingsLock)
                    {
                        settings.ForwardEmailAddress = txtEmail.getText().toString();
                        PublishSettings();
                    }
                    return true;
                }
                return false;
            }
        });

        txtEmail.setOnFocusChangeListener(new OnFocusChangeListener()
        {
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (!hasFocus)
                {
                    synchronized (settingsLock)
                    {
                        settings.ForwardEmailAddress = txtEmail.getText().toString();
                        PublishSettings();
                    }
                }
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        Log.d(TAG, "onResume()");

        if (settings == null)
        {
            Log.d(TAG, "settings == null");

            serviceResponseBroadcastListener = new BroadcastReceiver()
            {
                @Override
                public void onReceive(Context context, Intent intent)
                {
                    Log.d(TAG, "onReceive()");

                    Bundle extras = intent.getExtras();
                    if (extras != null && extras.containsKey(AppSettings.BundleKey))
                    {
                        Log.d(TAG, "extras != null");

                        // Get selected account
                        AppSettings smsExtra = extras.getParcelable(AppSettings.BundleKey);
                        UpdateSelectedAccount(context, smsExtra.OAuthEmail);
                        txtEmail.setText(smsExtra.ForwardEmailAddress);

                        synchronized (settingsLock)
                        {
                            settings = smsExtra;
                            UpdateForwardButtonText();
                            PublishSettings();
                        }

                        unregisterReceiver(serviceResponseBroadcastListener);
                        serviceResponseBroadcastListener = null;
                    }
                }
            };

            IntentFilter appSettingsFilter = new IntentFilter(AppSettings.IntentFilter);
            registerReceiver(serviceResponseBroadcastListener, appSettingsFilter);
        }

        UpdateForwardButtonText();
        Intent intent = new Intent(this, BackgroundService.class);
        if (settings != null)
        {
            intent.putExtra(AppSettings.BundleKey, settings);
        }
        startService(intent);
    }

    @Override
    public void onDestroy()
    {
        Log.d(TAG, "onDestroy()");

        super.onDestroy();
        if (serviceResponseBroadcastListener != null)
        {
            unregisterReceiver(serviceResponseBroadcastListener);
            serviceResponseBroadcastListener = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelable(AppSettings.BundleKey, settings);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (data == null)
        {
            return;
        }

        Bundle extras = data.getExtras();

        switch (requestCode)
        {
            case SettingsActivity.OPEN_SETTINGS:
                if (resultCode == RESULT_OK && extras.containsKey(AppSettings.BundleKey))
                {
                    synchronized (settingsLock)
                    {
                        settings = (AppSettings)extras.get(AppSettings.BundleKey);
                        UpdateSelectedAccount(getApplicationContext(), settings.OAuthEmail);
                        txtEmail.setText(settings.ForwardEmailAddress);
                        UpdateForwardButtonText();
                        PublishSettings();
                    }
                }
                break;
        }
    }

    private void UpdateSelectedAccount(Context context, String email)
    {
        selectedAccount = null;
        if (email != null)
        {
            Account[] accounts = AccountManager.get(MainActivity.this).getAccountsByType("com.google");
            for (Account account : accounts)
            {
                if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches()
                        && account.name.equals(email))
                {
                    selectedAccount = account;
                    break;
                }
            }
        }
        if (selectedAccount == null)
        {
            lblNotifications.setTextColor(Color.RED);
            lblNotifications.setText("No account selected for forwarding emails!");
            Toast.makeText(context, "Please select an account in Settings", Toast.LENGTH_SHORT);
            buttonForward.setEnabled(false);
            return;
        }
        else
        {
            lblNotifications.setText("");
            buttonForward.setEnabled(true);
        }

        Log.d(TAG, "Selected account: " + selectedAccount.name);

        if (settings != null)
        {
            Log.d(TAG, "EmailUsername=" + settings.EmailUsername + ","
                + "EmailPassword=" + settings.EmailPassword + ","
                + "OAuthEmail=" + settings.OAuthEmail + ","
                + "OAuthToken=" + settings.OAuthToken + ","
                + "ForwardEmailAddress=" + settings.ForwardEmailAddress + ","
                + "IsForwarding=" + settings.IsForwarding + ","
                + "ForwardSms=" + settings.ForwardSms + ",");
        }
        else
        {
            Log.d(TAG, "Settings is null!!");
        }

        // Get OAuthToken if needed
        if (settings != null && selectedAccount != null &&
                (settings.OAuthEmail == null || settings.OAuthToken == null))
        {
            Log.d(TAG, "About to get token: " + settings.ForwardEmailAddress + ", " +
                    "" + selectedAccount.name);

            // If not set, default the forwarding address to the selected account
            if (settings.ForwardEmailAddress == null)
            {
                settings.ForwardEmailAddress = selectedAccount.name;
            }

            settings.OAuthEmail = selectedAccount.name;
            OAuthAccountManager.GetOAuthToken(selectedAccount, MainActivity.this,
                new OAuthAccountManager.OAuthTokenAcquired()
                {
                    @Override
                    public void OnSuccess(Bundle bundle)
                    {
                        synchronized (settingsLock)
                        {
                            Log.d(TAG, "Got OAuth2 token");
                            settings.OAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                            PublishSettings();
                        }
                    }
                });
        }
    }

    private void UpdateForwardButtonText()
    {
        MainActivity.this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                //buttonForward.setEnabled(true);
                buttonSettings.setEnabled(true);

                if (settings != null && settings.IsForwarding)
                {
                    buttonForward.setText(getString(R.string.stop_forward));
                    txtEmail.setEnabled(false);
                    lblEmail.setEnabled(false);
                }
                else
                {
                    buttonForward.setText(getString(R.string.start_forward));
                    txtEmail.setEnabled(true);
                    lblEmail.setEnabled(true);
                }
            }
        });
    }

    private void PublishSettings()
    {
        Intent intent = new Intent(this, BackgroundService.class);
        intent.putExtra(AppSettings.BundleKey, settings);
        startService(intent);
    }

    public void btnSettings_onClick(View v)
    {
        synchronized (settingsLock)
        {
            settings.ForwardEmailAddress = txtEmail.getText().toString();
            PublishSettings();
        }

        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(AppSettings.BundleKey, settings);
        startActivityForResult(intent, SettingsActivity.OPEN_SETTINGS);
    }

    public void btnStartForwarding_onClick(View v)
    {
        synchronized (settingsLock)
        {
            settings.IsForwarding = !settings.IsForwarding;
            UpdateForwardButtonText();

            settings.ForwardEmailAddress = txtEmail.getText().toString();
            PublishSettings();
            
            if (settings.IsForwarding)
                Toast.makeText(getApplicationContext(), "Enabling forwarding...", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), "Disabling forwarding...", Toast.LENGTH_SHORT).show();
        }
    }
}
