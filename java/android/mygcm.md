```
    repositories {
        jcenter()
        mavenCentral()
        maven { url "https://jitpack.io" }
    }
```

```
dependencies {
    compile 'com.google.android.gms:play-services:8.4.0'
    compile 'com.github.raxden:AndroidGCM:v2.2.0'
    }
```
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.forsrc.myandroidgcm">

    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
    <uses-permission android:name="com.forsrc.myandroidgcm.permission.C2D_MESSAGE" />
    <permission android:name="com.forsrc.myandroidgcm.permission.C2D_MESSAGE"
        android:protectionLevel="signature" />


    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name=".MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>


        <meta-data android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version" />

        <receiver android:name="com.forsrc.myandroidgcm.GcmWakefulBroadcastReceiver"
            android:permission="com.google.android.c2dm.permission.SEND" >
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                <action android:name="com.google.android.c2dm.intent.REGISTRATION" />
                <action android:name="com.google.android.gcm.intent.RETRY" />
                <category android:name="com.forsrc.myandroidgcm" />
            </intent-filter>
        </receiver>

        <receiver android:name="com.forsrc.myandroidgcm.GcmBroadcastReceiver">
            <intent-filter android:priority="1">
                <action android:name="com.forsrc.myandroidgcm.gcm"></action>
            </intent-filter>
        </receiver>


        <service android:name="com.forsrc.myandroidgcm.GcmIntentService" />


    </application>

</manifest>



<resources>
    <string name="app_name">MyAndroidGcm</string>

    <string name="project_number">105824148373</string>
</resources>

```

```java
package com.forsrc.myandroidgcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.raxdenstudios.gcm.GCMHelper;

public class MainActivity extends AppCompatActivity {

    private Handler handler = new Handler();
    TextView textView;

    public static final String BR_TEXT = "update_textview_action";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        textView.setTextIsSelectable(true);
        textView.append("\n------------------\n");

        final Context context = this;
        GCMHelper.getInstance().registerPlayServices(this,
                getString(R.string.project_number),
                new GCMHelper.OnGCMRegisterListener() {
            @Override
            public void onGooglePlayServicesNotSupported() {
                Toast.makeText(getApplicationContext(), "GooglePlayServicesNotSupported",
                        Toast.LENGTH_SHORT).show();
                textView.append("--> GooglePlayServicesNotSupported\n");
                textView.append("------------------\n");
            }

            @Override
            public void onDeviceRegistered(String registrationId) {
                Toast.makeText(getApplicationContext(), registrationId,
                        Toast.LENGTH_SHORT).show();
                textView.append("registrationId: " + registrationId + "\n");
                textView.append("------------------\n");
                GcmUtils.sendNotification(context, "registrationId: " + registrationId);
            }

            @Override
            public void onDeviceNotRegistered(String message) {
                Toast.makeText(getApplicationContext(), message,
                        Toast.LENGTH_SHORT).show();
                textView.append("NotRegistered: " + message + "\n");
                textView.append("------------------\n");
                GcmUtils.sendNotification(context, "NotRegistered: " + message);
            }
        });

        updateTextView();

        GcmUtils.broadcast(context, BR_TEXT, "message", "hello world, broadcast.");
    }

    BroadcastReceiver broadcastReceiver = null;
    private void updateTextView() {
         broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getExtras().getString("message");
                textView.append("message: " + message + "\n");
                textView.append("------------------\n");

            }
        };


        GcmUtils.registerReceiver(this, broadcastReceiver, BR_TEXT);
    }

    protected void onDestroy() {
        GcmUtils.unregisterReceiver(this, broadcastReceiver);
        super.onDestroy();
    };
    
    public Handler getHandler() {
        return handler;
    }
    
    public void updateTextView(String message) {
        textView.append("message: " + message + "\n");
                textView.append("------------------\n");
    }
    public static void updateTextView(MainActivity mainActivity, final String message) {
        if (mainActivity != null) {
            mainActivity.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.updateTextView(message);
                }
            });
        }
    }
}



package com.forsrc.myandroidgcm;


import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;

public class GcmUtils {

