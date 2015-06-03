package com.auth0.lock.smartlock;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;

import com.auth0.api.APIClient;
import com.auth0.api.callback.AuthenticationCallback;
import com.auth0.lock.Lock;
import com.auth0.lock.LockActivity;
import com.auth0.lock.LockProvider;
import com.auth0.lock.credentials.CredentialStoreCallback;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Mockito.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = com.auth0.identity.BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class SmartLockTest {

    private static final String EMAIL = "smartlock@auth0.com";
    private static final String PASSWORD = "verysecretcontraseñadelusuario";
    private static final String USERNAME = "smartlock";
    private static final String PICTURE_URL = "https://somepicture.com/avatar.png";
    private SmartLock smartLock;

    @Mock private APIClient apiClient;
    @Mock private GoogleApiClient googleApiClient;
    @Mock private Activity activity;
    @Mock private Credential credential;
    @Mock private Status status;
    @Mock private GoogleApiClientConnectTask task;
    @Mock private CredentialStoreCallback callback;
    @Mock private Intent intent;
    @Captor private ArgumentCaptor<Intent> captor;
    @Captor private ArgumentCaptor<SaveCredentialTask> credentialCaptor;
    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        smartLock = spy(new SmartLock(googleApiClient, apiClient));
        when(credential.getId()).thenReturn(EMAIL);
        when(credential.getPassword()).thenReturn(PASSWORD);
        doNothing().when(smartLock).startTask(any(GoogleApiClientConnectTask.class));
    }

    @Test
    public void shouldLoginWhenCredentialsAreRetrievedFromSmartLock() throws Exception {
        smartLock.onCredentialsRetrieved(activity, credential);
        verify(apiClient).login(eq(EMAIL), eq(PASSWORD), anyMapOf(String.class, Object.class), any(AuthenticationCallback.class));
    }

    @Test
    public void shouldShowResolutionDialogWhenFailedToFetchCredentials() throws Exception {
        when(status.hasResolution()).thenReturn(true);
        when(status.getStatusCode()).thenReturn(CommonStatusCodes.RESOLUTION_REQUIRED);
        smartLock.onCredentialRetrievalError(activity, status, SmartLock.SMART_LOCK_READ);
        verify(status).startResolutionForResult(eq(activity), eq(SmartLock.SMART_LOCK_READ));
    }

    @Test
    public void shouldStartLockOnUnrecoverableErrorWhenFetcingCredentials() throws Exception {
        when(status.hasResolution()).thenReturn(false);

        smartLock.onCredentialRetrievalError(activity, status, SmartLock.SMART_LOCK_READ);

        verify(status, never()).startResolutionForResult(eq(activity), eq(SmartLock.SMART_LOCK_READ));
        verifyLockStartedFromActivity(activity);
    }

    @Test
    public void shouldShowLockWhenFailedShowResolution() throws Exception {
        when(status.hasResolution()).thenReturn(true);
        when(status.getStatusCode()).thenReturn(CommonStatusCodes.RESOLUTION_REQUIRED);
        final IntentSender.SendIntentException exception = new IntentSender.SendIntentException();
        doThrow(exception).when(status).startResolutionForResult(eq(activity), eq(SmartLock.SMART_LOCK_READ));

        smartLock.onCredentialRetrievalError(activity, status, SmartLock.SMART_LOCK_READ);

        verify(status).startResolutionForResult(eq(activity), eq(SmartLock.SMART_LOCK_READ));
        verifyLockStartedFromActivity(activity);
    }

    @Test
    public void shouldClearItselfOnStop() throws Exception {
        doCallRealMethod().when(smartLock).startTask(eq(task));
        smartLock.setCallback(callback);
        smartLock.startTask(task);
        smartLock.onStop();
        assertThat(smartLock.credentialRequest, is(nullValue(CredentialRequest.class)));
        assertThat(smartLock.currentTask(), is(nullValue(GoogleApiClientConnectTask.class)));
        assertThat(smartLock.getCallback(), notNullValue(CredentialStoreCallback.class));
        verifyCallbackIsCleaned();
        verify(task).cancel(false);
        verify(googleApiClient).disconnect();
    }

    @Test
    public void shouldTryToConnectOnStart() throws Exception {
        smartLock.onStart();
        verify(googleApiClient).connect();
    }

    @Test
    public void shouldIgnoreUnknownRequestCodes() throws Exception {
        smartLock.onActivityResult(activity, -1, -1, null);
        verifyZeroInteractions(googleApiClient, apiClient, activity, callback);
    }

    @Test
    public void shouldHandleSuccessfulSmartLockRead() throws Exception {
        when(intent.getParcelableExtra(Credential.EXTRA_KEY)).thenReturn(credential);
        doNothing().when(smartLock).onCredentialsRetrieved(eq(activity), eq(credential));
        smartLock.onActivityResult(activity, SmartLock.SMART_LOCK_READ, Activity.RESULT_OK, intent);
        verify(smartLock).onCredentialsRetrieved(eq(activity), eq(credential));
    }

    @Test
    public void shouldShowLockOnFailedRead() throws Exception {
        smartLock.onActivityResult(activity, SmartLock.SMART_LOCK_READ, Activity.RESULT_CANCELED, null);
        verify(smartLock, never()).onCredentialsRetrieved(eq(activity), eq(credential));
        verifyLockStartedFromActivity(activity);
    }

    @Test
    public void shouldHandleSuccessfulSmartLockSave() throws Exception {
        smartLock.setCallback(callback);
        smartLock.onActivityResult(activity, SmartLock.SMART_LOCK_SAVE, Activity.RESULT_OK, null);
        verify(callback).onSuccess();
        verifyCallbackIsCleaned();
    }

    @Test
    public void shouldReportErrorOnFailedCallback() throws Exception {
        smartLock.setCallback(callback);
        smartLock.onActivityResult(activity, SmartLock.SMART_LOCK_SAVE, Activity.RESULT_CANCELED, null);
        verify(callback).onError(eq(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_CANCELLED), isNull(Throwable.class));
        verifyCallbackIsCleaned();
    }

    @Test
    public void shouldSaveCredentials() throws Exception {
        smartLock.saveFromActivity(activity, USERNAME, EMAIL, PASSWORD, PICTURE_URL, callback);
        assertThat(smartLock.getCallback(), equalTo(callback));
        verify(smartLock).startTask(credentialCaptor.capture());
        final Credential credential = credentialCaptor.getValue().getCredential();
        assertThat(credential.getId(), equalTo(EMAIL));
        assertThat(credential.getName(), equalTo(USERNAME));
        assertThat(credential.getProfilePictureUri().toString(), equalTo(PICTURE_URL));
        assertThat(credential.getPassword(), equalTo(PASSWORD));
    }

    @Test
    public void shouldSaveCredentialsWithoutEmail() throws Exception {
        smartLock.saveFromActivity(activity, USERNAME, null, PASSWORD, PICTURE_URL, callback);
        verify(smartLock).startTask(credentialCaptor.capture());
        final Credential credential = credentialCaptor.getValue().getCredential();
        assertThat(credential.getId(), equalTo(USERNAME));
        assertThat(credential.getName(), equalTo(USERNAME));
    }

    @Test
    public void shouldSaveCredentialsWithoutPicture() throws Exception {
        smartLock.saveFromActivity(activity, USERNAME, EMAIL, PASSWORD, null, callback);
        verify(smartLock).startTask(credentialCaptor.capture());
        final Credential credential = credentialCaptor.getValue().getCredential();
        assertThat(credential.getProfilePictureUri(), nullValue(Uri.class));
    }

    @Test
    public void shouldFailToSaveWithNoIdentifier() throws Exception {
        smartLock.saveFromActivity(activity, null, null, PASSWORD, null, callback);
        verify(smartLock, never()).startTask(any(GoogleApiClientConnectTask.class));
        verify(callback).onError(eq(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_FAILED), isNull(Throwable.class));
    }

    @Test
    public void shouldFailToSaveWithNoPassword() throws Exception {
        smartLock.saveFromActivity(activity, USERNAME, EMAIL, null, null, callback);
        verify(smartLock, never()).startTask(any(GoogleApiClientConnectTask.class));
        verify(callback).onError(eq(CredentialStoreCallback.CREDENTIAL_STORE_SAVE_FAILED), isNull(Throwable.class));
    }

    @Test
    public void shouldRequestCredentialsOnLoginAttempt() throws Exception {
        smartLock.loginFromActivity(activity);
        verify(smartLock).startTask(any(RequestCredentialsTask.class));
    }

    @Test
    public void shouldFetchSmartLock() throws Exception {
        Application application = mock(Application.class, withSettings().extraInterfaces(LockProvider.class));
        when(((LockProvider)application).getLock()).thenReturn(smartLock);
        when(activity.getApplication()).thenReturn(application);
        assertThat(SmartLock.getSmartLock(activity), is(smartLock));
    }

    @Test
    public void shouldFailWhenApplicationDoesNotImplementLockProvider() throws Exception {
        expectedException.expect(IllegalStateException.class);
        Application application = mock(Application.class);
        when(activity.getApplication()).thenReturn(application);
        SmartLock.getSmartLock(activity);
    }

    @Test
    public void shouldFailWhenLockIsNotSmartLock() throws Exception {
        expectedException.expect(IllegalStateException.class);
        Application application = mock(Application.class, withSettings().extraInterfaces(LockProvider.class));
        when(((LockProvider)application).getLock()).thenReturn(mock(Lock.class));
        when(activity.getApplication()).thenReturn(application);
        SmartLock.getSmartLock(activity);
    }

    private void verifyLockStartedFromActivity(Activity activity) {
        verify(activity).startActivity(captor.capture());
        final Intent intent = captor.getValue();
        assertThat(intent.getComponent().getClassName(), equalTo(LockActivity.class.getName()));
    }

    private void verifyCallbackIsCleaned() {
        assertThat(smartLock.getCallback(), not(equalTo(callback)));
    }

}
