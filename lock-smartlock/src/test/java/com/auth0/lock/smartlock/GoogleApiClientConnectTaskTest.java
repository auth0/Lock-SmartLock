package com.auth0.lock.smartlock;

import android.app.Activity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = com.auth0.identity.BuildConfig.class, sdk = 18, manifest = Config.NONE)
public class GoogleApiClientConnectTaskTest {

    private GoogleApiClientConnectTask task;

    @Mock private Activity activity;
    @Mock private SmartLock smartLock;
    @Mock private GoogleApiClient client;
    @Mock private ConnectionResult result;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        task = spy(new GoogleApiClientConnectTask(activity) {
            @Override
            void onConnected(SmartLock smartLock, Activity activity) {}
        });
        when(smartLock.getCredentialClient()).thenReturn(client);
        when(client.blockingConnect(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(result);
        when(result.isSuccess()).thenReturn(true);
    }

    @Test
    public void shouldBlockUntilConnectionIsMade() throws Exception {
        assertThat(task.doInBackground(smartLock), is(smartLock));
        verify(client).blockingConnect(eq(5l), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldCallOnConnectedOnSuccess() throws Exception {
        when(client.isConnected()).thenReturn(true);
        task.onPostExecute(smartLock);
        verify(task).onConnected(eq(smartLock), eq(activity));
    }

    @Test
    public void shouldNotCallOnConnectedWhenConnectionFailed() throws Exception {
        when(client.isConnected()).thenReturn(false);
        task.onPostExecute(smartLock);
        verify(task, never()).onConnected(eq(smartLock), eq(activity));
    }

    @Test
    public void shouldNotCallOnConnectedWhenCancelled() throws Exception {
        when(task.isCancelled()).thenReturn(true);
        task.onPostExecute(smartLock);
        verify(task, never()).onConnected(eq(smartLock), eq(activity));
    }

    @Test
    public void shouldNotCallOnConnectedWhenActivityIsNull() throws Exception {
        task.onPostExecute(smartLock);
        task = spy(new GoogleApiClientConnectTask(null) {
            @Override
            void onConnected(SmartLock smartLock, Activity activity) {}
        });
        verify(task, never()).onConnected(eq(smartLock), eq(activity));
    }

}