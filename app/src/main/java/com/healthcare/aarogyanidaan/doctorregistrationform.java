package com.healthcare.aarogyanidaan;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.healthcare.aarogyanidaan.databinding.ActivityDoctorregistrationformBinding;

import java.util.Random;

public class doctorregistrationform extends AppCompatActivity {

    private ActivityDoctorregistrationformBinding binding;
    private boolean isRegistrationCompleted = false;
    private String selectedGender = "";
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private ProgressDialog progressDialog;
    private boolean isEmailVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoctorregistrationformBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupProgressDialog();
        setupListeners();
        setupDatePicker();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.main));
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    private void setupListeners() {
        // Email verification button
        binding.btnVerifyEmail.setOnClickListener(v -> sendEmailVerification());

        // Register button
        binding.doctorbuttonregister.setOnClickListener(v -> validateAndRegister());

        // Gender selection
        binding.rgPgender.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_pmale) selectedGender = "Male";
            else if (checkedId == R.id.rb_pfemale) selectedGender = "Female";
            else if (checkedId == R.id.rb_pother) selectedGender = "Other";
        });

        // Already have account link
        binding.alreadyaccount.setOnClickListener(v -> {
            startActivity(new Intent(doctorregistrationform.this, loginpage.class));
            finish();
        });

        // Back button
        binding.backbutton.setOnClickListener(v -> onBackPressed());

        // Terms and conditions link
        binding.doctortermsandcondition.setOnClickListener(v -> {
            startActivity(new Intent(doctorregistrationform.this, doctortermsandcondition.class));
        });

        // Profile image changer
        binding.fabChangeImage.setOnClickListener(v -> {
            // Implement image picker functionality here
            Toast.makeText(this, "Profile image change feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Need help button
        binding.fabHelp.setOnClickListener(v -> {
            Toast.makeText(this, "Help center will be available soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDatePicker() {
        binding.etSetdoctordob.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String date = String.format("%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                        binding.etSetdoctordob.setText(date);
                    },
                    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
                    java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void sendEmailVerification() {
        String email = binding.enterdoctoremail.getText().toString().trim();
        if (!validateEmail(email)) return;

        progressDialog.show();
        auth.createUserWithEmailAndPassword(email, "temp" + new Random().nextInt(1000))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        user.sendEmailVerification()
                                .addOnCompleteListener(emailTask -> {
                                    progressDialog.dismiss();
                                    if (emailTask.isSuccessful()) {
                                        Toast.makeText(this, "Verification email sent. Please check your inbox.", Toast.LENGTH_LONG).show();
                                        startEmailVerificationCheckLoop();
                                    } else {
                                        Toast.makeText(this, "Failed to send verification email: " + emailTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startEmailVerificationCheckLoop() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    isEmailVerified = true;
                    binding.btnVerifyEmail.setText("Email Verified ✓");
                    binding.btnVerifyEmail.setTextColor(
                            ContextCompat.getColor(this, android.R.color.holo_green_dark)
                    );
                    binding.btnVerifyEmail.setEnabled(false);
                    binding.enterdoctoremail.setEnabled(false);
                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    new android.os.Handler().postDelayed(
                            this::startEmailVerificationCheckLoop,
                            5000
                    );
                }
            });
        }
    }

    private void validateAndRegister() {
        if (!isEmailVerified) {
            binding.btnVerifyEmail.setTextColor(
                    ContextCompat.getColor(this, android.R.color.holo_red_dark)
            );
            Toast.makeText(this, "Please verify your email before registering", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = binding.enterdoctorname.getText().toString().trim();
        String email = binding.enterdoctoremail.getText().toString().trim();
        String contactno = binding.enterdoctorcontactno.getText().toString().trim();
        String specialization = binding.setdoctorspecialization.getText().toString().trim();
        String city = binding.enterdoctorcity.getText().toString().trim();
        String password = binding.setdoctorpassword.getText().toString().trim();
        String confirmpassword = binding.etConfirmPassword.getText().toString().trim();
        String dob = binding.etSetdoctordob.getText().toString().trim();

        if (!validateAllFields(name, email, contactno, specialization, city, password, confirmpassword, dob)) {
            return;
        }

        // Validate terms and conditions checkbox
        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        registerUser(name, email, contactno, specialization, city, password, dob);
    }

    private boolean validateAllFields(String name, String email, String contactno, String specialization,
                                      String city, String password, String confirmpassword, String dob) {
        if (name.isEmpty() || email.isEmpty() || contactno.isEmpty() || specialization.isEmpty() ||
                city.isEmpty() || password.isEmpty() || confirmpassword.isEmpty() || dob.isEmpty() ||
                selectedGender.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!validatePhone(contactno)) return false;
        if (!validatePassword(password, confirmpassword)) return false;
        if (!validateDOB(dob)) return false;

        return true;
    }

    private boolean validateEmail(String email) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.enterdoctoremail.setError("Invalid email address");
            return false;
        }
        return true;
    }

    private boolean validatePhone(String phone) {
        if (phone.length() != 10) {
            binding.enterdoctorcontactno.setError("Invalid phone number");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password, String confirmPassword) {
        if (password.length() < 8) {
            binding.setdoctorpassword.setError("Password must be at least 8 characters");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            binding.setdoctorpassword.setError("Password must contain at least one uppercase letter");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            binding.setdoctorpassword.setError("Password must contain at least one lowercase letter");
            return false;
        }
        if (!password.matches(".*[!@#$%^&*()].*")) {
            binding.setdoctorpassword.setError("Password must contain at least one special character");
            return false;
        }
        if (!password.matches(".*[0-9].*")) {
            binding.setdoctorpassword.setError("Password must contain at least one number");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    private boolean validateDOB(String dob) {
        if (!dob.matches("\\d{2}/\\d{2}/\\d{4}")) {
            binding.etSetdoctordob.setError("Invalid date format");
            return false;
        }
        return true;
    }

    private void registerUser(String name, String email, String contactno, String specialization,
                              String city, String password, String dob) {
        progressDialog.show();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUser.updatePassword(password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            saveUserToDatabase(currentUser.getUid(), name, email, contactno, selectedGender,
                                    specialization, dob, city, password);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Failed to update password: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveUserToDatabase(String doctorId, String name, String email, String contactno,
                                    String gender, String specialization, String dob, String city, String password) {
        DatabaseReference reference = database.getReference().child("doctor").child(doctorId);

        Users doctor = new Users();
        doctor.setDoctor_id(doctorId);
        doctor.setDoctor_name(name);
        doctor.setDoctor_email(email);
        doctor.setDoctor_contactno(contactno);
        doctor.setDoctor_gender(gender);
        doctor.setDoctor_specialization(specialization);
        doctor.setDoctor_dob(dob);
        doctor.setDoctor_city(city);
        doctor.setDoctor_password(password);
        doctor.setEmailVerified(true);

        // Save biometric preference
        doctor.setBiometricEnabled(binding.switchBiometric.isChecked());

        reference.setValue(doctor)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        isRegistrationCompleted = true;  // Mark registration as completed
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(doctorregistrationform.this, doctordashboard.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save user data: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cleanupIncompleteRegistration() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && !isRegistrationCompleted) {
            user.delete()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(doctorregistrationform.this,
                                    "Registration failed. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupIncompleteRegistration();
        binding = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        cleanupIncompleteRegistration();
    }

    @Override
    public void onBackPressed() {
        cleanupIncompleteRegistration();
        super.onBackPressed();
    }
}