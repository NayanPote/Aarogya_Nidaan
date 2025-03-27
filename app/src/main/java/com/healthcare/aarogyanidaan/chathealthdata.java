package com.healthcare.aarogyanidaan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class chathealthdata extends AppCompatActivity {

    private ImageButton backButton, clinicalNotessave, clinicalNotesedit, clinicalNotesmic, reportdownload;
    private EditText clinicalNotes;
    private ProgressDialog progressDialog;
    private boolean isEditing = false;

    private static final int SPEECH_REQUEST_CODE = 0;
    private static final String TAG = "ChatHealthData";

    private TextView temperatureValueText, heartRateValueText, bloodPressureValueText,
            oxygenSaturationValueText, respiratoryRateValueText, pulseRateValueText,
            sweatRateValueText, sleepDurationValueText, pulseTransitTimeValueText,
            cholesterollevelsValueText, hemoglobinlevelsValueText, electrolytelevelsValueText;

    private TextView patientIdText, reportDate, doctorNameText, patientNameText, reportTime, patientage;
    private String conversationId;
    private String patientId;
    private String currentNotes = "";

    // Database references
    private DatabaseReference healthDataRef;
    private DatabaseReference conversationRef;
    private DatabaseReference clinicalNotesRef;
    private DatabaseReference patientRef;
    private ValueEventListener healthDataListener;
    private ValueEventListener conversationListener;
    private ValueEventListener clinicalNotesListener;
    private ValueEventListener patientListener;
    private Timer dateTimeTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chathealthdata);

        conversationId = getIntent().getStringExtra("conversationId");

        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Error: Conversation ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupInitialState();
        setupListeners();
        setupConversationListener();
        startDateTimeUpdates();
    }

    private void initializeViews() {
        // Initialize all views
        backButton = findViewById(R.id.backButton);
        clinicalNotes = findViewById(R.id.clinicalNotes);
        clinicalNotessave = findViewById(R.id.clinicalNotessave);
        clinicalNotesedit = findViewById(R.id.clinicalNotesedit);
        clinicalNotesmic = findViewById(R.id.clinicalNotesmic);

        temperatureValueText = findViewById(R.id.temperatureValueText);
        heartRateValueText = findViewById(R.id.heartRateValueText);
        bloodPressureValueText = findViewById(R.id.bloodPressureValueText);
        oxygenSaturationValueText = findViewById(R.id.oxygenSaturationValueText);
        respiratoryRateValueText = findViewById(R.id.respiratoryrateValueText);
        pulseRateValueText = findViewById(R.id.pulserateValueText);
        sweatRateValueText = findViewById(R.id.sweatrateValueText);
        sleepDurationValueText = findViewById(R.id.sleepdataValueText);
        pulseTransitTimeValueText = findViewById(R.id.pulsetransittimeValueText);
        cholesterollevelsValueText = findViewById(R.id.cholesterollevelsValueText);
        hemoglobinlevelsValueText = findViewById(R.id.hemoglobinlevelsValueText);
        electrolytelevelsValueText = findViewById(R.id.electrolytelevelsValueText);
        reportdownload = findViewById(R.id.reportdownload);

        patientIdText = findViewById(R.id.PatientId);
        doctorNameText = findViewById(R.id.doctorName);
        patientNameText = findViewById(R.id.patientNameText);
        patientage = findViewById(R.id.patientage);
        reportDate = findViewById(R.id.reportDate);
        reportTime = findViewById(R.id.reportTime);
    }

    private void setupInitialState() {
        // Set initial states for clinical notes
        clinicalNotes.setEnabled(false);
        clinicalNotessave.setEnabled(false);
        clinicalNotesedit.setEnabled(true);
        clinicalNotesmic.setEnabled(true);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> onBackPressed());

        clinicalNotessave.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(clinicalNotes.getText())) {
                saveClinicalNotes(clinicalNotes.getText().toString().trim());
                setEditingMode(false);
            } else {
//                Toast.makeText(this, "Please enter notes before saving", Toast.LENGTH_SHORT).show();
                saveClinicalNotes(clinicalNotes.getText().toString().trim());
                setEditingMode(false);
            }
        });

        clinicalNotesedit.setOnClickListener(v -> setEditingMode(true));

        clinicalNotesmic.setOnClickListener(v -> startSpeechToText());

        reportdownload.setOnClickListener(v -> generatePdfReport());

    }

    private void generatePdfReport() {
        try {
            // Show progress dialog
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Generating Report");
            progressDialog.setMessage("Please wait while we prepare your health report...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();

            // Update progress
            updateProgress(10);

            // File name and path
            String fileName ="Aarogya_Nidaan_Report_" + System.currentTimeMillis() + ".pdf";
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, fileName);

            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headingPaint = new Paint();
            Paint tablePaint = new Paint();
            Paint linePaint = new Paint();

            // Page setup (A4 size)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            // Update progress
            updateProgress(20);

            // Set up paints
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextSize(24);
            titlePaint.setColor(Color.rgb(0, 102, 204));
            titlePaint.setTextAlign(Paint.Align.CENTER);

            headingPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headingPaint.setTextSize(16);
            headingPaint.setColor(Color.rgb(51, 51, 51));

            paint.setTextSize(12);
            paint.setColor(Color.BLACK);

            tablePaint.setStyle(Paint.Style.STROKE);
            tablePaint.setStrokeWidth(0.7f);
            tablePaint.setColor(Color.rgb(51, 51, 51));

            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(1.5f);
            linePaint.setColor(Color.rgb(0, 102, 204));

            // Update progress
            updateProgress(30);

            // Add logo/header
            // Draw Header Background
            Paint logoBackPaint = new Paint();
            logoBackPaint.setColor(Color.rgb(240, 248, 255));
            canvas.drawRect(30, 30, pageInfo.getPageWidth() - 30, 110, logoBackPaint);
            canvas.drawLine(30, 110, pageInfo.getPageWidth() - 30, 110, linePaint);

            // Load and draw the logo (add logo to 'res/drawable' folder)
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.aarogyanidaanlogo);
            if (logo != null) {
                // Resize the logo if it's too large
                int logoWidth = 60;
                int logoHeight = 60;
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, false);

                // Draw the logo on the left side
                int logoX = 40;
                int logoY = 40;
                canvas.drawBitmap(scaledLogo, logoX, logoY, null);
            }

            titlePaint.setTextSize(28);
            canvas.drawText("AAROGYA NIDAAN", pageInfo.getPageWidth() / 2, 65, titlePaint);

            titlePaint.setTextSize(14);
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            canvas.drawText("True Diagnosis For Wellness", pageInfo.getPageWidth() / 2, 90, titlePaint);


            // Update progress
            updateProgress(40);

            // Patient Info
            int y = 150;
            headingPaint.setTextSize(16);
            canvas.drawText("PATIENT HEALTH REPORT", 40, y, headingPaint);
            y += 10;
            canvas.drawLine(40, y, 300, y, linePaint);
            y += 20;

            // Patient details table
            drawPatientInfo(canvas, y);
            y += 120;

            // Update progress
            updateProgress(60);

            // Health Metrics
            headingPaint.setTextSize(16);
            canvas.drawText("HEALTH METRICS", 40, y, headingPaint);
            y += 10;
            canvas.drawLine(40, y, 300, y, linePaint);
            y += 20;

            // Health data table
            drawHealthDataTable(canvas, y);
            y += 330;

            canvas.drawLine(40, y, 300, y, linePaint);
            y += 40;

            // Update progress
            updateProgress(70);

            // Add recommendation section
            headingPaint.setTextSize(16);
            canvas.drawText("RECOMMENDED FOLLOW-UP", 40, y, headingPaint);
            y += 10;
            canvas.drawLine(40, y, 300, y, linePaint);
            y += 20;

            // Draw recommendations box
            Paint recommendBgPaint = new Paint();
            recommendBgPaint.setColor(Color.rgb(240, 248, 255));
            canvas.drawRect(40, y, pageInfo.getPageWidth() - 40, y + 80, recommendBgPaint);
            canvas.drawRect(40, y, pageInfo.getPageWidth() - 40, y + 80, tablePaint);

            paint.setTextSize(12);
            String followUpText = "Based on current health metrics, we recommend:";
            canvas.drawText(followUpText, 50, y + 20, paint);
            canvas.drawText("• Revisit in 3 months for follow-up assessment", 60, y + 40, paint);
            canvas.drawText("• Continue prescribed medication regimen", 60, y + 60, paint);

            y += 100;

            // Finish first page
            pdfDocument.finishPage(page);

            // Update progress
            updateProgress(70);

            // Create second page for Clinical Notes
            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 2).create();
            page = pdfDocument.startPage(pageInfo);
            canvas = page.getCanvas();

            // Start Clinical Notes section
            y = 50;  // Start below the header
            headingPaint.setTextSize(16);
            canvas.drawText("CLINICAL NOTES", 40, y, headingPaint);
            y += 10;
            canvas.drawLine(40, y, 300, y, linePaint);
            y += 20;

            if (!TextUtils.isEmpty(clinicalNotes.getText())) {
                Paint notesPaint = new Paint();
                notesPaint.setTextSize(12);
                notesPaint.setColor(Color.BLACK);

                String notesText = clinicalNotes.getText().toString();
                String[] lines = notesText.split("\n");

                int margin = 50; // Left margin
                int width = pageInfo.getPageWidth() - (2 * margin);
                int footerSpace = 40; // Reduced footer space

                for (String line : lines) {
                    // Use StaticLayout for better text wrapping
                    StaticLayout staticLayout = new StaticLayout(
                            line,
                            new TextPaint(notesPaint),
                            width,
                            Layout.Alignment.ALIGN_NORMAL,
                            1.0f,
                            0,
                            false
                    );

                    for (int i = 0; i < staticLayout.getLineCount(); i++) {
                        if (y + notesPaint.getTextSize() > pageInfo.getPageHeight() - footerSpace) {
                            // Finish current page and start a new one when near the bottom
                            pdfDocument.finishPage(page);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                            page = pdfDocument.startPage(pageInfo);
                            canvas = page.getCanvas();

                            // Reset y position and continue
                            y = 50;
                            canvas.drawText("CLINICAL NOTES (CONTINUED)", margin, y, notesPaint);
                            y += 30;
                        }

                        float x = staticLayout.getLineLeft(i);
                        float textY = y + staticLayout.getLineBaseline(i);
                        canvas.drawText(line.substring(staticLayout.getLineStart(i), staticLayout.getLineEnd(i)), margin + x, textY, notesPaint);
                    }

                    y += staticLayout.getHeight();
                }
            }

            // Update progress
            updateProgress(80);

            // Doctor's signature
            if (y > pageInfo.getPageHeight() - 150) {
                // If we're running out of space, finish this page and create a new one
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            y = Math.max(y, pageInfo.getPageHeight() - 230);

            // Doctor signature
            headingPaint.setTextSize(14);
            canvas.drawText("Doctor's Authorization:", pageInfo.getPageWidth() - 350, y, headingPaint);
            y += 20;

            // Draw signature line
            canvas.drawLine(pageInfo.getPageWidth() - 250, y + 40, pageInfo.getPageWidth() - 100, y + 40, linePaint);
            y += 60;

            // Doctor's name
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(doctorNameText.getText().toString(), pageInfo.getPageWidth() - 175, y, paint);
            y += 15;

            // Doctor designation
            paint.setTextSize(10);
            canvas.drawText("Medical Practitioner", pageInfo.getPageWidth() - 175, y, paint);
            paint.setTextAlign(Paint.Align.LEFT);

            // Update progress
            updateProgress(90);

            // Footer with disclaimer
            int footerY = pageInfo.getPageHeight() - 50;
            paint.setTextSize(8);
            paint.setColor(Color.GRAY);
            canvas.drawLine(30, footerY - 20, pageInfo.getPageWidth() - 30, footerY - 20, tablePaint);
            canvas.drawText("DISCLAIMER: This report is generated by Aarogya Nidaan application and is intended for informational purposes only.",
                    40, footerY, paint);
            canvas.drawText("It should not replace professional medical advice. Please consult with a healthcare professional for medical diagnosis and treatment.",
                    40, footerY + 10, paint);
            canvas.drawText("Report generated on: " + reportDate.getText().toString() + " at " + reportTime.getText().toString(),
                    40, footerY + 20, paint);

            pdfDocument.finishPage(page);

            // Save to file
            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            // Update progress to completion
            updateProgress(100);

            // Dismiss progress dialog
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            // Show success dialog with view option
            showReportCompletionDialog(file);

        } catch (IOException e) {
            Log.e(TAG, "Error creating PDF: " + e.getMessage());
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Toast.makeText(this, "Failed to generate report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void drawPatientInfo(Canvas canvas, int startY) {
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.rgb(80, 80, 80)); // Dark gray for labels
        labelPaint.setTextSize(12);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.BLACK); // Black for values
        valuePaint.setTextSize(12);

        int leftX = 40; // Starting X position
        int y = startY; // Starting Y position
        int lineSpacing = 20; // Space between lines

        // Draw Patient Name
        canvas.drawText("Patient Name: ", leftX, y, labelPaint);
        canvas.drawText(patientNameText.getText().toString(), leftX + 150, y, valuePaint);
        y += lineSpacing;

        // Draw Patient ID
        canvas.drawText("Patient ID: ", leftX, y, labelPaint);
        canvas.drawText(patientIdText.getText().toString(), leftX + 150, y, valuePaint);
        y += lineSpacing;

        // Draw Age
        canvas.drawText("Age: ", leftX, y, labelPaint);
        canvas.drawText(patientage.getText().toString(), leftX + 150, y, valuePaint);
        y += lineSpacing;

        // Draw Doctor Name
        canvas.drawText("Doctor: ", leftX, y, labelPaint);
        canvas.drawText(doctorNameText.getText().toString(), leftX + 150, y, valuePaint);
        y += lineSpacing;

        // Draw a separator line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.rgb(0, 102, 204)); // Blue color for the line
        linePaint.setStrokeWidth(1.5f);
        canvas.drawLine(leftX, y, 400, y, linePaint);
    }

    private int drawHealthDataTable(Canvas canvas, int y) {
        // Define reusable paints with better readability
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.rgb(50, 50, 50)); // Darker gray for better contrast
        labelPaint.setTextSize(14); // Slightly larger text
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setAntiAlias(true); // Smoother text rendering

        Paint normalPaint = new Paint();
        normalPaint.setColor(Color.rgb(0, 128, 0)); // Dark green for normal values
        normalPaint.setTextSize(14);
        normalPaint.setAntiAlias(true);

        Paint abnormalPaint = new Paint();
        abnormalPaint.setColor(Color.rgb(204, 0, 0)); // Less harsh red for abnormal values
        abnormalPaint.setTextSize(14);
        abnormalPaint.setAntiAlias(true);

        Paint rangePaint = new Paint();
        rangePaint.setColor(Color.rgb(100, 100, 100)); // Medium gray for range text
        rangePaint.setTextSize(12);
        rangePaint.setAntiAlias(true);

        // Layout dimensions - reduced column spacing
        int leftMargin = 40;
        int columnSpacing = 140; // Reduced from 180
        int rowHeight = 24; // Maintained for readability

        // Added extra padding to move header down
        int headerPadding = 10;
        y += headerPadding;

        // Draw header (moved down by headerPadding)
        canvas.drawText("Metric", leftMargin, y, labelPaint);
        canvas.drawText("Value", leftMargin + columnSpacing, y, labelPaint);
        canvas.drawText("Normal Range", leftMargin + columnSpacing * 2, y, labelPaint);

        // Table separator line
        Paint linePaint = new Paint();
        linePaint.setColor(Color.LTGRAY);
        linePaint.setStrokeWidth(1);
        canvas.drawLine(leftMargin, y + 10, leftMargin + columnSpacing * 3, y + 10, linePaint);

        y += rowHeight + 10; // Add extra space after header and separator line

        // Define health metrics with their values and normal ranges
        String[][] healthMetrics = {
                {"Temperature", temperatureValueText.getText().toString(), "36.5-37.5°C"},
                {"Heart Rate", heartRateValueText.getText().toString(), "60-100 bpm"},
                {"Blood Pressure", bloodPressureValueText.getText().toString(), "90/60-120/80 mmHg"},
                {"Oxygen Saturation", oxygenSaturationValueText.getText().toString(), "95-100%"},
                {"Respiratory Rate", respiratoryRateValueText.getText().toString(), "12-20 RPM"},
                {"Pulse Rate", pulseRateValueText.getText().toString(), "60-100 BPM"},
                {"Sweat Rate", sweatRateValueText.getText().toString(), "0.5-1.5 L/hr"},
                {"Sleep Duration", sleepDurationValueText.getText().toString(), "7-9 hrs"},
                {"Pulse Transit Time", pulseTransitTimeValueText.getText().toString(), "200-250 ms"},
                {"Cholesterol", cholesterollevelsValueText.getText().toString(), "<200 mg/dL"},
                {"Hemoglobin", hemoglobinlevelsValueText.getText().toString(), "13.5-17.5 g/dL (M), 12.0-15.5 g/dL (F)"},
                {"Electrolytes", electrolytelevelsValueText.getText().toString(), "Na+: 135-145 mmol/L, K+: 3.5-5.0 mmol/L"}
        };

        // Draw each row
        for (String[] metric : healthMetrics) {
            String metricName = metric[0];
            String value = metric[1];
            String normalRange = metric[2];

            // Draw metric name
            canvas.drawText(metricName, leftMargin, y, labelPaint);

            // Determine value display and color
            Paint valuePaint;
            if (value == null || value.trim().isEmpty()) {
                value = "N/A";
                valuePaint = rangePaint;
            } else {
                boolean isNormal = isValueNormal(value, normalRange);
                valuePaint = isNormal ? normalPaint : abnormalPaint;
            }

            // Draw value
            canvas.drawText(value, leftMargin + columnSpacing, y, valuePaint);

            // Draw normal range
            canvas.drawText(normalRange, leftMargin + columnSpacing * 2, y, rangePaint);

            y += rowHeight;
        }

        return y;
    }

    /**
     * Determines if a health metric value is within normal range
     * @param value The measured value as a string
     * @param normalRange The normal range specification
     * @return true if the value is within normal range, false otherwise
     */
    private boolean isValueNormal(String value, String normalRange) {
        if (value == null || value.trim().isEmpty() || normalRange == null || normalRange.trim().isEmpty()) {
            return false;
        }

        try {
            // Handle blood pressure (special case)
            if (value.contains("/") && normalRange.contains("/")) {
                return isBloodPressureNormal(value, normalRange);
            }

            // Handle gender-specific ranges
            if (normalRange.contains("(M)") || normalRange.contains("(F)")) {
                return isGenderSpecificValueNormal(value, normalRange);
            }

            // Handle electrolyte ranges
            if (normalRange.contains("Na+") || normalRange.contains("K+")) {
                return isElectrolyteNormal(value, normalRange);
            }

            // Handle simple ranges with min-max
            if (normalRange.contains("-")) {
                return isInSimpleRange(value, normalRange);
            }

            // Handle threshold values (less than or greater than)
            if (normalRange.startsWith("<") || normalRange.startsWith(">")) {
                return isWithinThreshold(value, normalRange);
            }

            // If no pattern matches, default to direct comparison
            return value.equalsIgnoreCase(normalRange);

        } catch (Exception e) {
            Log.e("HealthDataTable", "Error parsing value: " + value + " or range: " + normalRange, e);
            return false;
        }
    }

    /**
     * Checks if blood pressure is within normal range
     */
    private boolean isBloodPressureNormal(String value, String normalRange) {
        try {
            // Extract systolic and diastolic values
            String[] valueParts = value.split("/");
            if (valueParts.length != 2) return false;

            int systolic = Integer.parseInt(valueParts[0].trim().replaceAll("[^\\d]", ""));
            int diastolic = Integer.parseInt(valueParts[1].trim().replaceAll("[^\\d]", ""));

            // Extract normal range
            String[] rangeParts = normalRange.split("-");
            if (rangeParts.length != 2) return false;

            String[] lowParts = rangeParts[0].split("/");
            String[] highParts = rangeParts[1].split("/");

            int minSystolic = Integer.parseInt(lowParts[0].trim().replaceAll("[^\\d]", ""));
            int minDiastolic = Integer.parseInt(lowParts[1].trim().replaceAll("[^\\d]", ""));
            int maxSystolic = Integer.parseInt(highParts[0].trim().replaceAll("[^\\d]", ""));
            int maxDiastolic = Integer.parseInt(highParts[1].trim().replaceAll("[^\\d]", ""));

            return (systolic >= minSystolic && systolic <= maxSystolic) &&
                    (diastolic >= minDiastolic && diastolic <= maxDiastolic);
        } catch (Exception e) {
            Log.e("HealthDataTable", "Error parsing blood pressure", e);
            return false;
        }
    }

    /**
     * Checks if a value is within a gender-specific range
     */
    private boolean isGenderSpecificValueNormal(String value, String normalRange) {
        try {
            float actualValue = Float.parseFloat(value.replaceAll("[^\\d.]", ""));

            // Get gender-specific ranges
            String[] genderRanges = normalRange.split(",");

            for (String range : genderRanges) {
                // Extract min and max values
                int startIndex = range.indexOf("-");
                int endIndex = range.indexOf(" ", startIndex);
                if (startIndex > 0 && endIndex > startIndex) {
                    String minMax = range.substring(startIndex - 4, endIndex).trim();
                    String[] parts = minMax.split("-");
                    if (parts.length == 2) {
                        float min = Float.parseFloat(parts[0].replaceAll("[^\\d.]", ""));
                        float max = Float.parseFloat(parts[1].replaceAll("[^\\d.]", ""));

                        if (actualValue >= min && actualValue <= max) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Log.e("HealthDataTable", "Error parsing gender-specific value", e);
            return false;
        }
    }

    /**
     * Checks if electrolyte levels are normal
     */
    private boolean isElectrolyteNormal(String value, String normalRange) {
        try {
            // This is a simplified implementation - would need more context on how
            // electrolyte values are formatted in the application
            String[] valueParts = value.split(",");
            String[] rangeParts = normalRange.split(",");

            // Simplified check - assumes values are in same order as ranges
            for (int i = 0; i < Math.min(valueParts.length, rangeParts.length); i++) {
                String singleValue = valueParts[i].trim();
                String singleRange = rangeParts[i].trim();

                if (!isInSimpleRange(singleValue, singleRange)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("HealthDataTable", "Error parsing electrolyte values", e);
            return false;
        }
    }

    /**
     * Checks if a value is within a simple min-max range
     */
    private boolean isInSimpleRange(String value, String normalRange) {
        try {
            float actualValue = Float.parseFloat(value.replaceAll("[^\\d.]", ""));
            String[] parts = normalRange.split("-");
            if (parts.length == 2) {
                float min = Float.parseFloat(parts[0].replaceAll("[^\\d.]", ""));
                float max = Float.parseFloat(parts[1].replaceAll("[^\\d.]", ""));
                return actualValue >= min && actualValue <= max;
            }
            return false;
        } catch (Exception e) {
            Log.e("HealthDataTable", "Error parsing simple range", e);
            return false;
        }
    }

    /**
     * Checks if a value is within a threshold (less than or greater than)
     */
    private boolean isWithinThreshold(String value, String normalRange) {
        try {
            float actualValue = Float.parseFloat(value.replaceAll("[^\\d.]", ""));
            float thresholdValue = Float.parseFloat(normalRange.replaceAll("[^\\d.]", ""));

            if (normalRange.startsWith("<")) {
                return actualValue < thresholdValue;
            } else if (normalRange.startsWith(">")) {
                return actualValue > thresholdValue;
            }
            return false;
        } catch (Exception e) {
            Log.e("HealthDataTable", "Error parsing threshold value", e);
            return false;
        }
    }


    private void updateProgress(int progress) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setProgress(progress);
        }
    }

    private void showReportCompletionDialog(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Generated");
        builder.setMessage("Your health report has been successfully generated and saved to your Downloads folder.");
        builder.setPositiveButton("View Report", (dialog, which) -> {
            openPdfFile(file);
        });
        builder.setNegativeButton("Close", null);
        builder.show();

        // Also show notification
        showDownloadNotification(file.getAbsolutePath());
    }

    private void openPdfFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening PDF: " + e.getMessage());
            Toast.makeText(this, "Unable to open PDF. Please check if you have a PDF viewer installed.", Toast.LENGTH_LONG).show();
        }
    }

    public void showDownloadNotification(String filePath) {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "health_report_channel",
                    "Health Reports",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Notifications for Aarogya Nidaan health reports");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        File file = new File(filePath);
        Uri uri = FileProvider.getUriForFile(
                this,
                getApplicationContext().getPackageName() + ".fileprovider",
                file
        );

        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(uri, "application/pdf");
        viewIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "health_report_channel")
                .setSmallIcon(R.drawable.aarogyanidaanlogo)
                .setContentTitle("Aarogya Nidaan Health Report")
                .setContentText("Your health report is ready. Tap to view.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, we'll just skip showing the notification
            // The user will still be able to access the report from the completion dialog
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void setEditingMode(boolean editing) {
        isEditing = editing;
        clinicalNotes.setEnabled(editing);
        clinicalNotessave.setEnabled(editing);
        clinicalNotesedit.setEnabled(!editing);
        clinicalNotesmic.setEnabled(editing);

        if (editing) {
            clinicalNotes.requestFocus();
        }
    }

    private void setupClinicalNotesListener() {
        if (clinicalNotesListener != null && healthDataRef != null) {
            healthDataRef.removeEventListener(clinicalNotesListener);
        }

        clinicalNotesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isEditing) {
                    String notes = snapshot.child("clinicalNotes").getValue(String.class);
                    if (notes != null && !notes.equals(currentNotes)) {
                        currentNotes = notes;
                        clinicalNotes.setText(notes);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read clinical notes: " + error.getMessage());
            }
        };

        if (healthDataRef != null) {
            healthDataRef.addValueEventListener(clinicalNotesListener);
        }
    }

    private void saveClinicalNotes(String notes) {
        if (patientId == null || TextUtils.isEmpty(notes)) {
            Toast.makeText(this, "Unable to save notes", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("clinicalNotes", notes);
        updates.put("clinicalNotesLastUpdate", ServerValue.TIMESTAMP);

        healthDataRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    currentNotes = notes;
                    Toast.makeText(this, "Notes saved successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving notes: " + e.getMessage());
                    Toast.makeText(this, "Failed to save notes", Toast.LENGTH_SHORT).show();
                    setEditingMode(true);
                });
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to add clinical notes");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                String currentText = clinicalNotes.getText().toString();
                String updatedText = TextUtils.isEmpty(currentText) ?
                        spokenText : currentText + "\n" + spokenText;

                clinicalNotes.setText(updatedText);
                setEditingMode(true);
            }
        }
    }

    private void setupConversationListener() {
        conversationRef = FirebaseDatabase.getInstance().getReference()
                .child("conversations").child(conversationId);

        conversationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String newPatientId = snapshot.child("patientId").getValue(String.class);
                    String doctorName = snapshot.child("doctorName").getValue(String.class);
                    String patientName = snapshot.child("patientName").getValue(String.class);

                    doctorNameText.setText(doctorName != null ? doctorName : "N/A");

                    if (newPatientId != null && !newPatientId.equals(patientId)) {
                        patientId = newPatientId;
                        patientIdText.setText(patientId);

                        setupHealthDataReferences();
                        setupPatientListener();
                        updatePatientAge(patientId); // <-- Call the age calculation method
                    }

                    if (patientName != null) {
                        patientNameText.setText(patientName);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Conversation listener cancelled: " + error.getMessage());
            }
        };

        conversationRef.addValueEventListener(conversationListener);
    }

    private void setupHealthDataReferences() {
        healthDataRef = FirebaseDatabase.getInstance().getReference()
                .child("patient_health_data").child(patientId);
        setupHealthDataListener();
        setupClinicalNotesListener();
    }

    private void setupHealthDataListener() {
        if (healthDataListener != null) {
            healthDataRef.removeEventListener(healthDataListener);
        }

        healthDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateHealthDataUI(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Health data listener cancelled: " + error.getMessage());
                setDefaultValues();
            }
        };

        healthDataRef.addValueEventListener(healthDataListener);
    }

    private void setupPatientListener() {
        patientRef = FirebaseDatabase.getInstance().getReference()
                .child("patients").child(patientId);

        patientListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("patient_name").getValue(String.class);
                    String dob = snapshot.child("patient_dob").getValue(String.class);

                    if (name != null) {
                        patientNameText.setText(name);
                    }

                    if (dob != null) {
                        updatePatientAge(dob);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Patient listener cancelled: " + error.getMessage());
            }
        };

        patientRef.addValueEventListener(patientListener);
    }

    private void updatePatientAge(String patientId) {
        if (patientId == null || patientId.isEmpty()) {
            patientage.setText("Invalid patient ID");
            return;
        }

        DatabaseReference reference = FirebaseDatabase.getInstance()
                .getReference("patient")
                .child(patientId);

        reference.child("patient_dob").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String birthdate = task.getResult().getValue(String.class);
                if (birthdate != null) {
                    calculateAge(birthdate);
                } else {
                    patientage.setText("DOB not available");
                }
            } else {
                Log.e("Firebase", "Failed to fetch DOB: " + task.getException().getMessage());
                patientage.setText("Age not available");
            }
        });
    }

    private void calculateAge(String birthdate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(birthdate);
            Calendar birth = Calendar.getInstance();
            Calendar today = Calendar.getInstance();

            birth.setTime(birthDate);

            int years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
            int months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH);

            if (today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
                months--;
                if (months < 0) {
                    years--;
                    months += 12;
                }
            }

            String ageText = String.format(Locale.getDefault(), "%d years, %d months", years, months);
            patientage.setText(ageText);
        } catch (ParseException e) {
            Log.e("AgeCalculation", "Error parsing birthdate: " + e.getMessage());
            patientage.setText("Invalid DOB format");
        }
    }

    private void updateHealthDataUI(DataSnapshot snapshot) {
        Map<String, String> healthData = new HashMap<>();
        healthData.put("temperature", "°C");
        healthData.put("heartRate", " bpm");
        healthData.put("bloodPressure", " mmHg");
        healthData.put("oxygenSaturation", "%");
        healthData.put("respiratoryRate", " RPM");
        healthData.put("pulseRate", " BPM");
        healthData.put("sweatRate", " L/hr");
        healthData.put("sleepDuration", " hrs");
        healthData.put("pulseTransitTime", " ms");
        healthData.put("cholesterolLevels", " mg/dL");
        healthData.put("hemoglobinLevels", " g/dL");
        healthData.put("electrolyteLevels", " mmol/L");

        for (Map.Entry<String, String> entry : healthData.entrySet()) {
            String value = snapshot.child(entry.getKey()).getValue(String.class);
            String displayValue = (value != null ? value : "--") + entry.getValue();

            switch (entry.getKey()) {
                case "temperature":
                    temperatureValueText.setText(displayValue);
                    break;
                case "heartRate":
                    heartRateValueText.setText(displayValue);
                    break;
                case "bloodPressure":
                    bloodPressureValueText.setText(displayValue);
                    break;
                case "oxygenSaturation":
                    oxygenSaturationValueText.setText(displayValue);
                    break;
                case "respiratoryRate":
                    respiratoryRateValueText.setText(displayValue);
                    break;
                case "pulseRate":
                    pulseRateValueText.setText(displayValue);
                    break;
                case "sweatRate":
                    sweatRateValueText.setText(displayValue);
                    break;
                case "sleepDuration":
                    sleepDurationValueText.setText(displayValue);
                    break;
                case "pulseTransitTime":
                    pulseTransitTimeValueText.setText(displayValue);
                    break;
                case "cholesterolLevels":
                    cholesterollevelsValueText.setText(displayValue);
                    break;
                case "hemoglobinLevels":
                    hemoglobinlevelsValueText.setText(displayValue);
                    break;
                case "electrolyteLevels":
                    electrolytelevelsValueText.setText(displayValue);
                    break;
            }
        }
    }

    private void setDefaultValues() {
        temperatureValueText.setText("--°C");
        heartRateValueText.setText("-- bpm");
        bloodPressureValueText.setText("-- mmHg");
        oxygenSaturationValueText.setText("--%");
        respiratoryRateValueText.setText("-- RPM");
        pulseRateValueText.setText("-- BPM");
        sweatRateValueText.setText("-- L/hr");
        sleepDurationValueText.setText("-- hrs");
        pulseTransitTimeValueText.setText("-- ms");
        cholesterollevelsValueText.setText("-- mg/dL");
        hemoglobinlevelsValueText.setText("-- g/dL");
        electrolytelevelsValueText.setText("-- mmol/L");
    }

    private void startDateTimeUpdates() {
        dateTimeTimer = new Timer();
        dateTimeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> updateDateTime());
            }
        }, 0, 1000);
    }

    private void updateDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        Date now = new Date();

        reportDate.setText(dateFormat.format(now));
        reportTime.setText(timeFormat.format(now));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("clinical_notes", clinicalNotes.getText().toString());
        outState.putBoolean("is_editing", isEditing);
        outState.putString("current_notes", currentNotes);
        outState.putString("patient_id", patientId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String savedNotes = savedInstanceState.getString("clinical_notes", "");
        isEditing = savedInstanceState.getBoolean("is_editing", false);
        currentNotes = savedInstanceState.getString("current_notes", "");
        patientId = savedInstanceState.getString("patient_id", null);

        clinicalNotes.setText(savedNotes);
        setEditingMode(isEditing);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isEditing && !TextUtils.isEmpty(clinicalNotes.getText())) {
            saveClinicalNotes(clinicalNotes.getText().toString().trim());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupListeners();
        stopDateTimeUpdates();
    }

    private void cleanupListeners() {
        if (healthDataRef != null && healthDataListener != null) {
            healthDataRef.removeEventListener(healthDataListener);
        }
        if (conversationRef != null && conversationListener != null) {
            conversationRef.removeEventListener(conversationListener);
        }
        if (patientRef != null && patientListener != null) {
            patientRef.removeEventListener(patientListener);
        }
        if (clinicalNotesListener != null && healthDataRef != null) {
            healthDataRef.removeEventListener(clinicalNotesListener);
        }
    }

    private void stopDateTimeUpdates() {
        if (dateTimeTimer != null) {
            dateTimeTimer.cancel();
            dateTimeTimer.purge();
            dateTimeTimer = null;
        }
    }
}