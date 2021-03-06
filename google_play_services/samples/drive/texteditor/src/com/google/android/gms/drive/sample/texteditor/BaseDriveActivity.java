// Copyright 2013 Google Inc. All Rights Reserved.

package com.google.android.gms.drive.sample.texteditor;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.drive.Drive;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;


/**
 * The BaseDriveActivity handles authentication and the connection to the Drive services.  Each
 * activity that interacts with Drive should extend this class.
 * <p>The connection is requested in onResume, and disconnected in onPause.  Extend
 * {@link #onClientConnected()} to be notified when the connection is active.
 */
public class BaseDriveActivity extends Activity
      implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    private static final String TAG = "DriveActivity";

    protected static final String EXTRA_ACCOUNT_NAME = "accountName";

    // Magic value indicating use the GMS Core default account
    protected static final String DEFAULT_ACCOUNT = "DEFAULT ACCOUNT";

    protected static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
    protected static final int NEXT_AVAILABLE_REQUEST_CODE = 2;

    // This variable can only be accessed from the UI thread.
    protected GoogleApiClient mGoogleApiClient;

    protected String mAccountName;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        // Determine the active account:
        // In the saved instance bundle?
        // In the intent?
        // If not found, use the default account.
        if (b != null) {
            mAccountName = b.getString(EXTRA_ACCOUNT_NAME);
        }
        if (mAccountName == null) {
            mAccountName = getIntent().getStringExtra(EXTRA_ACCOUNT_NAME);
        }
        if (mAccountName == null) {
            Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
            if (accounts.length > 0) {
                mAccountName = accounts[0].name;
                Log.d(TAG, "No account specified, selecting " + mAccountName);
            } else {
                mAccountName = DEFAULT_ACCOUNT;
                Log.d(TAG, "No enabled accounts, changing to DEFAULT ACCOUNT");
            }
        }

        if (mGoogleApiClient == null) {
            GoogleApiClient.Builder builder = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this);
            // If account name is unset in the builder, the default is used
            if (!DEFAULT_ACCOUNT.equals(mAccountName)) {
                builder.setAccountName(mAccountName);
            } else {
                Log.d(TAG, "No account specified, selecting default account.");
            }
            mGoogleApiClient = builder.build();
        }
    }

    /**
     * Sets the account name to be used for the GoogleApiClient.
     */
    protected void setAccount(String accountName) {
        mAccountName = accountName;
    }

    /**
     * Invoked when the drive client has successfully connected.  This can be used by extending
     * activities to perform actions once the client is fully initialized.
     */
    protected void onClientConnected() {
    }


    protected String getAccountName() {
        return mAccountName;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_ACCOUNT_NAME, mAccountName);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                mGoogleApiClient.connect();
                break;
            default:
                Log.w(TAG, "Unexpected activity request code" + requestCode);
        }
    }

    @Override
    protected void onPause() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onPause();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        onClientConnected();
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "GoogleApiClient disconnected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: " + result.getErrorCode());
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }
        // If user interaction is required to resolve the connection failure, the result will
        // contain a resolution.  This will launch a UI that allows the user to resolve the issue.
        // (E.g., authorize your app.)
        try {
            result.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
        } catch (SendIntentException e) {
            Log.i(TAG, "Send intent failed", e);
        }
    }

}
