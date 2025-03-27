package com.healthcare.aarogyanidaan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";
    private static final String PREFS_NAME = "ChatPrefs";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                        intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
                        intent.getAction().equals("com.htc.intent.action.QUICKBOOT_POWERON"))) {

            Log.d(TAG, "Device boot completed, checking login status");

            // Delay the startup to ensure the system is fully booted
            new Thread(() -> {
                try {
                    // Wait for 10 seconds to let the system stabilize
                    Thread.sleep(10000);

                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    boolean isLoggedIn = prefs.getBoolean("userLoggedIn", false);
                    String userId = prefs.getString("currentUserId", null);

                    if (isLoggedIn && userId != null) {
                        Log.d(TAG, "User is logged in, starting notification service for user: " + userId);

                        // Start the foreground service
                        Intent serviceIntent = new Intent(context, NotificationForegroundService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent);
                        } else {
                            context.startService(serviceIntent);
                        }
                    } else {
                        Log.d(TAG, "No logged in user found, not starting service");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error in boot delay", e);
                }
            }).start();
        }
    }
}