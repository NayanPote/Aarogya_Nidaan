package com.healthcare.aarogyanidaan;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Admin extends AppCompatActivity {

    // UI elements
    private PieChart pieChartUsers;
    private LineChart lineChartAppointments;
    private static final String SHARED_PREFS = "sharedPrefs";
    private TextView txtPatientCount, txtDoctorCount;
    private TextView txtTotalAppointments, txtCompletedAppointments, txtPendingAppointments;
    private MaterialCardView cardPatients, cardDoctors;
    private FloatingActionButton fabRefresh;

    // Firebase reference
    private DatabaseReference databaseRef;

    // Instance variables
    private RecyclerView recentActivityRecyclerView;
    private ActivityLogAdapter activityLogAdapter;
    private ArrayList<ActivityLog> activityLogs = new ArrayList<>();
    private ArrayList<ActivityLog> filteredLogs = new ArrayList<>();
    private Chip chipActivityFilter;
    private MaterialButton btnViewAllActivity;
    private String currentFilter = "All";
    private boolean isViewingAll = false;
    private ImageButton btnNotifications, logout;

    // Counters
    private int patientCount = 0;
    private int doctorCount = 0;
    private int totalAppointments = 0;
    private int completedAppointments = 0;
    private int pendingAppointments = 0;

    // Monthly appointment data
    private int[] monthlyAppointments = new int[6]; // Last 6 months

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase
        databaseRef = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        initializeViews();

        // Configure window insets
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        // Set up click listeners
        setUpClickListeners();

        // Load data from Firebase
        fetchDataFromFirebase();
    }

    private void initializeViews() {
        // Initialize RecyclerView FIRST
        recentActivityRecyclerView = findViewById(R.id.recentActivityRecyclerView);
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Adapter AFTER RecyclerView is initialized
        activityLogAdapter = new ActivityLogAdapter(this, filteredLogs);
        recentActivityRecyclerView.setAdapter(activityLogAdapter);
        
        // Initialize UI elements
        logout = findViewById(R.id.logout);

        // Initialize TextViews
        txtPatientCount = findViewById(R.id.txtPatientCount);
        txtDoctorCount = findViewById(R.id.txtDoctorCount);
        txtTotalAppointments = findViewById(R.id.txtTotalAppointments);
        txtCompletedAppointments = findViewById(R.id.txtCompletedAppointments);
        txtPendingAppointments = findViewById(R.id.txtPendingAppointments);

        // Cards
        cardPatients = findViewById(R.id.cardPatients);
        cardDoctors = findViewById(R.id.cardDoctors);

        // Chips & Buttons
        chipActivityFilter = findViewById(R.id.chipActivityFilter);
        btnViewAllActivity = findViewById(R.id.btnViewAllActivity);

        // Charts
        pieChartUsers = findViewById(R.id.pieChartUsers);
        lineChartAppointments = findViewById(R.id.lineChartAppointments);
        btnNotifications = findViewById(R.id.btnNotifications);

        // Floating Action Button
        fabRefresh = findViewById(R.id.fabRefresh);

        // Set up empty charts with placeholders
        setupPieChart();
        setupLineChart();
    }


    private void setUpClickListeners() {
        fabRefresh.setOnClickListener(v -> {
            // Apply rotation animation to FAB
            fabRefresh.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));

            // Reset counters
            resetCounters();

            // Fetch data again
            fetchDataFromFirebase();

            // Show toast
            Toast.makeText(Admin.this, "Refreshing data...", Toast.LENGTH_SHORT).show();
        });

        // Activity filter chip
        chipActivityFilter.setOnClickListener(v -> {
            showFilterDialog();
        });

        btnNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminNotificationActivity.class);
            startActivity(intent);
        });
    
        // View all button
        btnViewAllActivity.setOnClickListener(v -> {
            isViewingAll = !isViewingAll;
            btnViewAllActivity.setText(isViewingAll ? "Show Recent" : "View All");
            filterLogs(currentFilter);;
        });

        cardPatients.setOnClickListener(v -> {
            // Handle patient card click - could navigate to detailed patients list
            Toast.makeText(Admin.this, "Patients: " + patientCount, Toast.LENGTH_SHORT).show();
        });

        cardDoctors.setOnClickListener(v -> {
            // Handle doctor card click - could navigate to detailed doctors list
            Toast.makeText(Admin.this, "Doctors: " + doctorCount, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnViewAllAppointments).setOnClickListener(v -> {
            // Navigate to appointments detailed view
            Toast.makeText(Admin.this, "View all appointments clicked", Toast.LENGTH_SHORT).show();
        });
        
        logout.setOnClickListener(v -> {
            // Handle logout click
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        if (isFinishing() || isDestroyed()) {
            return; // Prevent showing dialog if activity is closing
        }

        AlertDialog logoutDialog = new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create();

        logoutDialog.show();
    }

    private void logout() {
        // Get notification service instance before logout
        LocalNotificationService notificationService = LocalNotificationService.getInstance(this);
        if (notificationService != null) {
            // Explicitly cleanup notification service
            notificationService.cleanup();
        }

        // Clear "Remember Me" preference
        SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE).edit();
        editor.clear(); // Clear all preferences under SHARED_PREFS
        editor.apply();

        // Also clear ChatPrefs to ensure notification service doesn't restart
        getSharedPreferences("ChatPrefs", MODE_PRIVATE).edit().clear().apply();

        // Cancel all notifications
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        //  Clear chat history using ChatDatabaseHelper
        ChatDatabaseHelper chatDatabaseHelper = new ChatDatabaseHelper(this);
        chatDatabaseHelper.clearAllMessages(); //  Clear all chat history

        // Stop the foreground service (if running)
        Intent serviceIntent = new Intent(this, NotificationForegroundService.class);
        stopService(serviceIntent);

        // Firebase logout (if auth is initialized)
//        if (auth != null) {
//            auth.signOut();
//        }

        // Redirect to login page
        Intent intent = new Intent(Admin.this, loginpage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish current activity to prevent going back
    }

    private void showFilterDialog() {
        String[] filterOptions = {"All", "Appointments", "Chat Requests", "Conversations"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Activity By Type")
                .setItems(filterOptions, (dialog, which) -> {
                    currentFilter = filterOptions[which];
                    chipActivityFilter.setText("Filter: " + currentFilter);
                    filterLogs(currentFilter);
                });
        builder.create().show();
    }

    private void filterLogs(String filter) {
        filteredLogs.clear();

        if (filter.equals("All")) {
            if (isViewingAll) {
                filteredLogs.addAll(activityLogs);
            } else {
                // Show only the 5 most recent logs
                int count = Math.min(activityLogs.size(), 5);
                for (int i = 0; i < count; i++) {
                    filteredLogs.add(activityLogs.get(i));
                }
            }
        } else {
            for (ActivityLog log : activityLogs) {
                if (log.getType().equalsIgnoreCase(filter)) {
                    filteredLogs.add(log);
                }
            }

            if (!isViewingAll && filteredLogs.size() > 5) {
                filteredLogs = new ArrayList<>(filteredLogs.subList(0, 5));
            }
        }

        activityLogAdapter.notifyDataSetChanged();
    }

    private void fetchActivitiesFromFirebase() {
        activityLogs.clear();

        // 1. Fetch appointments
        fetchAppointments();
    }

    private void fetchAppointments() {
        databaseRef.child("adddelete").child("appointments")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot appointmentSnapshot : dataSnapshot.getChildren()) {
                            try {
                                // Extract appointment details
                                String id = appointmentSnapshot.child("id").getValue(String.class);
                                String doctorId = appointmentSnapshot.child("doctorId").getValue(String.class);
                                String patientId = appointmentSnapshot.child("patientId").getValue(String.class);
                                String dateStr = appointmentSnapshot.child("date").getValue(String.class);
                                String timeStr = appointmentSnapshot.child("time").getValue(String.class);
                                String status = appointmentSnapshot.child("status").getValue(String.class);

                                // Calculate timestamp
                                long timestamp = convertToTimestamp(dateStr, timeStr);

                                // Create message
                                String message = "New appointment scheduled with doctor ID: " + doctorId +
                                        " for patient ID: " + patientId + " (" + status + ")";

                                ActivityLog log = new ActivityLog(
                                        "Appointments",
                                        message,
                                        timestamp,
                                        patientId,
                                        "appointment"
                                );

                                activityLogs.add(log);
                            } catch (Exception e) {
                                // Handle parsing errors
                                e.printStackTrace();
                            }
                        }

                        // Continue with fetching chat requests
                        fetchChatRequests();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(Admin.this, "Failed to load appointments: " +
                                databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        fetchChatRequests(); // Continue with next fetch
                    }
                });
    }

    private void fetchChatRequests() {
        databaseRef.child("adddelete").child("chat_requests")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot requestSnapshot : dataSnapshot.getChildren()) {
                            try {
                                // Extract chat request details
                                String doctorId = requestSnapshot.child("doctorId").getValue(String.class);
                                String patientId = requestSnapshot.child("patientId").getValue(String.class);
                                String patientName = requestSnapshot.child("patientName").getValue(String.class);
                                String requestId = requestSnapshot.child("requestId").getValue(String.class);
                                String status = requestSnapshot.child("status").getValue(String.class);
                                Long timestamp = requestSnapshot.child("timestamp").getValue(Long.class);

                                if (timestamp == null) {
                                    timestamp = System.currentTimeMillis(); // Default to current time if missing
                                }

                                // Create message
                                String message = "Chat request from " + (patientName != null ? patientName : patientId) +
                                        " to doctor ID: " + doctorId + " (" + status + ")";

                                ActivityLog log = new ActivityLog(
                                        "Chat Requests",
                                        message,
                                        timestamp,
                                        patientId,
                                        "chat"
                                );

                                activityLogs.add(log);
                            } catch (Exception e) {
                                // Handle parsing errors
                                e.printStackTrace();
                            }
                        }

                        // Continue with fetching conversations
                        fetchConversations();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(Admin.this, "Failed to load chat requests: " +
                                databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        fetchConversations(); // Continue with next fetch
                    }
                });
    }

    private void fetchConversations() {
        databaseRef.child("adddelete").child("conversations")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot conversationSnapshot : dataSnapshot.getChildren()) {
                            try {
                                if (conversationSnapshot.hasChild("conversationId")) {
                                    String conversationId = conversationSnapshot.child("conversationId").getValue(String.class);

                                    // We don't need to fetch messages as mentioned in requirements

                                    // Create activity log
                                    ActivityLog log = new ActivityLog(
                                            "Conversations",
                                            "Conversation started with ID: " + conversationId,
                                            System.currentTimeMillis() - (long)(Math.random() * 86400000), // Random time within last 24h for demo
                                            "system",
                                            "conversation"
                                    );

                                    activityLogs.add(log);
                                }
                            } catch (Exception e) {
                                // Handle parsing errors
                                e.printStackTrace();
                            }
                        }

                        // Sort all logs by timestamp (newest first)
                        Collections.sort(activityLogs, (log1, log2) ->
                                Long.compare(log2.getTimestamp(), log1.getTimestamp()));

                        // Apply initial filter
                        filterLogs("All");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(Admin.this, "Failed to load conversations: " +
                                databaseError.getMessage(), Toast.LENGTH_SHORT).show();

                        // If all else fails, sort and display what we have
                        Collections.sort(activityLogs, (log1, log2) ->
                                Long.compare(log2.getTimestamp(), log1.getTimestamp()));
                        filterLogs("All");
                    }
                });
    }

    private long convertToTimestamp(String dateStr, String timeStr) {
        try {
            // Parse date in the format "dd/MM/yyyy" and time in the format "HH:mm"
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = dateFormat.parse(dateStr + " " + timeStr);
            return date != null ? date.getTime() : System.currentTimeMillis();
        } catch (ParseException e) {
            e.printStackTrace();
            return System.currentTimeMillis(); // Return current time if parsing fails
        }
    }



    private void resetCounters() {
        patientCount = 0;
        doctorCount = 0;
        totalAppointments = 0;
        completedAppointments = 0;
        pendingAppointments = 0;

        for (int i = 0; i < monthlyAppointments.length; i++) {
            monthlyAppointments[i] = 0;
        }
    }

    private void fetchDataFromFirebase() {
        fetchActivitiesFromFirebase();

        // Fetch patient count
        databaseRef.child("patient").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                patientCount = (int) dataSnapshot.getChildrenCount();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Admin.this, "Failed to load patient data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch doctor count
        databaseRef.child("doctor").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                doctorCount = (int) dataSnapshot.getChildrenCount();
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Admin.this, "Failed to load doctor data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch appointments data
        databaseRef.child("appointments").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                totalAppointments = (int) dataSnapshot.getChildrenCount();

                // Get current month and year
                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH);
                int currentYear = calendar.get(Calendar.YEAR);

                // Analyze appointments
                for (DataSnapshot appointmentSnapshot : dataSnapshot.getChildren()) {
                    // Example: Check appointment status
                    String status = appointmentSnapshot.child("status").getValue(String.class);
                    if (status != null) {
                        if (status.equalsIgnoreCase("completed")) {
                            completedAppointments++;
                        } else {
                            pendingAppointments++;
                        }
                    }

                    // Example: Gather monthly data
                    Long timestamp = appointmentSnapshot.child("timestamp").getValue(Long.class);
                    if (timestamp != null) {
                        Calendar appointmentDate = Calendar.getInstance();
                        appointmentDate.setTimeInMillis(timestamp);
                        int appointmentMonth = appointmentDate.get(Calendar.MONTH);
                        int appointmentYear = appointmentDate.get(Calendar.YEAR);

                        // Calculate month difference
                        int monthDiff = (currentYear - appointmentYear) * 12 + (currentMonth - appointmentMonth);
                        if (monthDiff >= 0 && monthDiff < 6) {
                            monthlyAppointments[5 - monthDiff]++;
                        }
                    }
                }

                updateUI();

                databaseRef.child("activityLogs").limitToLast(5).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        activityLogs.clear();

                        for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                            ActivityLog log = logSnapshot.getValue(ActivityLog.class);
                            if (log != null) {
                                activityLogs.add(log);
                            }
                        }

                        // If no logs found, add sample logs for testing
                        if (activityLogs.isEmpty()) {

                        }

                        // Update the adapter
                        activityLogAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(Admin.this, "Failed to load activity logs: " +
                                databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Admin.this, "Failed to load appointment data: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        // Update text views with animations
        animateTextView(txtPatientCount, 0, patientCount);
        animateTextView(txtDoctorCount, 0, doctorCount);
        animateTextView(txtTotalAppointments, 0, totalAppointments);
        animateTextView(txtCompletedAppointments, 0, completedAppointments);
        animateTextView(txtPendingAppointments, 0, pendingAppointments);

        // Update charts
        updatePieChart();
        updateLineChart();
    }

    private void animateTextView(TextView textView, int start, int end) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(1500);
        animator.addUpdateListener(animation -> {
            textView.setText(String.valueOf(animation.getAnimatedValue()));
        });
        animator.start();
    }

    private void setupPieChart() {
        pieChartUsers.setUsePercentValues(true);
        pieChartUsers.getDescription().setEnabled(false);
        pieChartUsers.setExtraOffsets(5, 10, 5, 5);
        pieChartUsers.setDragDecelerationFrictionCoef(0.95f);

        pieChartUsers.setDrawHoleEnabled(true);
        pieChartUsers.setHoleColor(ContextCompat.getColor(this, R.color.background));
        pieChartUsers.setTransparentCircleColor(ContextCompat.getColor(this, R.color.background));
        pieChartUsers.setTransparentCircleAlpha(110);
        pieChartUsers.setHoleRadius(58f);
        pieChartUsers.setTransparentCircleRadius(61f);

        pieChartUsers.setDrawCenterText(true);
        pieChartUsers.setCenterText("User\nDistribution");
        pieChartUsers.setCenterTextSize(16f);

        pieChartUsers.setRotationAngle(0);
        pieChartUsers.setRotationEnabled(true);
        pieChartUsers.setHighlightPerTapEnabled(true);

        // Set up legend
        Legend legend = pieChartUsers.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(0f);
        legend.setYOffset(0f);

        // Set initial data (will be updated)
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(1, "Patients"));
        entries.add(new PieEntry(1, "Doctors"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Set custom colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(this, R.color.patient_color));
        colors.add(ContextCompat.getColor(this, R.color.doctor_color));
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartUsers));
        data.setValueTextSize(11f);
        data.setValueTextColor(ContextCompat.getColor(this, R.color.white));

        pieChartUsers.setData(data);
        pieChartUsers.highlightValues(null);

        // Refresh
        pieChartUsers.invalidate();
    }

    private void updatePieChart() {
        // Don't update if counts are both zero
        if (patientCount == 0 && doctorCount == 0) {
            return;
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(patientCount, "Patients"));
        entries.add(new PieEntry(doctorCount, "Doctors"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Set custom colors
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(this, R.color.patient_color));
        colors.add(ContextCompat.getColor(this, R.color.doctor_color));
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChartUsers));
        data.setValueTextSize(11f);
        data.setValueTextColor(ContextCompat.getColor(this, R.color.white));

        pieChartUsers.setData(data);

        // Animate the chart
        pieChartUsers.animateY(1400, Easing.EaseInOutQuad);

        // Refresh
        pieChartUsers.invalidate();
    }

    private void setupLineChart() {
        lineChartAppointments.getDescription().setEnabled(false);
        lineChartAppointments.setDrawGridBackground(false);
        lineChartAppointments.setTouchEnabled(true);
        lineChartAppointments.setDragEnabled(true);
        lineChartAppointments.setScaleEnabled(true);
        lineChartAppointments.setPinchZoom(true);

        // Get the last 6 months
        String[] months = getLastSixMonths();

        // Configure X-Axis
        XAxis xAxis = lineChartAppointments.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(months));

        // Configure Y-Axis (Left)
        YAxis leftAxis = lineChartAppointments.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        // Disable right Y-Axis
        lineChartAppointments.getAxisRight().setEnabled(false);

        // Create initial data (will be updated)
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            entries.add(new Entry(i, 0));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Appointments");
        dataSet.setColor(ContextCompat.getColor(this, R.color.appointment_color));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.appointment_color));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.transparent));

        LineData lineData = new LineData(dataSet);
        lineChartAppointments.setData(lineData);

        // Refresh
        lineChartAppointments.invalidate();
    }

    private void updateLineChart() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            entries.add(new Entry(i, monthlyAppointments[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Appointments");
        dataSet.setColor(ContextCompat.getColor(this, R.color.appointment_color));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.appointment_color));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.transparent));

        LineData lineData = new LineData(dataSet);
        lineChartAppointments.setData(lineData);

        // Animate the chart
        lineChartAppointments.animateX(1400, Easing.EaseInOutQuad);

        // Refresh
        lineChartAppointments.invalidate();
    }

    private String[] getLastSixMonths() {
        String[] months = new String[6];
        Calendar calendar = Calendar.getInstance();

        for (int i = 5; i >= 0; i--) {
            // Get current month
            int currentMonth = calendar.get(Calendar.MONTH);
            // Move back i months
            calendar.add(Calendar.MONTH, -i);
            // Format month name (short form)
            months[5 - i] = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
            // Reset to current date
            calendar = Calendar.getInstance();
        }

        return months;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when activity resumes
        resetCounters();
        fetchDataFromFirebase();
    }

    public static class ActivityLog {
        private String type;
        private String message;
        private long timestamp;
        private String userId;
        private String iconType;

        // Required empty constructor for Firebase
        public ActivityLog() {}

        public ActivityLog(String type, String message, long timestamp, String userId, String iconType) {
            this.type = type;
            this.message = message;
            this.timestamp = timestamp;
            this.userId = userId;
            this.iconType = iconType;
        }

        // Getters
        public String getType() { return type; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public String getUserId() { return userId; }
        public String getIconType() { return iconType; }

        // Formatted time (e.g., "Mar 20, 2025 15:30")
        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        // Relative time (e.g., "2 hours ago")
        public String getRelativeTime() {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (days > 0) {
                return days == 1 ? "1 day ago" : days + " days ago";
            } else if (hours > 0) {
                return hours == 1 ? "1 hour ago" : hours + " hours ago";
            } else if (minutes > 0) {
                return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";
            } else {
                return "Just now";
            }
        }
    }


    // Adapter for activity logs
    private class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.ViewHolder> {
        private Context context;
        private ArrayList<ActivityLog> logs;

        public ActivityLogAdapter(Context context, ArrayList<ActivityLog> logs) {
            this.context = context;
            this.logs = logs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_activity_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ActivityLog log = logs.get(position);

            holder.txtActivityMessage.setText(log.getMessage());
            holder.txtActivityTime.setText(log.getRelativeTime());
            holder.txtActivityType.setText(log.getType());

            // Set icon based on activity type
            int iconResId;
            int bgColor;

            switch (log.getIconType()) {
                case "appointment":
                    iconResId = R.drawable.colorcalander; // Replace with your icon
                    bgColor = ContextCompat.getColor(context, R.color.appointment_color);
                    break;
                case "registration":
                    iconResId = R.drawable.blackperson; // Replace with your icon
                    bgColor = ContextCompat.getColor(context, R.color.patient_color);
                    break;
                case "update":
                    iconResId = R.drawable.ic_update; // Replace with your icon
                    bgColor = ContextCompat.getColor(context, R.color.doctor_color);
                    break;
                default:
                    iconResId = R.drawable.info; // Replace with your icon
                    bgColor = ContextCompat.getColor(context, R.color.primary);
                    break;
            }

            holder.imgActivityIcon.setImageResource(iconResId);
            holder.iconBackground.setBackgroundTintList(ColorStateList.valueOf(bgColor));

            // Set card color based on type
            int cardColor;
            switch (log.getType()) {
                case "Appointments":
                    cardColor = ContextCompat.getColor(context, R.color.appointment_bg);
                    break;
                case "Chat Requests":
                    cardColor = ContextCompat.getColor(context, R.color.chat_bg);
                    break;
                case "Conversations":
                    cardColor = ContextCompat.getColor(context, R.color.conversation_bg);
                    break;
                default:
                    cardColor = ContextCompat.getColor(context, R.color.white);
                    break;
            }
            holder.itemView.setBackgroundTintList(ColorStateList.valueOf(cardColor));

            // Animate the item
            Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
            holder.itemView.startAnimation(animation);
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtActivityMessage, txtActivityTime, txtActivityType;
            ImageView imgActivityIcon;
            View iconBackground;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                txtActivityMessage = itemView.findViewById(R.id.txtActivityMessage);
                txtActivityTime = itemView.findViewById(R.id.txtActivityTime);
                txtActivityType = itemView.findViewById(R.id.txtActivityType);
                imgActivityIcon = itemView.findViewById(R.id.imgActivityIcon);
                iconBackground = itemView.findViewById(R.id.iconBackground);

                itemView.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ActivityLog log = logs.get(position);
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(log.getType())
                                .setMessage("Time: " + log.getFormattedTime() + "\n\n" + log.getMessage())
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            }
        }
    }
}