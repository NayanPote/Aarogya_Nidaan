package com.healthcare.aarogyanidaan;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class NotificationSchedulerActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "health_reminder_channel";
    private static final String CHANNEL_NAME = "Health Reminders";
    private static final String CHANNEL_DESC = "Daily health reminders from Aarogya Nidaan";

    private static final String PREFS_NAME = "AarogyaNidaanPrefs";
    private static final String PREF_NOTIF_ENABLED = "notifications_enabled";
    private static final String PREF_START_HOUR = "start_hour";
    private static final String PREF_END_HOUR = "end_hour";

    private Switch notificationSwitch;
    private TimePicker startTimePicker;
    private TimePicker endTimePicker;
    private Button saveButton;
    private Button testButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_scheduler);

        // Initialize UI elements
        initializeUI();

        // Create notification channel (required for Android 8.0 and above)
        createNotificationChannel();

        // Check notification permission for Android 13+
        checkNotificationPermission();

        // Load saved preferences
        loadPreferences();

        // Set click listeners
        setupClickListeners();
    }

    private void initializeUI() {
        notificationSwitch = findViewById(R.id.switch_notifications);
        startTimePicker = findViewById(R.id.time_picker_start);
        endTimePicker = findViewById(R.id.time_picker_end);
        saveButton = findViewById(R.id.button_save);
        testButton = findViewById(R.id.button_test);

        // Set 24-hour format for better UX
        startTimePicker.setIs24HourView(true);
        endTimePicker.setIs24HourView(true);
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(PREF_NOTIF_ENABLED, true);
        int startHour = prefs.getInt(PREF_START_HOUR, 6);
        int endHour = prefs.getInt(PREF_END_HOUR, 22);

        notificationSwitch.setChecked(notificationsEnabled);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startTimePicker.setHour(startHour);
            startTimePicker.setMinute(0);
            endTimePicker.setHour(endHour);
            endTimePicker.setMinute(0);
        } else {
            startTimePicker.setCurrentHour(startHour);
            startTimePicker.setCurrentMinute(0);
            endTimePicker.setCurrentHour(endHour);
            endTimePicker.setCurrentMinute(0);
        }
    }

    private void setupClickListeners() {
        saveButton.setOnClickListener(v -> {
            savePreferences();
            scheduleNotifications();
            Toast.makeText(this, "Preferences saved and notifications scheduled", Toast.LENGTH_SHORT).show();
        });

        testButton.setOnClickListener(v -> {
            testNotification();
        });
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        boolean notificationsEnabled = notificationSwitch.isChecked();
        int startHour, endHour;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startHour = startTimePicker.getHour();
            endHour = endTimePicker.getHour();
        } else {
            startHour = startTimePicker.getCurrentHour();
            endHour = endTimePicker.getCurrentHour();
        }

        editor.putBoolean(PREF_NOTIF_ENABLED, notificationsEnabled);
        editor.putInt(PREF_START_HOUR, startHour);
        editor.putInt(PREF_END_HOUR, endHour);
        editor.apply();
    }

    private void checkNotificationPermission() {
        // For Android 13+, check if notification permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                Toast.makeText(this, "Please enable notifications for Aarogya Nidaan", Toast.LENGTH_LONG).show();
                startActivity(intent);
            }
        }
        // For Android 10-12, use the notification compatibility check
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Toast.makeText(this, "Please enable notifications for Aarogya Nidaan", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            channel.setShowBadge(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void scheduleNotifications() {
        // Cancel any existing notifications first
        cancelExistingNotifications();

        // If notifications are disabled, don't schedule new ones
        if (!notificationSwitch.isChecked()) {
            return;
        }

        // Get user-defined start and end hours
        int startHour, endHour;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startHour = startTimePicker.getHour();
            endHour = endTimePicker.getHour();
        } else {
            startHour = startTimePicker.getCurrentHour();
            endHour = endTimePicker.getCurrentHour();
        }

        // Map of hour -> list of notifications for that hour
        Map<Integer, List<String>> healthNotifications = getHealthNotifications();

        // Filter notifications based on user preferences
        Map<Integer, List<String>> filteredNotifications = new HashMap<>();
        for (Map.Entry<Integer, List<String>> entry : healthNotifications.entrySet()) {
            int hour = entry.getKey();
            if (isHourInRange(hour, startHour, endHour)) {
                filteredNotifications.put(hour, entry.getValue());
            }
        }

        // Schedule all notifications
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        int notificationId = 1;

        for (Map.Entry<Integer, List<String>> entry : filteredNotifications.entrySet()) {
            int hour = entry.getKey();
            List<String> notifications = entry.getValue();

            for (int i = 0; i < notifications.size(); i++) {
                String notificationText = notifications.get(i);

                // Schedule notification
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, 15 * i); // Spread notifications 15 minutes apart
                calendar.set(Calendar.SECOND, 0);

                // If time has already passed today, schedule for tomorrow
                if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                Intent intent = new Intent(this, NotificationReceiver.class);
                intent.putExtra("notification_id", notificationId);
                intent.putExtra("notification_text", notificationText);

                // Use appropriate flag based on Android version
                int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT;

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        notificationId,
                        intent,
                        flags
                );

                // Schedule repeating alarm with appropriate method based on Android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                    // For Android 12+, check if we can schedule exact alarms
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );

                    // Schedule again for tomorrow to make it "repeating" without using deprecated methods
                    PendingIntent tomorrowIntent = PendingIntent.getBroadcast(
                            this,
                            notificationId + 10000, // Different request code to avoid overriding
                            intent,
                            flags
                    );

                    Calendar tomorrowCalendar = (Calendar) calendar.clone();
                    tomorrowCalendar.add(Calendar.DAY_OF_YEAR, 1);

                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            tomorrowCalendar.getTimeInMillis(),
                            tomorrowIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // For Android 6-11
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    // For earlier versions, using setRepeating is more reliable
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                    );
                }

                notificationId++;
            }
        }

        // Schedule a daily alarm to reset all notifications (ensures they continue to work)
        scheduleNotificationReset();
    }

    private boolean isHourInRange(int hour, int startHour, int endHour) {
        if (startHour <= endHour) {
            return hour >= startHour && hour <= endHour;
        } else {
            // Handle case where range crosses midnight (e.g., 22-6)
            return hour >= startHour || hour <= endHour;
        }
    }

    private void scheduleNotificationReset() {
        // Set up an alarm that triggers at midnight to reschedule all notifications
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.add(Calendar.DAY_OF_YEAR, 1);

        Intent resetIntent = new Intent(this, NotificationResetReceiver.class);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent resetPendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                resetIntent,
                flags
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    midnight.getTimeInMillis(),
                    resetPendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    midnight.getTimeInMillis(),
                    resetPendingIntent
            );
        }
    }

    private void cancelExistingNotifications() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Cancel up to 100 notifications (adjust if needed)
        for (int i = 1; i <= 100; i++) {
            Intent intent = new Intent(this, NotificationReceiver.class);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    i,
                    intent,
                    flags
            );

            alarmManager.cancel(pendingIntent);

            // Also cancel the potential "tomorrow" intent for Android 12+
            PendingIntent tomorrowIntent = PendingIntent.getBroadcast(
                    this,
                    i + 10000,
                    intent,
                    flags
            );
            alarmManager.cancel(tomorrowIntent);
        }
    }

    private Map<Integer, List<String>> getHealthNotifications() {
        Map<Integer, List<String>> healthNotifications = new HashMap<>();

        // Early Morning (6-8 AM)
        List<String> earlyMorningNotifications = new ArrayList<>();
        earlyMorningNotifications.add("🌅 Time for yoga! Just 10 mins — you got this! 😎");
        earlyMorningNotifications.add("💧 Sip some warm water — your body will thank you! 😉");
        earlyMorningNotifications.add("🩸 BP check? Quick and easy — stay on top of it! 💪");
        earlyMorningNotifications.add("☀️ Morning sunshine! Let's stretch for 2 minutes! 🙆‍♀️");
        healthNotifications.put(6, earlyMorningNotifications);

        // Breakfast time (8-10 AM)
        List<String> breakfastNotifications = new ArrayList<>();
        breakfastNotifications.add("🍳 Don't skip breakfast — fuel up for the day! 🚀");
        breakfastNotifications.add("💪 Add some protein — power up your morning! 🥚");
        breakfastNotifications.add("💊 Morning meds? Take them with food! 🍎");
        breakfastNotifications.add("🥣 Oats are heart-friendly! Add some nuts and berries! ❤️");
        healthNotifications.put(8, breakfastNotifications);

        // Mid-morning (10 AM - 12 PM)
        List<String> midMorningNotifications = new ArrayList<>();
        midMorningNotifications.add("🚶‍♂️ 5-min walk? Stretch those legs! 🦵");
        midMorningNotifications.add("💧 Hydration check! Time for a water break. 💦");
        midMorningNotifications.add("🧘‍♀️ Deep breath — chill mode ON. 😌");
        midMorningNotifications.add("👀 Eye strain? Try the 20-20-20 rule! Look away for 20s. 👁️");
        healthNotifications.put(10, midMorningNotifications);

        // Lunch time (12-2 PM)
        List<String> lunchNotifications = new ArrayList<>();
        lunchNotifications.add("🥗 Veggies = happy tummy. Don't skip 'em! 🌽");
        lunchNotifications.add("🍴 Slow down — taste your food! 😋");
        lunchNotifications.add("🚶‍♀️ Post-lunch stroll? Feels good, right? 🌞");
        lunchNotifications.add("🧠 Take a mental break with your meal. Full focus on food! 🍲");
        healthNotifications.put(12, lunchNotifications);

        // Afternoon (2-4 PM)
        List<String> afternoonNotifications = new ArrayList<>();
        afternoonNotifications.add("😴 Afternoon slump? Stretch it out! 🧘‍♂️");
        afternoonNotifications.add("💧 Water time! Your body's asking for it. 🌊");
        afternoonNotifications.add("🪑 Fix your posture — no slouching! 😎");
        afternoonNotifications.add("🍊 Craving sugar? Grab a juicy orange instead! 🍊");
        healthNotifications.put(14, afternoonNotifications);

        // Evening (4-6 PM)
        List<String> eveningNotifications = new ArrayList<>();
        eveningNotifications.add("🚶‍♀️ Evening walk? Let's gooo! 🏃‍♂️");
        eveningNotifications.add("🍎 Grab a fruit — sweet and healthy! 🍌");
        eveningNotifications.add("💊 Evening meds? Quick reminder! ✅");
        eveningNotifications.add("💆‍♀️ Time for a quick 2-min head massage! So relaxing! 😌");
        healthNotifications.put(16, eveningNotifications);

        // Dinner time (6-8 PM)
        List<String> dinnerNotifications = new ArrayList<>();
        dinnerNotifications.add("🍽️ Light and tasty — that's the dinner vibe! 😋");
        dinnerNotifications.add("⏰ Dinner 2-3 hours before bed = happy sleep! 😴");
        dinnerNotifications.add("🥦 Balanced meal = happy you! 🥗");
        dinnerNotifications.add("🧂 Less salt = better heart health! Just a pinch will do! ❤️");
        healthNotifications.put(18, dinnerNotifications);

        // Night time (8-10 PM)
        List<String> nightNotifications = new ArrayList<>();
        nightNotifications.add("📵 Screen time off — bedtime vibes! 🌙");
        nightNotifications.add("📖 Chill out with a book or stretch! 🧘‍♂️");
        nightNotifications.add("👜 Set out your essentials for tomorrow! ✅");
        nightNotifications.add("🧠 Journal for 5 mins! Clear that busy mind. ✍️");
        healthNotifications.put(20, nightNotifications);

        // Late night (10-11 PM)
        List<String> lateNightNotifications = new ArrayList<>();
        lateNightNotifications.add("🛌 Sleep time! Your body needs it. 😴");
        lateNightNotifications.add("🦷 Brush + floss = happy smile! 😁");
        lateNightNotifications.add("🙏 Gratitude time — reflect and relax. ❤️");
        lateNightNotifications.add("💤 Cool room = better sleep. Optimal is 18-20°C! 🌡️");
        healthNotifications.put(22, lateNightNotifications);

        return healthNotifications;
    }

    private void testNotification() {
        // Get a random notification to test
        Map<Integer, List<String>> notifications = getHealthNotifications();
        List<Integer> hours = new ArrayList<>(notifications.keySet());
        int randomHour = hours.get(new Random().nextInt(hours.size()));
        List<String> hourNotifications = notifications.get(randomHour);
        String randomNotification = hourNotifications.get(new Random().nextInt(hourNotifications.size()));

        // Get a random title for the notification
        String[] titles = {
                "Health Reminder",
                "Aarogya Nidaan",
                "Wellness Check",
                "Health Tip",
                "Daily Reminder",
                "Stay Healthy!"
        };
        String title = titles[new Random().nextInt(titles.length)];

        // Build the notification
        Drawable drawable;
        try {
            drawable = AppCompatResources.getDrawable(this, R.drawable.chatbot);
        } catch (Exception e) {
            // If chatbot drawable isn't found, use a fallback
            drawable = AppCompatResources.getDrawable(this, android.R.drawable.ic_dialog_info);
        }

        Bitmap largeIcon = null;
        if (drawable != null) {
            largeIcon = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(largeIcon);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        // Get small icon
        int smallIconResource;
        try {
            smallIconResource = R.drawable.aarogyanidaanlogo;
        } catch (Exception e) {
            // Fallback to system icon if app icon not found
            smallIconResource = android.R.drawable.ic_dialog_info;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(smallIconResource)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(randomNotification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(getResources().getColor(android.R.color.holo_green_dark));

        // Add action buttons
        Intent dismissIntent = new Intent(this, NotificationActionReceiver.class);
        dismissIntent.setAction("ACTION_DISMISS");

        Intent reminderIntent = new Intent(this, NotificationActionReceiver.class);
        reminderIntent.setAction("ACTION_REMIND_LATER");

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                this, 0, dismissIntent, flags);

        PendingIntent reminderPendingIntent = PendingIntent.getBroadcast(
                this, 1, reminderIntent, flags);

        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent);
        builder.addAction(android.R.drawable.ic_popup_reminder, "Remind in 30m", reminderPendingIntent);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(999, builder.build());
            Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, "Notification permission denied. Please check settings.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // BroadcastReceiver to handle notification events
    public static class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int notificationId = intent.getIntExtra("notification_id", 0);
            String notificationText = intent.getStringExtra("notification_text");

            // Get a random title for the notification
            String[] titles = {
                    "Health Reminder",
                    "Aarogya Nidaan",
                    "Wellness Check",
                    "Health Tip",
                    "Daily Reminder",
                    "Stay Healthy!"
            };
            String title = titles[new Random().nextInt(titles.length)];

            // Build the notification
            Drawable drawable;
            try {
                drawable = AppCompatResources.getDrawable(context, R.drawable.chatbot);
            } catch (Exception e) {
                // If chatbot drawable isn't found, use a fallback
                drawable = AppCompatResources.getDrawable(context, android.R.drawable.ic_dialog_info);
            }

            Bitmap largeIcon = null;
            if (drawable != null) {
                largeIcon = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                        drawable.getIntrinsicHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(largeIcon);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }

            // Get small icon
            int smallIconResource;
            try {
                smallIconResource = R.drawable.aarogyanidaanlogo;
            } catch (Exception e) {
                // Fallback to system icon if app icon not found
                smallIconResource = android.R.drawable.ic_dialog_info;
            }

            // Create action intents
            Intent dismissIntent = new Intent(context, NotificationActionReceiver.class);
            dismissIntent.setAction("ACTION_DISMISS");
            dismissIntent.putExtra("notification_id", notificationId);

            Intent reminderIntent = new Intent(context, NotificationActionReceiver.class);
            reminderIntent.setAction("ACTION_REMIND_LATER");
            reminderIntent.putExtra("notification_id", notificationId);
            reminderIntent.putExtra("notification_text", notificationText);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
                    context, notificationId, dismissIntent, flags);

            PendingIntent reminderPendingIntent = PendingIntent.getBroadcast(
                    context, notificationId + 1000, reminderIntent, flags);

            // Build the notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(smallIconResource)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(title)
                    .setContentText(notificationText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setColor(context.getResources().getColor(android.R.color.holo_red_dark))
                    // Add action buttons
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                    .addAction(android.R.drawable.ic_popup_reminder, "Remind in 30m", reminderPendingIntent);

            // Show the notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            try {
                notificationManager.notify(notificationId, builder.build());
            } catch (SecurityException e) {
                e.printStackTrace();
                // Handle the case where notification permission is not granted
            }
        }
    }

    // BroadcastReceiver to handle notification actions
    public static class NotificationActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int notificationId = intent.getIntExtra("notification_id", 0);

            // Cancel the current notification
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(notificationId);

            if ("ACTION_REMIND_LATER".equals(action)) {
                // Schedule a reminder for 30 minutes later
                String notificationText = intent.getStringExtra("notification_text");

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, 30);

                Intent reminderIntent = new Intent(context, NotificationReceiver.class);
                reminderIntent.putExtra("notification_id", notificationId);
                reminderIntent.putExtra("notification_text", "⏰ Reminder: " + notificationText);

                int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        : PendingIntent.FLAG_UPDATE_CURRENT;

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId + 5000, // Different request code to avoid conflicts
                        reminderIntent,
                        flags
                );

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            calendar.getTimeInMillis(),
                            pendingIntent
                    );
                }

                // Show a toast confirmation
                Toast.makeText(context, "Reminder set for 30 minutes from now", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // BroadcastReceiver to handle daily notification reset
    public static class NotificationResetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Reschedule all notifications to ensure they continue working
            Intent launchIntent = new Intent(context, NotificationSchedulerActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);

            // Schedule the next day's reset
            Calendar midnight = Calendar.getInstance();
            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.add(Calendar.DAY_OF_YEAR, 1);

            Intent resetIntent = new Intent(context, NotificationResetReceiver.class);

            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    : PendingIntent.FLAG_UPDATE_CURRENT;

            PendingIntent resetPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    resetIntent,
                    flags
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        midnight.getTimeInMillis(),
                        resetPendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        midnight.getTimeInMillis(),
                        resetPendingIntent
                );
            }
        }
    }
}