package com.auth0.lock.smartlock;

import android.app.Activity;
import android.content.IntentSender;
import android.util.Log;

import com.auth0.lock.credentials.CredentialStoreCallback;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.android.gms.common.api.ResultCallback;

class SaveCredentialTask extends GoogleApiClientConnectTask {
    private static final String TAG = SaveCredentialTask.class.getName();
    private final Credential credential;

    public SaveCredentialTask(Activity activity, Credential credential) {
        super(activity);
        this.credential = credential;
    }

    @Override
    void onConnected(final SmartLock smartLock, final Activity activity) {
        getCredentialsApi().save(smartLock.getCredentialClient(), credential).setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
            @Override
            public void onResult(com.google.android.gms.common.api.Status status) {
                if (resolveStatus(status, activity, smartLock)) {
                    smartLock.clearCredentialStoreCallback();
                }
            }

            private boolean resolveStatus(com.google.android.gms.common.api.Status status, Activity activity, SmartLock smartLock) {
                final CredentialStoreCallback callback = smartLock.getCallback();
                if (status.isSuccess()) {
                    Log.d(TAG, "Saved user's credentials in Smart Lock");
                    callback.onSuccess();
                    return true;
                }
                if (status.hasResolution()) {
                    try {
                        status.startResolutionForResult(activity, SmartLock.SMART_LOCK_SAVE);
                        return false;
                    } catch (IntentSender.SendIntentException e) {
                        Log.e(TAG, "Couldn't resolve Smart Lock save issues", e);
                        callback.onError(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_FAILED, e);
                    }
                } else {
                    callback.onError(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_CANCELLED, null);
                }
                return true;
            }
        });
    }

    CredentialsApi getCredentialsApi() {
        return Auth.CredentialsApi;
    }
}
