package com.cfred1985.emailsms;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class SettingsActivity extends Activity
{
    private static final String TAG = "SettingsActivity";
    public static final int OPEN_SETTINGS = 0;

    private AppSettings settings;

    private CheckBox overrideEmailCheck;
    private CheckBox forwardSmsCheck;
    private CheckBox forwardCallsCheck;
    private ListView accountList;
    private EditText usernameText;
    private EditText passwordText;
    private TextView usernameLabel;
    private TextView passwordLabel;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        overrideEmailCheck = (CheckBox)findViewById(R.id.chkOverrideEmail);
        forwardSmsCheck = (CheckBox)findViewById(R.id.chkForwardSms);
        forwardCallsCheck = (CheckBox)findViewById(R.id.chkForwardCalls);
        accountList = (ListView)findViewById(R.id.lstAccounts);
        usernameText = (EditText)findViewById(R.id.txtUsername);
        passwordText = (EditText)findViewById(R.id.txtPassword);
        buttonLogin = (Button)findViewById(R.id.btnLogin);
        usernameLabel = (TextView)findViewById(R.id.lblUsername);
        passwordLabel = (TextView)findViewById(R.id.lblPassword);

        overrideEmailCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton v, boolean ischk)
            {
                chkOverrideEmail_onChange(v, ischk);
            }
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(AppSettings.BundleKey))
        {
            settings = (AppSettings)extras.get(AppSettings.BundleKey);
        }
        if (settings == null)
        {
            settings = new AppSettings();
        }

        settings.Login();
        buttonLogin.setText(settings.OverrideEmail && settings.IsLoggedIn()
            ? getString(R.string.logout)
            : getString(R.string.login));

        Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
        if (accounts != null && accounts.length > 0)
        {
            ArrayList<String> emailAccounts = new ArrayList();
            for (Account account : accounts)
            {
                if (Patterns.EMAIL_ADDRESS.matcher(account.name).matches())
                {
                    emailAccounts.add(account.name);
                }
            }
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_activated_1, emailAccounts);
            accountList.setAdapter(adapter);
            // Set selected item if one is
            if (settings.OAuthEmail != null)
            {
                for (int i = 0; i < emailAccounts.size(); i++)
                {
                    if (settings.OAuthEmail.equals(emailAccounts.get(i)))
                    {
                        accountList.setItemChecked(i, true);
                        break;
                    }
                }
            }
            accountList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    settings.OAuthEmail = (String)parent.getItemAtPosition(position);
                    settings.OAuthToken = null;
                    Log.d(TAG, "New item selected: " + settings.OAuthEmail);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {
                    settings.OAuthEmail = null;
                    settings.OAuthToken = null;
                }
            });
            accountList.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                    {
                        settings.OAuthEmail = (String)parent.getItemAtPosition(position);
                        settings.OAuthToken = null;
                        Log.d(TAG, "New item clicked: " + parent.getItemAtPosition(position));
                    }
                });
        }

        LoadSettings();
    }

    void LoadSettings()
    {
        overrideEmailCheck.setChecked(settings.OverrideEmail);
        forwardSmsCheck.setChecked(settings.ForwardSms);
        forwardCallsCheck.setChecked(settings.ForwardCalls);
        usernameText.setText(settings.EmailUsername);
        passwordText.setText(settings.EmailPassword);

        chkOverrideEmail_onChange(null, settings.OverrideEmail);
    }

    void UpdateSettingsFromGui()
    {
        settings.OverrideEmail = overrideEmailCheck.isChecked();
        settings.ForwardSms = forwardSmsCheck.isChecked();
        settings.ForwardCalls = forwardCallsCheck.isChecked();
        settings.EmailUsername = usernameText.getText().toString();
        settings.EmailPassword = passwordText.getText().toString();
    }

    public void btnLogin_onClick(View v)
    {
        UpdateSettingsFromGui();

        if (!settings.IsLoggedIn())
        {
            SetLoginBoxesEnabled(false);
            new AsyncLogin().execute();
        }
        else
        {
            settings.Logout();
            buttonLogin.setText(getString(R.string.login));
            SetLoginBoxesEnabled(true);
        }
    }

    public void btnDone_onClick(View v)
    {
        UpdateSettingsFromGui();

        Intent returnIntent = new Intent();
        returnIntent.putExtra(AppSettings.BundleKey, settings);
        setResult(RESULT_OK, returnIntent);

        finish();
    }

    void chkOverrideEmail_onChange(CompoundButton v, boolean isChk)
    {
        buttonLogin.setEnabled(isChk);
        settings.OverrideEmail = isChk;
        SetLoginBoxesEnabled(isChk && !settings.IsLoggedIn());
    }

    void SetLoginBoxesEnabled(boolean enabled)
    {
        usernameText.setEnabled(enabled);
        passwordText.setEnabled(enabled);
        usernameLabel.setEnabled(enabled);
        passwordLabel.setEnabled(enabled);
    }

    private class AsyncLogin extends AsyncTask<Void, Void, Void>
    {
        boolean loginResult;

        @Override
        protected Void doInBackground(Void... unused)
        {
            try
            {
                loginResult = settings.Login();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused)
        {
            if (loginResult)
            {
                Toast.makeText(getApplicationContext(), "Login success!", Toast.LENGTH_SHORT).show();
                buttonLogin.setText(getString(R.string.logout));
                SetLoginBoxesEnabled(false);
            }
            else
            {
                Toast.makeText(getApplicationContext(), "Login failure!", Toast.LENGTH_SHORT).show();
                SetLoginBoxesEnabled(true);
            }
        }
    }
}
