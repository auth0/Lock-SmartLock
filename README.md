Lock + SmartLock
============
[![CI Status](http://img.shields.io/travis/auth0/Lock-SmartLock.svg?style=flat)](https://travis-ci.org/auth0/Lock-SmartLock)
[![License](http://img.shields.io/:license-mit-blue.svg?style=flat)](http://doge.mit-license.org)
[![Maven Central](https://img.shields.io/maven-central/v/com.auth0.android/lock-smartlock.svg)](http://search.maven.org/#artifactdetails%7Ccom.auth0.android%7Clock%7C1.8.0%7Caar)
[ ![Download](https://api.bintray.com/packages/auth0/lock-android/lock-smartlock/images/download.svg) ](https://bintray.com/auth0/lock-android/lock-smartlock/_latestVersion)

[Auth0](https://auth0.com) is an authentication broker that supports social identity providers as well as enterprise identity providers such as Active Directory, LDAP, Google Apps and Salesforce.

Auth0 Lock integration with Google Smart Lock for Android

## Requierements

Android API level 14+ and Google Play Services `7.5.+`

##Install

Lock + SmartLock is available both in [Maven Central](http://search.maven.org) and [JCenter](https://bintray.com/bintray/jcenter). To start using it add this line to your `build.gradle` dependencies file:

```gradle
compile 'com.auth0.android:lock-smartlock:0.1.+'
```

Next we're going to configure Auth0 Lock & Google's Smart Lock

###Auth0 Lock

Once it's installed, you'll need to configure LockActivity in your`AndroidManifest.xml`, inside the `application` tag:

```xml
<!--Auth0 Lock-->
<activity
  android:name="com.auth0.lock.LockActivity"
  android:theme="@style/Lock.Theme"
  android:screenOrientation="portrait"
  android:launchMode="singleTask">
  <intent-filter>
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="a0INSERT_YOUR_APP_CLIENT_ID" android:host="@string/auth0_domain"/>
  </intent-filter>
</activity>
<meta-data android:name="com.auth0.lock.client-id" android:value="@string/auth0_client_id"/>
<meta-data android:name="com.auth0.lock.domain-url" android:value="@string/auth0_domain"/>
<!--Auth0 Lock End-->
```

> The value `@string/auth0_client_id` is your application's clientID and `@string/auth0_domain` is your tenant's domain in Auth0, both values can be found in your app's settings.
> The final value of `android:scheme` must be in lowercase

Also you'll need to add the following permission:
```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

Finally, Make your Application class implement the interface `com.auth0.lock.LockProvider` and create a `SmartLock` instance:

```java
public class MyApplication extends Application implements LockProvider {

    private SmartLock lock;

    public void onCreate() {
        super.onCreate();
        lock = new SmartLock.Builder(this)
            .loadFromApplication(this)
            /** Other configuration goes here */
            .closable(true)
            .build();
    }

    @Override
    public Lock getLock() {
        return lock;
    }
}
```

###Smart Lock configuration

To enable your application to access Smart Lock, you need to register your application in [Google Developers Console](https://console.developers.google.com/) by creating a project and registering your keystore SHA1 fingerprint.
> We recoomend following Google's [Getting Started Guide](https://developers.google.com/identity/smartlock-passwords/android/get-started)

Once you have registered your app just add this to your android manifest:

```xml
<meta-data android:name="com.google.android.gms.version" android:value="@integer/google_play_services_version" />
```

## Usage

From any Activity just override the following methods that will allow `Lock` to interact with `Smart Lock`

```java
@Override
protected void onStart() {
    super.onStart();
    SmartLock.getSmartLock(this).onStart();
}

@Override
protected void onStop() {
    super.onStop();
    SmartLock.getSmartLock(this).onStop();
}

@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    SmartLock.getSmartLock(this).onActivityResult(this, requestCode, resultCode, data);
}
```

Then register the following `BroadcastReceiver` to be notified when the user is authenticated

```java
private BroadcastReceiver authenticationReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        UserProfile profile = intent.getParcelableExtra("profile");
        Token token = intent.getParcelableExtra("token");
        // Handle user's credentials
    }
};

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //.....
    broadcastManager = LocalBroadcastManager.getInstance(this);
    broadcastManager.registerReceiver(authenticationReceiver, new IntentFilter(Lock.AUTHENTICATION_ACTION));
}

@Override
protected void onDestroy() {
    super.onDestroy();
    broadcastManager.unregisterReceiver(authenticationReceiver);
}
```

And to login, just call this method

```java
SmartLock.getSmartLock(this).loginFromActivity(this);
```

## Issue Reporting

If you have found a bug or if you have a feature request, please report them at this repository issues section. Please do not report security vulnerabilities on the public GitHub issue tracker. The [Responsible Disclosure Program](https://auth0.com/whitehat) details the procedure for disclosing security issues.

## What is Auth0?

Auth0 helps you to:

* Add authentication with [multiple authentication sources](https://docs.auth0.com/identityproviders), either social like **Google, Facebook, Microsoft Account, LinkedIn, GitHub, Twitter, Box, Salesforce, amont others**, or enterprise identity systems like **Windows Azure AD, Google Apps, Active Directory, ADFS or any SAML Identity Provider**.
* Add authentication through more traditional **[username/password databases](https://docs.auth0.com/mysql-connection-tutorial)**.
* Add support for **[linking different user accounts](https://docs.auth0.com/link-accounts)** with the same user.
* Support for generating signed [Json Web Tokens](https://docs.auth0.com/jwt) to call your APIs and **flow the user identity** securely.
* Analytics of how, when and where users are logging in.
* Pull data from other sources and add it to the user profile, through [JavaScript rules](https://docs.auth0.com/rules).

## Create a free account in Auth0

1. Go to [Auth0](https://auth0.com) and click Sign Up.
2. Use Google, GitHub or Microsoft Account to login.

## Author

Auth0

## License

Lock is available under the MIT license. See the [LICENSE file](LICENSE) for more info.
