package com.healthcare.aarogyanidaan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class NotificationForegroundService extends Service {

    private static final String TAG = "NotificationForegroundService";
    private static final String FOREGROUND_CHANNEL_ID = "foreground_service_channel";
    private static final int NOTIFICATION_ID = 12345;

    private LocalNotificationService notificationService;
    private final Handler handler = new Handler();
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Foreground service created");

        //Initialize notification service
        notificationService = LocalNotificationService.getInstance(this);

        // Create notification channel
        createForegroundNotificationChannel();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting foreground service");

        if (isServiceRunning) {
            return START_STICKY;
        }

        //  Create a notification with MainActivity intent
        Intent mainIntent = new Intent(this, splashpage.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);

        //  Start foreground immediately with a notification
        startForeground(NOTIFICATION_ID, createNotification(pendingIntent));

        isServiceRunning = true;

        //  Start heavy work in a separate thread
        handler.post(() -> {
            Log.d(TAG, "Initializing notification service...");
            notificationService.checkAuthStatusAndReInitialize();
        });

        return START_STICKY;
    }

    private Notification createNotification(PendingIntent pendingIntent) {
        return new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setContentTitle("Aarogya Nidaan is Active")
                .setContentText("Monitoring health data and notifications")
                .setSmallIcon(R.drawable.aarogyanidaanlogo)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.aarogyanidaanlogo))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Keeps it high-priority
//                .setContentIntent(pendingIntent) // Opens the app when tapped
                .setAutoCancel(false) // Keeps the notification visible until dismissed manually
                .setOngoing(true) // Makes it a foreground notification (non-dismissible)
                .setColor(ContextCompat.getColor(this, R.color.red)) // Use app's theme color
                .setCategory(NotificationCompat.CATEGORY_SERVICE) // Categorize it as a service
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show even on lock screen
//                .addAction(R.drawable.passwordicon, "Stop Monitoring", getStopPendingIntent()) // Add action to stop service
                .build();
    }

//    private PendingIntent getStopPendingIntent() {
//        Intent stopIntent = new Intent(this, splashpage.class);
//        stopIntent.setAction("STOP_SERVICE");
//        return PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//    }


    private void createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_HIGH //  High importance to avoid being killed
            );
            channel.setDescription("Channel for keeping notification service alive");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.setVibrationPattern(new long[]{0});
            channel.enableVibration(false);
            channel.setShowBadge(false);
            channel.setSound(null, null); //  No sound for silent notification

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Foreground service destroyed");

        isServiceRunning = false;

        // Restart service if the user is logged in
        if (notificationService.isUserLoggedIn()) {
            Log.d(TAG, "Restarting service after destruction...");
            restartService();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Service task removed - app swiped away");

        //Restart service if the user is logged in
        if (notificationService.isUserLoggedIn()) {
            Log.d(TAG, "Restarting service after task removal...");
            restartService();
        }

        super.onTaskRemoved(rootIntent);
    }

    private void restartService() {
        Intent restartServiceIntent = new Intent(getApplicationContext(), NotificationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartServiceIntent);
        } else {
            startService(restartServiceIntent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
