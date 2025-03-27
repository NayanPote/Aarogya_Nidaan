package com.healthcare.aarogyanidaan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;

import com.healthcare.aarogyanidaan.databinding.ActivityDoctorslistBinding;

public class Doctorslist extends AppCompatActivity {

    private ActivityDoctorslistBinding binding;
    private doctorlistadapter adapter;
    private ArrayList<Users> doctorList;
    private ArrayList<Users> filteredList;
    private FirebaseDatabase database;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDoctorslistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViews();
        setupRecyclerView();
        fetchDoctorsData();
        setupBackButton();
        setupSearchBar();
        setupChipGroup();
        setupFloatingActionButton();
        setupClearFiltersButton();
    }

    private void initializeViews() {
        auth = FirebaseAuth.getInstance();

        binding.backdoctorslist.setOnClickListener(v -> {
            startActivity(new Intent(Doctorslist.this, patientdashboard.class));
            finish();
        });
    }

    private void setupRecyclerView() {
        doctorList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new doctorlistadapter(this, filteredList);
        binding.topdoctorlist.setLayoutManager(new LinearLayoutManager(this));
        binding.topdoctorlist.setAdapter(adapter);

        // Set click listener for doctor selection
        adapter.setOnDoctorClickListener((doctor, position) -> {
            Intent intent = new Intent(Doctorslist.this, DoctorDetailsActivity.class);
            intent.putExtra("doctor_id", doctor.getDoctor_id());
            intent.putExtra("doctor_name", doctor.getDoctor_name());
            startActivity(intent);
        });
    }

    private void fetchDoctorsData() {
        showLoading(true);
        database = FirebaseDatabase.getInstance();
        DatabaseReference doctorsRef = database.getReference("doctor");

        doctorsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doctorList.clear();
                for (DataSnapshot doctorSnapshot : snapshot.getChildren()) {
                    Users doctor = doctorSnapshot.getValue(Users.class);
                    if (doctor != null) {
                        doctorList.add(doctor);
                    }
                }

                // Update the doctor count
                updateDoctorCount(doctorList.size());

                // Initially show all doctors in the filtered list
                filteredList.clear();
                filteredList.addAll(doctorList);
                updateUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                binding.doccount.setText("0");
                binding.doccount.setVisibility(View.VISIBLE);

                binding.emptyView.setVisibility(View.VISIBLE);
                binding.topdoctorlist.setVisibility(View.GONE);
            }
        });
    }

    private void updateDoctorCount(int count) {
        String countText = count + (count != 1 ? "+" : "");
        binding.doccount.setText(countText);
        binding.doccount.setVisibility(View.VISIBLE);
    }

    private void setupSearchBar() {
        binding.docsearchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBySearchText(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBySearchText(newText);
                return true;
            }
        });
    }

    // Search includes doctor name, specialization, and city
    private void filterBySearchText(String query) {
        ArrayList<Users> searchResults = new ArrayList<>();
        if (query.isEmpty()) {
            // If no search text, show all doctors or apply just the specialty filter
            applySpecialtyFilter();
        } else {
            // Apply both search text and specialty filter
            String selectedSpecialty = getSelectedSpecialty();
            boolean filterBySpecialty = !selectedSpecialty.equals("All");

            for (Users doctor : doctorList) {
                boolean matchesSearch = doctor.getDoctor_name().toLowerCase().contains(query.toLowerCase()) ||
                        doctor.getDoctor_specialization().toLowerCase().contains(query.toLowerCase()) ||
                        doctor.getDoctor_city().toLowerCase().contains(query.toLowerCase());

                boolean matchesSpecialty = !filterBySpecialty ||
                        doctor.getDoctor_specialization().equalsIgnoreCase(selectedSpecialty);

                if (matchesSearch && matchesSpecialty) {
                    searchResults.add(doctor);
                }
            }

            filteredList.clear();
            filteredList.addAll(searchResults);
            updateUI();

            // Update the count to show filtered results
            updateDoctorCount(filteredList.size());
        }
    }

    private void applySpecialtyFilter() {
        String selectedSpecialty = getSelectedSpecialty();
        filteredList.clear();

        if (selectedSpecialty.equals("All")) {
            filteredList.addAll(doctorList);
        } else {
            for (Users doctor : doctorList) {
                if (doctor.getDoctor_specialization().equalsIgnoreCase(selectedSpecialty)) {
                    filteredList.add(doctor);
                }
            }
        }

        // Update the count to show filtered results
        updateDoctorCount(filteredList.size());
        updateUI();
    }

    private String getSelectedSpecialty() {
        // Get the selected chip text
        for (int i = 0; i < binding.categoryChips.getChildCount(); i++) {
            Chip chip = (Chip) binding.categoryChips.getChildAt(i);
            if (chip.isChecked()) {
                return chip.getText().toString();
            }
        }
        return "All"; // Default if none selected
    }

    private void setupChipGroup() {
        binding.categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Apply filters when chip selection changes
            applySpecialtyFilter();
            // Also apply any existing search filter
            String currentQuery = binding.docsearchBar.getQuery().toString();
            if (!currentQuery.isEmpty()) {
                filterBySearchText(currentQuery);
            }
        });
    }

    private void setupFloatingActionButton() {
        binding.fabFilter.setOnClickListener(v -> {
            // Show filter dialog or bottom sheet
            showFilterDialog();
        });
    }

    private void showFilterDialog() {
        // Create and show a filter dialog or bottom sheet
        // This is a placeholder for the filter functionality
        binding.doccount.setText(filteredList.size() + " Doctors");
    }

    private void setupClearFiltersButton() {
        binding.btnClearFilters.setOnClickListener(v -> {
            // Clear all filters
            binding.chipAll.setChecked(true);
            binding.docsearchBar.setQuery("", false);
            filteredList.clear();
            filteredList.addAll(doctorList);
            updateDoctorCount(doctorList.size());
            updateUI();
        });
    }

    private void updateUI() {
        showLoading(false);

        if (filteredList.isEmpty()) {
            binding.emptyView.setVisibility(View.VISIBLE);
            binding.topdoctorlist.setVisibility(View.GONE);
        } else {
            binding.emptyView.setVisibility(View.GONE);
            binding.topdoctorlist.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.topdoctorlist.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.emptyView.setVisibility(View.GONE);
    }

    private void setupBackButton() {
        binding.backdoctorslist.setOnClickListener(v -> {
            onBackPressed(); // Go back to the previous activity
            finish(); // Close the current activity
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}