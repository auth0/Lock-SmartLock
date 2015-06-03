package com.auth0.lock.smartlock;

import android.app.Activity;

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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = com.auth0.identity.BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class RequestCredentialsTaskTest {

    private RequestCredentialsTask task;

    @Mock private Activity activity;
    @Mock private CredentialsApi credentialsApi;
    @Mock private SmartLock smartLock;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private CredentialRequest credentialRequest;
    @Mock private PendingResult<CredentialRequestResult> pendingResult;
    @Mock private CredentialRequestResult credentialRequestResult;
    @Mock private Status status;
    @Mock private Credential credential;

    @Captor private ArgumentCaptor<ResultCallback<CredentialRequestResult>> captor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        task = spy(new RequestCredentialsTask(activity));
        when(task.getCredentialsApi()).thenReturn(credentialsApi);
        when(smartLock.getCredentialClient()).thenReturn(googleApiClient);
        when(smartLock.newCredentialRequest()).thenReturn(credentialRequest);
        when(credentialsApi.request(eq(googleApiClient), eq(credentialRequest))).thenReturn(pendingResult);
        when(credentialRequestResult.getStatus()).thenReturn(status);
    }

    @Test
    public void shouldRequestCredentials() throws Exception {
        task.onConnected(smartLock, activity);
        verify(credentialsApi).request(eq(googleApiClient), eq(credentialRequest));
    }

    @Test
    public void shouldSetResultCallback() throws Exception {
        task.onConnected(smartLock, activity);
        verify(pendingResult).setResultCallback(Matchers.<ResultCallback<CredentialRequestResult>>any());
    }

    @Test
    public void shouldHandleRequestSuccess() throws Exception {
        when(status.isSuccess()).thenReturn(true);
        when(credentialRequestResult.getCredential()).thenReturn(credential);
        task.onConnected(smartLock, activity);
        verify(pendingResult).setResultCallback(captor.capture());
        final ResultCallback<CredentialRequestResult> resultCallback = captor.getValue();
        resultCallback.onResult(credentialRequestResult);
        verify(smartLock).onCredentialsRetrieved(eq(activity), eq(credential));
    }

    @Test
    public void shouldHandleRequestFailure() throws Exception {
        when(status.isSuccess()).thenReturn(false);
        task.onConnected(smartLock, activity);
        verify(pendingResult).setResultCallback(captor.capture());
        final ResultCallback<CredentialRequestResult> resultCallback = captor.getValue();
        resultCallback.onResult(credentialRequestResult);
        verify(smartLock).onCredentialRetrievalError(eq(activity), eq(status), eq(SmartLock.SMART_LOCK_READ));
    }
}