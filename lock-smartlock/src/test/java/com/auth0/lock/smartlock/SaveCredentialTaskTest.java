package com.auth0.lock.smartlock;

import android.app.Activity;
import android.content.IntentSender;

import com.auth0.lock.credentials.CredentialStoreCallback;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.CredentialsApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = com.auth0.identity.BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class SaveCredentialTaskTest {

    private SaveCredentialTask task;

    @Mock private Activity activity;
    @Mock private CredentialsApi credentialsApi;
    @Mock private SmartLock smartLock;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private PendingResult<Status> pendingResult;
    @Mock private Status status;
    @Mock private Credential credential;
    @Mock private CredentialStoreCallback callback;

    @Captor
    private ArgumentCaptor<ResultCallback<Status>> captor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        task = spy(new SaveCredentialTask(activity, credential));
        when(task.getCredentialsApi()).thenReturn(credentialsApi);
        when(credentialsApi.save(eq(googleApiClient), eq(credential))).thenReturn(pendingResult);
        when(smartLock.getCredentialClient()).thenReturn(googleApiClient);
        when(smartLock.getCallback()).thenReturn(callback);
    }

    @Test
    public void shouldRequestSave() throws Exception {
        task.onConnected(smartLock, activity);
        verify(credentialsApi).save(eq(googleApiClient), eq(credential));
    }

    @Test
    public void shouldSetResultCallback() throws Exception {
        task.onConnected(smartLock, activity);
        verify(pendingResult).setResultCallback(Matchers.<ResultCallback<Status>>any());
    }

    @Test
    public void shouldHandleSuccess() throws Exception {
        triggerResultWithSuccess(true);
        verify(callback).onSuccess();
        verify(smartLock).clearCredentialStoreCallback();
    }

    @Test
    public void shouldHandleNeedForResolution() throws Exception {
        triggerResultWithSuccess(false, true);
        verify(status).startResolutionForResult(eq(activity), eq(SmartLock.SMART_LOCK_SAVE));
        verifyZeroInteractions(callback);
        verify(smartLock, never()).clearCredentialStoreCallback();
    }

    @Test
    public void shouldHandleExceptionWhenResolutionStarts() throws Exception {
        final IntentSender.SendIntentException exception = new IntentSender.SendIntentException();
        doThrow(exception).when(status).startResolutionForResult(eq(activity), eq(SmartLock.SMART_LOCK_SAVE));
        triggerResultWithSuccess(false, true);
        verify(status).startResolutionForResult(eq(activity), eq(SmartLock.SMART_LOCK_SAVE));
        verify(callback).onError(eq(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_FAILED), eq(exception));
    }

    @Test
    public void shouldHandleFailure() throws Exception {
        triggerResultWithSuccess(false);
        verify(callback).onError(eq(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_CANCELLED), isNull(Throwable.class));
    }

    private void triggerResultWithSuccess(boolean success) {
        triggerResultWithSuccess(success, false);
    }

    private void triggerResultWithSuccess(boolean success, boolean hasResolution) {
        when(status.hasResolution()).thenReturn(hasResolution);
        when(status.isSuccess()).thenReturn(success);
        task.onConnected(smartLock, activity);
        verify(pendingResult).setResultCallback(captor.capture());
        final ResultCallback<Status> resultCallback = captor.getValue();
        resultCallback.onResult(status);
    }
}