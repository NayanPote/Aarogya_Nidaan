package com.healthcare.aarogyanidaan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class registeroptions extends AppCompatActivity {

    Button doctoroptionregister;
    Button patientoptionregister;
    RelativeLayout doctoroption, patientoption, admin;
    Animation topAnim, bottomAnim, leftright;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registeroptions);

        // Initialize the views

        doctoroptionregister = findViewById(R.id.doctoroptionregister);
        patientoptionregister = findViewById(R.id.patientoptionregister);
        doctoroption = findViewById(R.id.doctoroption);
        patientoption = findViewById(R.id.patientoption);
        admin = findViewById(R.id.admin);

        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation);
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);
        leftright = AnimationUtils.loadAnimation(this, R.anim.leftright);

        //animations
        doctoroption.setAnimation(leftright);
        patientoption.setAnimation(leftright);
        // Apply window insets listener
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Set click listeners for the buttons
        patientoptionregister.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), "Fill all details", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(registeroptions.this, patientregistrationform.class));
        });

        doctoroptionregister.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), "Fill all details", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(registeroptions.this, doctorregistrationform.class));
        });

        admin.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), "Fill all details", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(registeroptions.this, adminregistrationform.class));
        });

    }
}
