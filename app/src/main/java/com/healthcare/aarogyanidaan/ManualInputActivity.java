package com.healthcare.aarogyanidaan;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class ManualInputActivity extends AppCompatActivity {

    private DatabaseReference firebaseDatabase;
    private String currentPatientId;

    private EditText temperatureInput, heartRateInput,
            bloodPressureInput, oxygenSaturationInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_input);

        // Get patient ID passed from previous activity
        currentPatientId = getIntent().getStringExtra("PATIENT_ID");

        // Initialize Firebase Database
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("patient_health_data");

        // Initialize UI Components
        initializeUIComponents();
    }

    private void initializeUIComponents() {
        temperatureInput = findViewById(R.id.temperatureInput);
        heartRateInput = findViewById(R.id.heartRateInput);
        bloodPressureInput = findViewById(R.id.bloodPressureInput);
        oxygenSaturationInput = findViewById(R.id.oxygenSaturationInput);
    }

    private void saveManualHealthData() {
        // Create a map to store non-empty inputs
        Map<String, Object> healthData = new HashMap<>();

        // Add non-empty inputs to the map
        addInputIfNotEmpty(healthData, "temperature", temperatureInput);
        addInputIfNotEmpty(healthData, "heartRate", heartRateInput);
        addInputIfNotEmpty(healthData, "bloodPressure", bloodPressureInput);
        addInputIfNotEmpty(healthData, "oxygenSaturation", oxygenSaturationInput);

        // Check if any data was entered
        if (!healthData.isEmpty()) {
            DatabaseReference patientRef = firebaseDatabase.child(currentPatientId);
            patientRef.updateChildren(healthData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No data entered", Toast.LENGTH_SHORT).show();
        }
    }

    private void addInputIfNotEmpty(Map<String, Object> dataMap, String key, EditText editText) {
        String value = editText.getText().toString().trim();
        if (!value.isEmpty()) {
            dataMap.put(key, value);
        }
    }
}