package com.healthcare.aarogyanidaan;

import android.content.Context;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AppointmentManager {
    static void loadAppointmentsForUser(String userId, String userType,
                                        RecyclerView recyclerView, Context context) {
        // Get references to progress bar and empty state text view
        View parentView = (View) recyclerView.getParent();
        TextView emptyStateText = parentView.findViewById(R.id.emptyAppointmentsText);

        List<Appointment> appointments = new ArrayList<>();
        AppointmentAdapter adapter = new AppointmentAdapter(appointments, context);

        // Show progress bar, hide recycler view and empty state initially
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        // Get the existing orientation of the RecyclerView
        int orientation = recyclerView.getLayoutParams().height > recyclerView.getLayoutParams().width ?
                LinearLayoutManager.VERTICAL : LinearLayoutManager.HORIZONTAL;

        // Set layout manager with detected orientation
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, orientation, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        DatabaseReference appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        Query query = userType.equals("patient") ?
                appointmentsRef.orderByChild("patientId").equalTo(userId) :
                appointmentsRef.orderByChild("doctorId").equalTo(userId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointments.clear();
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    Appointment appointment = appointmentSnapshot.getValue(Appointment.class);
                    if (appointment != null && !isPastAppointment(appointment)) {
                        appointments.add(appointment);
                    } else if (appointment != null && isPastAppointment(appointment)) {
                        // Remove past appointments
                        appointmentSnapshot.getRef().removeValue();
                    }
                }


                // Show either recycler view or empty state text
                if (appointments.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyStateText.setVisibility(View.VISIBLE);
                    emptyStateText.setText("No appointments scheduled");
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                recyclerView.setVisibility(View.GONE);
                emptyStateText.setVisibility(View.VISIBLE);
                emptyStateText.setText("Error loading appointments");

                Toast.makeText(context, "Error loading appointments: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static boolean isPastAppointment(Appointment appointment) {
        try {
            // Parse appointment date and time
            String[] dateParts = appointment.getDate().split("/");
            String[] timeParts = appointment.getTime().split(":");

            Calendar appointmentCal = Calendar.getInstance();
            appointmentCal.set(
                    Integer.parseInt(dateParts[2]), // year
                    Integer.parseInt(dateParts[1]) - 1, // month (0-based)
                    Integer.parseInt(dateParts[0]), // day
                    Integer.parseInt(timeParts[0]), // hour
                    Integer.parseInt(timeParts[1]) // minute
            );

            // Compare with current time
            return appointmentCal.getTimeInMillis() < System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}