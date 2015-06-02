package com.auth0.lock.smartlock;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.api.ResultCallback;

class RequestCredentialsTask extends GoogleApiClientConnectTask {

    private static final String TAG = RequestCredentialsTask.class.getName();

    public RequestCredentialsTask(Activity activity) {
        super(activity);
    }

    @Override
    void onConnected(final SmartLock smartLock, final Activity activity) {
        Log.v(TAG, "Requesting credentials from SmartLock");
        Auth.CredentialsApi.request(smartLock.getCredentialClient(), smartLock.newCredentialRequest()).setResultCallback(new ResultCallback<CredentialRequestResult>() {
            @Override
            public void onResult(CredentialRequestResult credentialRequestResult) {
                final com.google.android.gms.common.api.Status status = credentialRequestResult.getStatus();
                if (status.isSuccess()) {
                    final Credential credential = credentialRequestResult.getCredential();
                    Log.v(TAG, "Retrieved credentials for type " + credential.getAccountType());
                    smartLock.onCredentialsRetrieved(activity, credential);
                } else {
                    smartLock.onCredentialRetrievalError(activity, status, SmartLock.SMART_LOCK_READ);
                }
            }
        });
    }
}
