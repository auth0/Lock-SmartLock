package com.auth0.lock.smartlock.app;

import android.app.Application;

import com.auth0.lock.Lock;
import com.auth0.lock.LockBuilder;
import com.auth0.lock.LockProvider;
import com.auth0.lock.smartlock.SmartLock;

public class SmartLockApplication extends Application implements LockProvider {

    private SmartLock smartLock;
    private Lock lock;

    @Override
    public void onCreate() {
        super.onCreate();

        smartLock = new SmartLock(this);
        lock = new LockBuilder()
                .loadFromApplication(this)
                .closable(true)
                .useCredentialStore(smartLock)
                .build();
    }

    @Override
    public Lock getLock() {
        return lock;
    }

    public SmartLock getSmartLock() {
        return smartLock;
    }
}
