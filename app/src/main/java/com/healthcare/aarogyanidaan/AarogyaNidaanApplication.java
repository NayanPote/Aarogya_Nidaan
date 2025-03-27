package com.healthcare.aarogyanidaan;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class AarogyaNidaanApplication extends Application {
    private static final String TAG = "AarogyaNidaanApp";
    private LocalNotificationService notificationService;
    private FirebaseAuthListener authListener;
    private FirebaseAuth.AuthStateListener serviceStartListener;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application starting");

        // Enable Firebase persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Initialize the notification service
        notificationService = LocalNotificationService.getInstance(this);

        // Register auth state listener
        authListener = new FirebaseAuthListener(this);
        authListener.register();

        // Add listener specifically to start/stop the foreground service
        serviceStartListener = firebaseAuth -> {
            if (firebaseAuth.getCurrentUser() != null) {
                // Start the foreground service when user logs in
                startNotificationService();
            } else {
                // Stop the service when user logs out
                stopNotificationService();
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(serviceStartListener);

        // Check if we should start service immediately (for app restart scenarios)
        checkAndStartService();

        Log.d(TAG, "Application started successfully");
    }

    private void checkAndStartService() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("userLoggedIn", false);
        String userId = prefs.getString("currentUserId", null);

        if (FirebaseAuth.getInstance().getCurrentUser() != null || (isLoggedIn && userId != null)) {
            startNotificationService();
        }
    }

    private void startNotificationService() {
        Intent serviceIntent = new Intent(this, NotificationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d(TAG, "Notification service started");
    }

    private void stopNotificationService() {
        //this line ensure LocalNotificationService is cleaned up or not
        LocalNotificationService.getInstance(this).cleanup();

        // Remove any remaining notification
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        stopService(new Intent(this, NotificationForegroundService.class));
        Log.d(TAG, "Notification service stopped");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Clean up resources
        if (authListener != null) {
            authListener.unregister();
        }

        if (serviceStartListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(serviceStartListener);
        }

        Log.d(TAG, "Application terminated");
    }
}