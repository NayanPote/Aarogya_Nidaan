package com.healthcare.aarogyanidaan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RatingBar;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class doctorlistadapter extends RecyclerView.Adapter<doctorlistadapter.ViewHolder> {

    private final Context context;
    private final ArrayList<Users> doctorList;
    private OnDoctorClickListener clickListener;

    public interface OnDoctorClickListener {
        void onDoctorClick(Users doctor, int position);
    }

    public doctorlistadapter(Context context, ArrayList<Users> doctorList) {
        this.context = context;
        this.doctorList = doctorList;
    }

    public void setOnDoctorClickListener(OnDoctorClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.doctorslist_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users doctor = doctorList.get(position);

        // Set text values
        holder.doctorListName.setText(doctor.getDoctor_name());
        holder.doctorListSpecialization.setText(doctor.getDoctor_specialization());
        holder.doctorListCity.setText(doctor.getDoctor_city());

        // Set default avatar or load real image using an image loading library
        holder.doctorAvatar.setImageResource(R.drawable.doctoravatar3);

        // Set rating if available in the doctor object
        if (doctor.getDoctor_rating() != null && !doctor.getDoctor_rating().isEmpty()) {
            try {
                float rating = Float.parseFloat(doctor.getDoctor_rating());
                holder.doctorRating.setRating(rating);
                holder.ratingText.setText(doctor.getDoctor_rating());
            } catch (NumberFormatException e) {
                holder.doctorRating.setRating(4.0f); // Default rating
                holder.ratingText.setText("4.0");
            }
        } else {
            holder.doctorRating.setRating(4.0f); // Default rating
            holder.ratingText.setText("4.0");
        }

        // Set review count if available
        if (doctor.getDoctor_reviews_count() != null && !doctor.getDoctor_reviews_count().isEmpty()) {
            holder.reviewsCount.setText("(" + doctor.getDoctor_reviews_count() + " reviews)");
        } else {
            holder.reviewsCount.setText("(236 reviews)"); // Default review count
        }

        // Handle book appointment button click
        holder.btnBookAppointment.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDoctorClick(doctor, position);
            }
        });

        // Handle entire item click
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDoctorClick(doctor, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return doctorList != null ? doctorList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView doctorListName, doctorListSpecialization, doctorListCity;
        TextView ratingText, reviewsCount;
        ImageView doctorAvatar, verifiedBadge;
        RatingBar doctorRating;
        MaterialButton btnBookAppointment;
        CardView avatarContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            doctorListName = itemView.findViewById(R.id.doctorlistname);
            doctorListSpecialization = itemView.findViewById(R.id.doctorlistspe);
            doctorListCity = itemView.findViewById(R.id.doclistcity);
            doctorAvatar = itemView.findViewById(R.id.doctoravatar);
            verifiedBadge = itemView.findViewById(R.id.verified_badge);
            doctorRating = itemView.findViewById(R.id.doctor_rating);
            ratingText = itemView.findViewById(R.id.rating_text);
            reviewsCount = itemView.findViewById(R.id.reviews_count);
            btnBookAppointment = itemView.findViewById(R.id.btn_book_appointment);
            avatarContainer = itemView.findViewById(R.id.avatar_container);
        }
    }
}