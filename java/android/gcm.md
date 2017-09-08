build.gradle
```
    repositories {
        jcenter()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
    
    dependencies {
        compile 'com.google.android.gms:play-services:8.4.0'
        compile 'com.github.raxden:AndroidGCM:v2.2.0'
    }
```

AndroidManifest.xml
```xml
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
    <uses-permission android:name="com.forsrc.myandroidgcm.permission.C2D_MESSAGE" />
    <permission android:name="com.forsrc.myandroidgcm.permission.C2D_MESSAGE"

     <meta-data android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version" />

        <receiver android:name="com.raxdenstudios.gcm.receiver.GCMBroadcastReceiver"
            android:permission="com.google.android.c2dm.permission.SEND" >
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
                <action android:name="com.google.android.gcm.intent.RETRY" />
                <category android:name="com.forsrc.myandroidgcm" />
            </intent-filter>
        </receiver>

        <receiver android:name="com.raxdenstudios.gcm.receiver.GCMSendNotificationReceiver">
            <intent-filter android:priority="1">
                <action android:name="com.forsrc.myandroidgcm.gcm"></action>
            </intent-filter>
        </receiver>

        <service android:name="com.raxdenstudios.gcm.service.GCMIntentService" />
```

activity_main.xml
```xml
<TextView android:id="@+id/textView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello World!"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
```


```java
package com.forsrc.myandroidgcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.raxdenstudios.gcm.GCMHelper;

public class MainActivity extends AppCompatActivity {

    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        textView.append("\n------------------\n");
        GCMHelper.getInstance().registerPlayServices(this,
                getString(R.string.project_number),
                new GCMHelper.OnGCMRegisterListener() {
            @Override
            public void onGooglePlayServicesNotSupported() {
                Toast.makeText(getApplicationContext(), "GooglePlayServicesNotSupported",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceRegistered(String registrationId) {
                Toast.makeText(getApplicationContext(), registrationId,
                        Toast.LENGTH_SHORT).show();
                textView.append("registrationId: " + registrationId + "\n");
                textView.append("------------------\n");
            }

            @Override
            public void onDeviceNotRegistered(String message) {
                Toast.makeText(getApplicationContext(), message,
                        Toast.LENGTH_SHORT).show();
                textView.append("message: " + message + "\n");
                textView.append("------------------\n");
                sendNotification(message);
            }
        });
    }

    private void sendNotification(String message) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent =
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.sns_gcm_icon)
                        .setContentTitle("SNS GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        manager.notify(0, mBuilder.build());
    }
}
```



