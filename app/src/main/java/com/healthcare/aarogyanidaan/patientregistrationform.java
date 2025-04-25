package com.healthcare.aarogyanidaan;
import com.healthcare.aarogyanidaan.SupabaseConfig;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.healthcare.aarogyanidaan.databinding.ActivityPatientregistrationformBinding;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.UUID;

//import io.supabase.postgrest.PostgrestClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class patientregistrationform extends AppCompatActivity {
    private ActivityPatientregistrationformBinding binding;
    private String selectedGender = "";
    private boolean isRegistrationCompleted = false;
    private boolean isEmailVerified = false;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private ProgressDialog progressDialog;
    private Uri imageUri = null;
    private String supabaseImageUrl = null;

    // Supabase configuration
    private static final String SUPABASE_URL = SupabaseConfig.SUPABASE_URL;
    private static final String SUPABASE_ANON_KEY = SupabaseConfig.SUPABASE_ANON_KEY;
    private static final String SUPABASE_STORAGE_BUCKET = SupabaseConfig.SUPABASE_STORAGE_BUCKET;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    binding.profileImage.setImageURI(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPatientregistrationformBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupFirebase();
        setupListeners();
        setupDatePicker();
        setupBloodGroupDropdown();
        setupProgressDialog();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.main));
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    private void setupListeners() {
        // Profile image change
        binding.fabChangeImage.setOnClickListener(v -> openImagePicker());

        // Email verification button
        binding.btnVerifyEmail.setOnClickListener(v -> sendEmailVerification());

        // Register button
        binding.patientbuttonregister.setOnClickListener(v -> validateAndRegister());

        // Gender selection
        binding.rgPgender.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_pmale) selectedGender = "Male";
            else if (checkedId == R.id.rb_pfemale) selectedGender = "Female";
            else if (checkedId == R.id.rb_pother) selectedGender = "Other";
        });

        // Already have account link
        binding.alreadyaccount.setOnClickListener(v -> {
            startActivity(new Intent(patientregistrationform.this, loginpage.class));
            finish();
        });

        // Back button
        binding.backbutton.setOnClickListener(v -> onBackPressed());

        // Terms and conditions link
        binding.patienttermsandcondition.setOnClickListener(v -> {
            startActivity(new Intent(patientregistrationform.this, patienttermsandcondition.class));
        });

        // Need Help button
        binding.fabHelp.setOnClickListener(v -> {
            Toast.makeText(this, "Help Center will open", Toast.LENGTH_SHORT).show();
            // Implement help functionality here
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Profile Image"));
    }

    private void setupBloodGroupDropdown() {
        String[] bloodGroups = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, bloodGroups);
        binding.etBloodGroup.setAdapter(adapter);
    }

    private void setupDatePicker() {
        binding.etSetpatientdob.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        String date = String.format("%02d/%02d/%04d", dayOfMonth, (month + 1), year);
                        binding.etSetpatientdob.setText(date);
                    },
                    java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
                    java.util.Calendar.getInstance().get(java.util.Calendar.MONTH),
                    java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void sendEmailVerification() {
        String email = binding.enterpatientemail.getText().toString().trim();
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
                    binding.enterpatientemail.setEnabled(false);
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

        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = binding.enterpatientname.getText().toString().trim();
        String email = binding.enterpatientemail.getText().toString().trim();
        String contactno = binding.enterpatientcontactno.getText().toString().trim();
        String city = binding.enterpatientcity.getText().toString().trim();
        String password = binding.setpatientpassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        String dob = binding.etSetpatientdob.getText().toString().trim();
        String bloodGroup = binding.etBloodGroup.getText().toString().trim();
        String allergies = binding.etAllergies.getText().toString().trim();
        String emergencyContact = binding.etEmergencyContact.getText().toString().trim();
        String medicalHistory = binding.etMedicalHistory.getText().toString().trim();
        boolean enableBiometric = binding.switchBiometric.isChecked();

        if (!validateAllFields(name, email, contactno, city, password, confirmPassword, dob, bloodGroup, emergencyContact)) {
            return;
        }

        progressDialog.show();
        progressDialog.setMessage("Uploading image...");

        if (imageUri != null) {
            uploadImageToSupabase(imageUri, name, email, contactno, city, password, dob, bloodGroup, allergies, emergencyContact, medicalHistory, enableBiometric);
        } else {
            // No image selected, continue with registration
            registerUser(name, email, contactno, city, password, dob, bloodGroup, allergies, emergencyContact, medicalHistory, enableBiometric, null);
        }
    }

    private void uploadImageToSupabase(Uri imageUri, String name, String email, String contactno, String city,
                                       String password, String dob, String bloodGroup, String allergies,
                                       String emergencyContact, String medicalHistory, boolean enableBiometric) {
        // Create a unique file name for the image
        String fileName = UUID.randomUUID().toString() + ".jpg";

        // Execute network operation in background thread
        new Thread(() -> {
            try {
                // Get image bytes
                byte[] imageBytes = getImageBytes(imageUri);

                // Configure Supabase storage client
                OkHttpClient client = new OkHttpClient();

                // Create request body
                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName,
                                RequestBody.create(MediaType.parse("image/jpeg"), imageBytes))
                        .build();

                // Create request
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/storage/v1/object/" + SUPABASE_STORAGE_BUCKET + "/" + fileName)
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .post(requestBody)
                        .build();

                // Execute request
                Response response = client.newCall(request).execute();

                // Check response
                if (response.isSuccessful()) {
                    // Create the public URL for the uploaded image
                    String imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + SUPABASE_STORAGE_BUCKET + "/" + fileName;

                    // Update UI thread
                    runOnUiThread(() -> {
                        progressDialog.setMessage("Registering user...");
                        registerUser(name, email, contactno, city, password, dob, bloodGroup, allergies, emergencyContact, medicalHistory, enableBiometric, imageUrl);
                    });
                } else {
                    // Handle error
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(patientregistrationform.this,
                                "Failed to upload image: " + response.message(),
                                Toast.LENGTH_SHORT).show();
                        // Continue registration without image
                        registerUser(name, email, contactno, city, password, dob, bloodGroup, allergies, emergencyContact, medicalHistory, enableBiometric, null);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(patientregistrationform.this,
                            "Error uploading image: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    // Continue registration without image
                    registerUser(name, email, contactno, city, password, dob, bloodGroup, allergies, emergencyContact, medicalHistory, enableBiometric, null);
                });
            }
        }).start();
    }

    private byte[] getImageBytes(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);

        // Compress image to reduce size
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);

        return baos.toByteArray();
    }

    private boolean validateAllFields(String name, String email, String contactno, String city,
                                      String password, String confirmPassword, String dob,
                                      String bloodGroup, String emergencyContact) {
        if (name.isEmpty() || email.isEmpty() || contactno.isEmpty() || city.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty() || dob.isEmpty() || selectedGender.isEmpty()) {
            Toast.makeText(this, "All required fields must be filled", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (bloodGroup.isEmpty()) {
            binding.etBloodGroup.setError("Blood group is required");
            return false;
        }

        if (emergencyContact.isEmpty()) {
            binding.etEmergencyContact.setError("Emergency contact is required");
            return false;
        }

        if (!validatePhone(contactno)) return false;
        if (!validatePhone(emergencyContact)) {
            binding.etEmergencyContact.setError("Invalid emergency contact number");
            return false;
        }
        if (!validatePassword(password, confirmPassword)) return false;
        if (!validateDOB(dob)) return false;

        return true;
    }

    private boolean validateEmail(String email) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.enterpatientemail.setError("Invalid email address");
            return false;
        }
        return true;
    }

    private boolean validatePhone(String phone) {
        if (phone.length() != 10) {
            binding.enterpatientcontactno.setError("Phone number must be 10 digits");
            return false;
        }
        return true;
    }

    private boolean validatePassword(String password, String confirmPassword) {
        if (password.length() < 8) {
            binding.setpatientpassword.setError("Password must be at least 8 characters");
            return false;
        }
        if (!password.matches(".*[A-Z].*")) {
            binding.setpatientpassword.setError("Password must contain at least one uppercase letter");
            return false;
        }
        if (!password.matches(".*[a-z].*")) {
            binding.setpatientpassword.setError("Password must contain at least one lowercase letter");
            return false;
        }
        if (!password.matches(".*[!@#$%^&*()].*")) {
            binding.setpatientpassword.setError("Password must contain at least one special character");
            return false;
        }
        if (!password.matches(".*[0-9].*")) {
            binding.setpatientpassword.setError("Password must contain at least one number");
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
            binding.etSetpatientdob.setError("Invalid date format");
            return false;
        }
        return true;
    }

    private void registerUser(String name, String email, String contactno, String city,
                              String password, String dob, String bloodGroup, String allergies,
                              String emergencyContact, String medicalHistory, boolean enableBiometric,
                              String profileImageUrl) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUser.updatePassword(password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            saveUserToDatabase(currentUser.getUid(), name, email, contactno, selectedGender, dob, city,
                                    password, bloodGroup, allergies, emergencyContact, medicalHistory, enableBiometric, profileImageUrl);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Failed to update password: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveUserToDatabase(String patientId, String name, String email, String contactno,
                                    String gender, String dob, String city, String password,
                                    String bloodGroup, String allergies, String emergencyContact,
                                    String medicalHistory, boolean enableBiometric, String profileImageUrl) {
        DatabaseReference reference = database.getReference().child("patient").child(patientId);

        Users.PatientUser patient = new Users.PatientUser(patientId, name, email, contactno, gender, dob, city,
                password, bloodGroup, allergies, emergencyContact, medicalHistory);
        patient.setEmailVerified(true);
        patient.setPhoneVerified(true);
        patient.setEnableBiometric(enableBiometric);

        // Set the Supabase image URL instead of the local URI
        if (profileImageUrl != null) {
            patient.setProfileImageUrl(profileImageUrl);
        }

        reference.setValue(patient)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        isRegistrationCompleted = true;  // Mark registration as completed
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(patientregistrationform.this, patientdashboard.class);
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
                            Toast.makeText(patientregistrationform.this,
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