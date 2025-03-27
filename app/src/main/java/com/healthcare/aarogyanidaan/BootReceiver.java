package com.healthcare.aarogyanidaan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Wait a bit before rescheduling (system might still be initializing)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Start the notification scheduler activity to reschedule notifications
                Intent schedulerIntent = new Intent(context, NotificationSchedulerActivity.class);
                schedulerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(schedulerIntent);
            }, 60000); // Wait 1 minute after boot
        }
    }
}