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

string.xml
```xml
<resources>
    <string name="app_name">MyAndroidGcm</string>

    <string name="project_number">000</string>
</resources>
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


```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.samplesubscriber"
    android:versionCode="1"
    android:versionName="1.0" >
 
    <uses-sdk
        android:minSdkVersion="8"
        android:targetSdkVersion="18" />
     
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
 
    <permission
        android:name="com.example.samplesubscriber.permission.C2D_MESSAGE"
        android:protectionLevel="signature" />
 
    <uses-permission android:name="com.example.samplesubscriber.permission.C2D_MESSAGE" />
 
    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme" >
        <activity
            android:name="com.example.samplesubscriber.MainActivity"
            android:label="@string/app_name" >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <receiver
            android:name=".GcmBroadcastReceiver"
            android:permission="com.google.android.c2dm.permission.SEND" >
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                <category android:name="com.example.samplesubscriber" />
            </intent-filter>
        </receiver>
        <service android:name=".GcmIntentService" />
    </application>
 
</manifest>





```

```java

package com.example.samplesubscriber;
 
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
 
import com.google.android.gms.gcm.GoogleCloudMessaging;
 
public class GcmIntentService extends IntentService {
 
    private static final String TAG = "GcmIntentService";
 
    public GcmIntentService() {
        super("GcmIntentService");
    }
 
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
 
        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                Log.d(TAG, "messageType: " + messageType + ",body:" + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                Log.d(TAG, "messageType: " + messageType + ",body:" + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Log.d(TAG, "messageType: " + messageType + ",body:" + extras.toString());
                sendNotification(extras.getString("default"));
            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }
 
    private void sendNotification(String message) {
        NotificationManager manager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
 
        PendingIntent contentIntent = 
                PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
 
        NotificationCompat.Builder mBuilder = 
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("GCM Notification")
        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
        .setContentText(message);
 
        mBuilder.setContentIntent(contentIntent);
        manager.notify(0, mBuilder.build());
    }
}


package com.example.samplesubscriber;
 
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
 
public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName comp = 
          new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}


package com.example.samplesubscriber;
 
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
 
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
 
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
 
import com.google.android.gms.gcm.GoogleCloudMessaging;
 
public class MainActivity extends Activity {
     
    private static final String TAG = MainActivity.class.getSimpleName();
 
    private GoogleCloudMessaging mGcm;
    private Context mContext;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
 
        mGcm = GoogleCloudMessaging.getInstance(this);
        registerInBackground();
    }
 
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String registrationId = "";
                try {
                    if (mGcm == null) {
                        mGcm = GoogleCloudMessaging.getInstance(mContext);
                    }
                    registrationId = mGcm.register("YOUR_SENDER_ID");
                    Log.d(TAG, "Device registered, registration ID=" + registrationId);
                } catch (IOException ex) {
                    Log.e(TAG, "Error :" + ex.getMessage());
                }
                return registrationId;
            }
 
            @Override
            protected void onPostExecute(String msg) {
            }
        }.execute(null, null, null);
    }
}


```
