package com.healthcare.aarogyanidaan;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class patientprofile extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference reference;
    private DatabaseReference chatRequestsRef;
    private DatabaseReference conversationsRef;
    private TextView patientId, patientName, patientEmail, patientPhone,
            patientGender, patientCity, patientDob;
    private SimpleDateFormat dateFormatter;
    private String selectedGender = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patientprofile);

        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        initializeUIComponents();
        setupFirebase();
        setupListeners();
    }

    private void initializeUIComponents() {
        patientId = findViewById(R.id.patientid);
        patientName = findViewById(R.id.patientname);
        patientEmail = findViewById(R.id.patientemail);
        patientPhone = findViewById(R.id.patientphone);
        patientGender = findViewById(R.id.patientgender);
        patientCity = findViewById(R.id.patientcity);
        patientDob = findViewById(R.id.patientdob);
        ImageButton editButton = findViewById(R.id.editbutton);
        ImageButton backButton = findViewById(R.id.backbutton);
        ImageButton copyPatientId = findViewById(R.id.copypatientid);
        Button patientdeleteprofile = findViewById(R.id.patientdeleteprofile);

        editButton.setOnClickListener(v -> showEditDialog());
        backButton.setOnClickListener(v -> onBackPressed());
        patientdeleteprofile.setOnClickListener(v -> showDeleteDialog());
        copyPatientId.setOnClickListener(v -> copyToClipboard(patientId.getText().toString()));
    }

    private void copyToClipboard(String patientId) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Patient ID", patientId);
        clipboard.setPrimaryClip(clip);

        // Show a toast message for user feedback
        Toast.makeText(this, "Patient ID copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle("DELETE account?")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Show re-authentication dialog before deleting
                    showReAuthenticationForDelete();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showReAuthenticationForDelete() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_reauth, null);
        builder.setView(dialogView);

        final EditText passwordInput = dialogView.findViewById(R.id.password_input);

        AlertDialog dialog = builder.setTitle("Re-authenticate")
                .setMessage("Please enter your current password to delete account")
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String password = passwordInput.getText().toString();
                if (TextUtils.isEmpty(password)) {
                    passwordInput.setError("Password required");
                    return;
                }

                FirebaseUser user = auth.getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

                    user.reauthenticate(credential)
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                deleteUserData();
                            })
                            .addOnFailureListener(e -> Toast.makeText(patientprofile.this,
                                    "Authentication failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                }
            });
        });

        dialog.show();
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize main references
        reference = FirebaseDatabase.getInstance()
                .getReference("patient")
                .child(currentUser.getUid());

        // Initialize chat-related references
        chatRequestsRef = FirebaseDatabase.getInstance().getReference("chat_requests");
        conversationsRef = FirebaseDatabase.getInstance().getReference("conversations");
    }

    private void deleteUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting account...");
        progressDialog.show();

        // Create a list to store all delete operations
        Map<String, Object> updates = new HashMap<>();

        // 1. Delete chat requests
        chatRequestsRef.orderByChild("patientId").equalTo(userId)
                .get().addOnSuccessListener(dataSnapshot -> {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        updates.put("/chat_requests/" + snapshot.getKey(), null);
                    }

                    // 2. Delete conversations
                    conversationsRef.orderByChild("patientId").equalTo(userId)
                            .get().addOnSuccessListener(convSnapshot -> {
                                for (DataSnapshot snapshot : convSnapshot.getChildren()) {
                                    updates.put("/conversations/" + snapshot.getKey(), null);
                                }

                                // 3. Delete patient profile and health data
                                updates.put("/patient/" + userId, null);
                                updates.put("/patient_health_data/" + userId, null);

                                // Perform all deletions in a single atomic operation
                                FirebaseDatabase.getInstance().getReference()
                                        .updateChildren(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            // After database cleanup, delete the user account
                                            user.delete()
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        progressDialog.dismiss();
                                                        Toast.makeText(patientprofile.this,
                                                                "Account deleted successfully",
                                                                Toast.LENGTH_SHORT).show();
                                                        navigateToLogin();
                                                    })
                                                    .addOnFailureListener(e -> handleDeletionError(e, progressDialog));
                                        })
                                        .addOnFailureListener(e -> handleDeletionError(e, progressDialog));
                            })
                            .addOnFailureListener(e -> handleDeletionError(e, progressDialog));
                })
                .addOnFailureListener(e -> handleDeletionError(e, progressDialog));
    }
    private void handleDeletionError(Exception e, ProgressDialog progressDialog) {
        progressDialog.dismiss();
        Toast.makeText(patientprofile.this,
                "Error deleting account: " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(patientprofile.this, loginpage.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_patient_edit_profile, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editpatientname);
        TextView editEmail = dialogView.findViewById(R.id.editpatientemail);
        EditText editPhone = dialogView.findViewById(R.id.editpatientphone);
        EditText editCity = dialogView.findViewById(R.id.editpatientcity);
        EditText editDob = dialogView.findViewById(R.id.editpatientdob);
        RadioGroup editGender = dialogView.findViewById(R.id.editpatientgender);

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    editName.setText(snapshot.child("patient_name").getValue(String.class));
                    editEmail.setText(snapshot.child("patient_email").getValue(String.class));
                    editPhone.setText(snapshot.child("patient_contactno").getValue(String.class));
                    editCity.setText(snapshot.child("patient_city").getValue(String.class));
                    editDob.setText(snapshot.child("patient_dob").getValue(String.class));

                    String gender = snapshot.child("patient_gender").getValue(String.class);
                    if (gender != null) {
                        selectedGender = gender;
                        switch (gender) {
                            case "Male":
                                editGender.check(R.id.rb_pmale);
                                break;
                            case "Female":
                                editGender.check(R.id.rb_pfemale);
                                break;
                            case "Other":
                                editGender.check(R.id.rb_pother);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(patientprofile.this,
                        "Error loading data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        editDob.setKeyListener(null);
        editDob.setOnClickListener(v -> showDatePicker(editDob));

        editGender.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_pmale) {
                selectedGender = "Male";
            } else if (checkedId == R.id.rb_pfemale) {
                selectedGender = "Female";
            } else if (checkedId == R.id.rb_pother) {
                selectedGender = "Other";
            }
        });

        AlertDialog dialog = builder.setTitle("Edit Profile")
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (validateData(editName, editPhone, editCity, editDob)) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("patient_name", editName.getText().toString().trim());
                    updates.put("patient_contactno", editPhone.getText().toString().trim());
                    updates.put("patient_city", editCity.getText().toString().trim());
                    updates.put("patient_dob", editDob.getText().toString().trim());
                    updates.put("patient_gender", selectedGender);

                    savePatientData(updates);
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showDatePicker(EditText dateField) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, monthOfYear, dayOfMonth);

                    if (selectedDate.after(Calendar.getInstance())) {
                        Toast.makeText(this, "Cannot select future date", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            dayOfMonth, (monthOfYear + 1), year1);
                    dateField.setText(date);
                },
                year, month, day
        );

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private boolean validateData(EditText name, EditText phone,
                                 EditText city, EditText dob) {
        if (TextUtils.isEmpty(name.getText())) {
            name.setError("Name is required");
            return false;
        }

        String phoneStr = phone.getText().toString().trim();
        if (TextUtils.isEmpty(phoneStr)) {
            phone.setError("Phone number is required");
            return false;
        }
        if (!phoneStr.matches("^[0-9]{10}$")) {
            phone.setError("Invalid phone number");
            return false;
        }

        if (TextUtils.isEmpty(city.getText())) {
            city.setError("City is required");
            return false;
        }

        String dobStr = dob.getText().toString().trim();
        if (TextUtils.isEmpty(dobStr)) {
            dob.setError("Date of birth is required");
            return false;
        }
        if (!isValidDate(dobStr)) {
            dob.setError("Invalid date");
            return false;
        }

        if (selectedGender.isEmpty()) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidDate(String dateStr) {
        try {
            Date date = dateFormatter.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            Calendar now = Calendar.getInstance();
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -150);

            return !cal.after(now) && !cal.before(minDate);
        } catch (ParseException e) {
            return false;
        }
    }

    private void savePatientData(Map<String, Object> patientData) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        String newName = (String) patientData.get("patient_name");

        // Update patient name in conversations and chat requests
        if (newName != null) {
            // Update chat requests
            chatRequestsRef.orderByChild("patientId").equalTo(userId)
                    .get().addOnSuccessListener(dataSnapshot -> {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            chatRequestsRef.child(snapshot.getKey())
                                    .child("patientName")
                                    .setValue(newName);
                        }
                    });

            // Update conversations
            conversationsRef.orderByChild("patientId").equalTo(userId)
                    .get().addOnSuccessListener(dataSnapshot -> {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            conversationsRef.child(snapshot.getKey())
                                    .child("patientName")
                                    .setValue(newName);
                        }
                    });
        }

        // Continue with normal profile update
        String newEmail = (String) patientData.get("patient_email");
        if (newEmail != null && !newEmail.equals(user.getEmail())) {
            showReAuthenticationDialog(newEmail, patientData);
        } else {
            updateDatabase(userId, patientData);
        }
    }

    private void showReAuthenticationDialog(String newEmail, Map<String, Object> patientData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_reauth, null);
        builder.setView(dialogView);

        final EditText passwordInput = dialogView.findViewById(R.id.password_input);

        AlertDialog dialog = builder.setTitle("Re-authenticate")
                .setMessage("Please enter your current password to update email")
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String password = passwordInput.getText().toString();
                if (TextUtils.isEmpty(password)) {
                    passwordInput.setError("Password required");
                    return;
                }

                // Get credentials and re-authenticate
                FirebaseUser user = auth.getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

                    // Show progress dialog
                    ProgressDialog progressDialog = new ProgressDialog(this);
                    progressDialog.setMessage("Updating profile...");
                    progressDialog.show();

                    user.reauthenticate(credential)
                            .addOnSuccessListener(aVoid -> {
                                // After re-authentication, update email
                                user.updateEmail(newEmail)
                                        .addOnSuccessListener(aVoid1 -> {
                                            updateDatabase(user.getUid(), patientData);
                                            progressDialog.dismiss();
                                            dialog.dismiss();
                                        })
                                        .addOnFailureListener(e -> {
                                            progressDialog.dismiss();
                                            Toast.makeText(patientprofile.this,
                                                    "Failed to update email: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(patientprofile.this,
                                        "Authentication failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                }
            });
        });

        dialog.show();
    }

    private void updateDatabase(String userId, Map<String, Object> patientData) {
        reference.updateChildren(patientData)
                .addOnSuccessListener(aVoid -> Toast.makeText(this,
                        "Profile updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to update profile: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void setupListeners() {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    updateUI(snapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(patientprofile.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(DataSnapshot snapshot) {
        String id = snapshot.child("patient_id").getValue(String.class);
        String name = snapshot.child("patient_name").getValue(String.class);
        String email = snapshot.child("patient_email").getValue(String.class);
        String phone = snapshot.child("patient_contactno").getValue(String.class);
        String gender = snapshot.child("patient_gender").getValue(String.class);
        String city = snapshot.child("patient_city").getValue(String.class);
        String dob = snapshot.child("patient_dob").getValue(String.class);

        patientId.setText(id != null ? id : "Not set");
        patientName.setText(name != null ? name : "Not set");
        patientEmail.setText(email != null ? email : "Not set");
        patientPhone.setText(phone != null ? phone : "Not set");
        patientGender.setText(gender != null ? gender : "Not set");
        patientCity.setText(city != null ? city : "Not set");
        patientDob.setText(dob != null ? dob : "Not set");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (auth.getCurrentUser() != null) {
            setupListeners();
        }
    }
}