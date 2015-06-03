package com.auth0.lock.smartlock.app;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.auth0.core.Token;
import com.auth0.core.UserProfile;
import com.auth0.lock.Lock;
import com.auth0.lock.smartlock.SmartLock;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private ProgressDialog progressDialog;
    private LocalBroadcastManager broadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button loginButton = (Button) findViewById(R.id.main_login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProgressDialog();
                SmartLock.getSmartLock(MainActivity.this).loginFromActivity(MainActivity.this);
            }
        });
        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(authenticationReceiver, new IntentFilter(Lock.AUTHENTICATION_ACTION));
        broadcastManager.registerReceiver(cancelReceiver, new IntentFilter(Lock.CANCEL_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        broadcastManager.unregisterReceiver(authenticationReceiver);
        broadcastManager.unregisterReceiver(cancelReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        SmartLock.getSmartLock(MainActivity.this).onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SmartLock.getSmartLock(MainActivity.this).onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SmartLock.getSmartLock(MainActivity.this).onActivityResult(this, requestCode, resultCode, data);
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = null;
    }

    private BroadcastReceiver authenticationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UserProfile profile = intent.getParcelableExtra(Lock.AUTHENTICATION_ACTION_PROFILE_PARAMETER);
            Token token = intent.getParcelableExtra(Lock.AUTHENTICATION_ACTION_TOKEN_PARAMETER);
            Log.d(TAG, "User " + profile.getName() + " with token " + token.getIdToken());
            TextView welcomeLabel = (TextView) findViewById(R.id.main_welcome_label);
            welcomeLabel.setText("Welcome " + profile.getName());
            dismissProgressDialog();
        }
    };

    private BroadcastReceiver cancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "User Cancelled");
            dismissProgressDialog();
        }
    };
}
