package com.cfred1985.util.mail;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;

public class OAuthAccountManager
{
    public static void GetOAuthToken(Account account, Activity activity, OAuthTokenAcquired callback)
    {
        AccountManager am = AccountManager.get(activity);
        am.getAuthToken(account, "oauth2:https://mail.google.com/", null, activity, new OnTokenAcquired(callback),
                        null);
    }

    public static abstract class OAuthTokenAcquired
    {
        public abstract void OnSuccess(Bundle bundle);

        public void OnFail(Bundle bundle, Exception exception)
        {
            exception.printStackTrace();
        }
    }

    private static class OnTokenAcquired implements AccountManagerCallback<Bundle>
    {
        OAuthTokenAcquired callback;

        public OnTokenAcquired(OAuthTokenAcquired callback)
        {
            this.callback = callback;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> result)
        {
            Bundle bundle = null;
            try
            {
                bundle = result.getResult();
                callback.OnSuccess(bundle);
            }
            catch (Exception e)
            {
                callback.OnFail(bundle, e);
            }
        }
    }
}