    public static void sendNotification(Context context, String message) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent =
                PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ttl_osusume_icon)
                        .setContentTitle("SNS GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        manager.notify(0, mBuilder.build());
    }

    public static void registerReceiver(Context context, BroadcastReceiver broadcastReceiver, String name){
        IntentFilter filter = new IntentFilter(name);
        context.registerReceiver(broadcastReceiver, filter);
    }

    public static void broadcast(Context context, String name, String key, String value){
        Intent intent = new Intent(name);
        intent.putExtra(key, value);
        context.sendBroadcast(intent);
        /*
        boolean alarmRunning = (PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE) != null);
        if(alarmRunning == false) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            //alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), 1000, pendingIntent);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 100, pendingIntent);
        }
        */
    }

    public static void unregisterReceiver(Context context, BroadcastReceiver broadcastReceiver){
        context.unregisterReceiver(broadcastReceiver);
    }
}


package com.forsrc.myandroidgcm;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.raxdenstudios.gcm.GCMHelper;
import com.raxdenstudios.gcm.model.GCMessage;



public class GcmBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = GcmBroadcastReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Log.d(TAG, "[onReceive] handling notification with extras: " + extras != null?extras.toString():"empty");
        if(extras != null && extras.containsKey(GCMessage.class.getSimpleName())) {
            GCMessage message = (GCMessage)extras.getParcelable(GCMessage.class.getSimpleName());
            if(message != null && GCMHelper.getInstance().consumeGCMMessage(context, message.getId()) != null) {
                int defaults = -1;
                if(message.isSound()) {
                    defaults |= 1;
                }

                if(message.isVibrate()) {
                    defaults |= 2;
                }

                Log.d(TAG, "[onReceive] notification with id " + message.getId() + " sended!");
                // NotificationUtils.sendNotification(context, extras, message.getId(), com.raxdenstudios.gcm.R.drawable.ic_notification, message.getTitle(), message.getSubtitle(), message.getMessage(), message.getTicker(), defaults);
                Toast.makeText(context, "message: " + message.getMessage(), Toast.LENGTH_SHORT).show();
                GcmUtils.sendNotification(context, message.getMessage());

                GcmUtils.broadcast(context, MainActivity.BR_TEXT, "message", message.getMessage());
            }
        }

    }


}


package com.forsrc.myandroidgcm;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.raxdenstudios.commons.util.Utils;
import com.raxdenstudios.gcm.GCMHelper;
import com.raxdenstudios.gcm.model.GCMessage;




public class GcmIntentService  extends IntentService {
    private static final String TAG = GcmIntentService.class.getSimpleName();

    public GcmIntentService() {
        super(GcmIntentService.class.getSimpleName());
    }

    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "[onHandleIntent]");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        Log.d(TAG, "[onHandleIntent] messageType received: " + messageType);
        if(!extras.isEmpty()) {
            Log.d(TAG, "[onHandleIntent] extras received: " + extras.toString());
            if("send_error".equals(messageType)) {
                this.sendNotification("send_error", extras);
            } else if("deleted_messages".equals(messageType)) {
                this.sendNotification("deleted_messages", extras);
            } else if("gcm".equals(messageType)) {
                this.sendNotification("gcm", extras);
            }
        } else {
            Log.d(TAG, "[onHandleIntent] extras is empty!");
        }

        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    protected void sendNotification(String action, Bundle extras) {
        if("gcm".equals(action)) {
            GCMessage broadcastIntent = this.parseGCMessage(extras);
            if(broadcastIntent != null) {
                GCMHelper.getInstance().addGCMessage(this, broadcastIntent);
                extras.putParcelable(GCMessage.class.getSimpleName(), broadcastIntent);
            }
        }

        Log.d(TAG, "[sendNotification] sending notification to " + action + " with extras: " + extras.toString());
        Intent broadcastIntent1 = new Intent();
        broadcastIntent1.setAction(Utils.getPackageName(this.getApplicationContext()) + "." + action);
        broadcastIntent1.putExtras(extras);
        this.sendOrderedBroadcast(broadcastIntent1, (String)null);
    }

    protected GCMessage parseGCMessage(Bundle extras) {
        return GCMHelper.getInstance().buildGCMessage(this, extras);
    }
}

package com.forsrc.myandroidgcm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;



public class GcmWakefulBroadcastReceiver extends WakefulBroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        ComponentName comp = new ComponentName(context.getPackageName(), GcmIntentService.class.getName());
        startWakefulService(context, intent.setComponent(comp));
        this.setResultCode(-1);
    }
}


```

