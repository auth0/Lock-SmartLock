package com.auth0.lock.smartlock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.auth0.api.callback.AuthenticationCallback;
import com.auth0.core.Token;
import com.auth0.core.UserProfile;
import com.auth0.lock.Lock;
import com.auth0.lock.LockActivity;
import com.auth0.lock.LockProvider;
import com.auth0.lock.credentials.CredentialStore;
import com.auth0.lock.credentials.CredentialStoreCallback;
import com.auth0.lock.error.ErrorDialogBuilder;
import com.auth0.lock.error.LoginAuthenticationErrorBuilder;
import com.auth0.lock.event.AuthenticationError;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

import java.lang.ref.WeakReference;

public class SmartLock implements CredentialStore, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int SMART_LOCK_READ = 90001;
    public static final int SMART_LOCK_SAVE = 90002;

    private static final String TAG = SmartLock.class.getName();

    private final GoogleApiClient credentialClient;
    private CredentialRequest credentialRequest;
    private CredentialStoreCallback callback;
    private WeakReference<GoogleApiClientConnectTask> task;

    public SmartLock(Context context) {
        credentialClient = new GoogleApiClient.Builder(context.getApplicationContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
        clearTask();
        clearCredentialStoreCallback();
    }

    /**
     * Prepares SmartLock resources to perform authentication operations
     * This should be called from {@link Activity#onStart()} of the Activity that needs authentication
     */
    public void onStart() {
        getCredentialClient().connect();
    }

    /**
     * Cleans up SmartLock resources and state
     * This should be called from {@link Activity#onStop()} of the Activity that needs authentication
     */
    public void onStop() {
        clearCredentialStoreCallback();
        credentialRequest = null;
        getCredentialClient().disconnect();
        if (task.get() != null) {
            task.get().cancel(false);
        }
        clearTask();
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SMART_LOCK_READ:
                if (resultCode == Activity.RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    onCredentialsRetrieved(activity, credential);
                } else {
                    startLockFromActivity(activity);
                }
                break;
            case SMART_LOCK_SAVE:
                final CredentialStoreCallback callback = getCallback();
                if (resultCode == Activity.RESULT_OK) {
                    callback.onSuccess();
                } else {
                    callback.onError(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_CANCELLED, null);
                }
                clearCredentialStoreCallback();
                break;
        }
    }

    @Override
    public void saveFromActivity(Activity activity, String username, String email, String password, String pictureUrl, CredentialStoreCallback callback) {
        setCallback(callback);
        String id = email != null ? email : username;
        Uri pictureUri = pictureUrl != null ? Uri.parse(pictureUrl) : null;
        final Credential credential = new Credential.Builder(id)
                .setName(username)
                .setProfilePictureUri(pictureUri)
                .setPassword(password)
                .build();
        GoogleApiClientConnectTask task = new SaveCredentialTask(activity, credential);
        task.execute(this);
        this.task = new WeakReference<>(task);
    }

    public void loginFromActivity(Activity activity) {
        GoogleApiClientConnectTask task = new RequestCredentialsTask(activity);
        task.execute(this);
        this.task = new WeakReference<>(task);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "GoogleApiClient connection failed with code " + connectionResult.getErrorCode());
    }

    void clearCredentialStoreCallback() {
        callback = new CredentialStoreCallback() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onError(int errorCode, Throwable e) {
            }
        };
    }

    @NonNull CredentialStoreCallback getCallback() {
        return callback;
    }

    void setCallback(@NonNull CredentialStoreCallback callback) {
        this.callback = callback;
    }

    @NonNull GoogleApiClient getCredentialClient() {
        return credentialClient;
    }

    @NonNull CredentialRequest newCredentialRequest() {
        credentialRequest = new CredentialRequest.Builder()
                .setSupportsPasswordLogin(true)
                .build();
        return credentialRequest;
    }

    void onCredentialsRetrieved(final Activity activity, final Credential credential) {
        Log.v(TAG, "Credentials : " + credential.getName());
        String email = credential.getId();
        String password = credential.getPassword();

        LockProvider provider = (LockProvider) activity.getApplication();
        final Lock lock = provider.getLock();
        lock.getAPIClient().login(email, password, lock.getAuthenticationParameters(), new AuthenticationCallback() {
            @Override
            public void onSuccess(UserProfile profile, Token token) {
                Intent result = new Intent(Lock.AUTHENTICATION_ACTION)
                        .putExtra(Lock.AUTHENTICATION_ACTION_PROFILE_PARAMETER, profile)
                        .putExtra(Lock.AUTHENTICATION_ACTION_TOKEN_PARAMETER, token);
                LocalBroadcastManager.getInstance(activity).sendBroadcast(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                LoginAuthenticationErrorBuilder builder = new LoginAuthenticationErrorBuilder();
                AuthenticationError error = builder.buildFrom(throwable);
                ErrorDialogBuilder.showAlertDialog(activity, error);
            }
        });
    }

    void onCredentialRetrievalError(Activity activity, Status status, int requestCode) {
        if (status.hasResolution() && status.getStatusCode() != CommonStatusCodes.SIGN_IN_REQUIRED) {
            try {
                status.startResolutionForResult(activity, requestCode);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to send intent for Smart Lock resolution", e);
                startLockFromActivity(activity);
            }
        } else {
            Log.e(TAG, "Couldn't read the credentials using Smart Lock. Showing Lock...");
            startLockFromActivity(activity);
        }
    }

    private void startLockFromActivity(Activity activity) {
        activity.startActivity(new Intent(activity, LockActivity.class));
    }

    private void clearTask() {
        task = new WeakReference<>(null);
    }

}
