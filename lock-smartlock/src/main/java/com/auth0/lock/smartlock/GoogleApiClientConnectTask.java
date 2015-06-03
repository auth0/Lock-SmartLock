package com.auth0.lock.smartlock;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.auth0.lock.credentials.CredentialStoreCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

abstract class GoogleApiClientConnectTask extends AsyncTask<SmartLock, Void, SmartLock> {

    private static final String TAG = GoogleApiClientConnectTask.class.getName();
    private final WeakReference<Activity> activityRef;

    public GoogleApiClientConnectTask(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    @Override
    protected SmartLock doInBackground(SmartLock... smartLocks) {
        Log.v(TAG, "Connecting GoogleApiClient....");
        final SmartLock smartLock = smartLocks[0];
        GoogleApiClient client = smartLock.getCredentialClient();
        ConnectionResult result = client.blockingConnect(5, TimeUnit.SECONDS);
        if (!result.isSuccess()) {
            Log.e(TAG, "Failed to connect with GoogleApiClient with code " + result.getErrorCode());
        }
        return smartLock;
    }

    @Override
    protected void onPostExecute(SmartLock smartLock) {
        final Activity activity = this.activityRef.get();
        if (activity == null) {
            Log.w(TAG, "No activity or smart lock store found when trying to save credentials");
            smartLock.getCallback().onError(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_FAILED, null);
            smartLock.clearCredentialStoreCallback();
            return;
        }
        GoogleApiClient client = smartLock.getCredentialClient();
        if (client.isConnected() && !isCancelled()) {
            onConnected(smartLock, activity);
        }
    }

    abstract void onConnected(SmartLock smartLock, Activity activity);
}
