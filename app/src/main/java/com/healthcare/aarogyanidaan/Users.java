package com.healthcare.aarogyanidaan;

public class Users {

    // Patient fields
    private String patient_id, patient_name, patient_email, patient_contactno, patient_gender, patient_dob, patient_city, patient_password;

    // Doctor fields
    private String doctor_id, doctor_name, doctor_email, doctor_contactno, doctor_gender, doctor_dob, doctor_city, doctor_password, doctor_specialization;

    // Verification fields
    private boolean emailVerified;
    private boolean phoneVerified;
    private String doctor_rating;
    private String doctor_reviews_count;
    private String doctor_address;
    private String doctor_experience;
    private String doctor_qualification;
    private String doctor_description;
    private String doctor_profile_image;
    private String doctor_verified;
    private String doctor_available;

    // Default constructor required for Firebase
    public Users() {}

    // Constructor for Doctor
    public Users(String doctor_id, String doctor_name, String doctor_email, String doctor_contactno,
                 String doctor_gender, String doctor_dob, String doctor_city, String doctor_password,
                 String doctor_specialization, String doctor_rating, String doctor_reviews_count, String doctor_address,
                 String doctor_experience, String doctor_qualification, String doctor_description, String doctor_profile_image,
                 String doctor_verified, String doctor_available) {
        this.doctor_id = doctor_id;
        this.doctor_name = doctor_name;
        this.doctor_email = doctor_email;
        this.doctor_contactno = doctor_contactno;
        this.doctor_gender = doctor_gender;
        this.doctor_dob = doctor_dob;
        this.doctor_city = doctor_city;
        this.doctor_password = doctor_password;
        this.doctor_specialization = doctor_specialization;
        this.doctor_rating = doctor_rating;
        this.doctor_reviews_count = doctor_reviews_count;
        this.doctor_address = doctor_address;
        this.doctor_experience = doctor_experience;
        this.doctor_qualification = doctor_qualification;
        this.doctor_description = doctor_description;
        this.doctor_profile_image = doctor_profile_image;
        this.doctor_verified = doctor_verified;
        this.doctor_available = doctor_available;
        this.emailVerified = false;
        this.phoneVerified = false;

    }

    // Constructor for Patient
    public Users(String patient_id, String patient_name, String patient_email, String patient_contactno,
                 String patient_gender, String patient_dob, String patient_city, String patient_password) {
        this.patient_id = patient_id;
        this.patient_name = patient_name;
        this.patient_email = patient_email;
        this.patient_contactno = patient_contactno;
        this.patient_gender = patient_gender;
        this.patient_dob = patient_dob;
        this.patient_city = patient_city;
        this.patient_password = patient_password;
    }

    // Doctor Getters and Setters
    public String getDoctor_id() { return doctor_id; }
    public void setDoctor_id(String doctor_id) { this.doctor_id = doctor_id; }

    public String getDoctor_name() { return doctor_name; }
    public void setDoctor_name(String doctor_name) { this.doctor_name = doctor_name; }

    public String getDoctor_email() { return doctor_email; }
    public void setDoctor_email(String doctor_email) { this.doctor_email = doctor_email; }

    public String getDoctor_contactno() { return doctor_contactno; }
    public void setDoctor_contactno(String doctor_contactno) { this.doctor_contactno = doctor_contactno; }

    public String getDoctor_gender() { return doctor_gender; }
    public void setDoctor_gender(String doctor_gender) { this.doctor_gender = doctor_gender; }

    public String getDoctor_dob() { return doctor_dob; }
    public void setDoctor_dob(String doctor_dob) { this.doctor_dob = doctor_dob; }

    public String getDoctor_city() { return doctor_city; }
    public void setDoctor_city(String doctor_city) { this.doctor_city = doctor_city; }

    public String getDoctor_password() { return doctor_password; }
    public void setDoctor_password(String doctor_password) { this.doctor_password = doctor_password; }

    public String getDoctor_specialization() { return doctor_specialization; }
    public void setDoctor_specialization(String doctor_specialization) { this.doctor_specialization = doctor_specialization; }

    public String getDoctor_rating() {
        return doctor_rating;
    }

    public void setDoctor_rating(String doctor_rating) {
        this.doctor_rating = doctor_rating;
    }

    public String getDoctor_reviews_count() {
        return doctor_reviews_count;
    }

    public void setDoctor_reviews_count(String doctor_reviews_count) {
        this.doctor_reviews_count = doctor_reviews_count;
    }

    public String getDoctor_address() {
        return doctor_address;
    }

    public void setDoctor_address(String doctor_address) {
        this.doctor_address = doctor_address;
    }

    public String getDoctor_experience() {
        return doctor_experience;
    }

    public void setDoctor_experience(String doctor_experience) {
        this.doctor_experience = doctor_experience;
    }

    public String getDoctor_qualification() {
        return doctor_qualification;
    }

    public void setDoctor_qualification(String doctor_qualification) {
        this.doctor_qualification = doctor_qualification;
    }

    public String getDoctor_description() {
        return doctor_description;
    }

    public void setDoctor_description(String doctor_description) {
        this.doctor_description = doctor_description;
    }

    public String getDoctor_profile_image() {
        return doctor_profile_image;
    }

    public void setDoctor_profile_image(String doctor_profile_image) {
        this.doctor_profile_image = doctor_profile_image;
    }

    public String getDoctor_verified() {
        return doctor_verified;
    }

    public void setDoctor_verified(String doctor_verified) {
        this.doctor_verified = doctor_verified;
    }

    public String getDoctor_available() {
        return doctor_available;
    }

    public void setDoctor_available(String doctor_available) {
        this.doctor_available = doctor_available;
    }

    // Patient Getters and Setters
    public String getPatient_id() { return patient_id; }
    public void setPatient_id(String patient_id) { this.patient_id = patient_id; }

    public String getPatient_name() { return patient_name; }
    public void setPatient_name(String patient_name) { this.patient_name = patient_name; }

    public String getPatient_email() { return patient_email; }
    public void setPatient_email(String patient_email) { this.patient_email = patient_email; }

    public String getPatient_contactno() { return patient_contactno; }
    public void setPatient_contactno(String patient_contactno) { this.patient_contactno = patient_contactno; }

    public String getPatient_gender() { return patient_gender; }
    public void setPatient_gender(String patient_gender) { this.patient_gender = patient_gender; }

    public String getPatient_dob() { return patient_dob; }
    public void setPatient_dob(String patient_dob) { this.patient_dob = patient_dob; }

    public String getPatient_city() { return patient_city; }
    public void setPatient_city(String patient_city) { this.patient_city = patient_city; }

    public String getPatient_password() { return patient_password; }
    public void setPatient_password(String patient_password) { this.patient_password = patient_password; }

    // Confirm Password
    public void setPatient_confirmpassword(String confirmpassword) {
        this.patient_password = confirmpassword;
    }

    // Verification Getters and Setters
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public void setBiometricEnabled(boolean checked) {

    }

    // Extended Patient Model (Nested Class)
    public static class PatientUser extends Users {
        private String bloodGroup;
        private String allergies;
        private String emergencyContact;
        private String medicalHistory;
        private boolean enableBiometric;
        private String profileImageUrl;

        // Default constructor required for Firebase
        public PatientUser() {
            super();
            this.enableBiometric = false;
            this.profileImageUrl = "";
        }

        // Constructor
        public PatientUser(String patientId, String name, String email, String contactno, String gender,
                           String dob, String city, String password, String bloodGroup, String allergies,
                           String emergencyContact, String medicalHistory) {
            super(patientId, name, email, contactno, gender, dob, city, password);
            this.bloodGroup = bloodGroup;
            this.allergies = allergies;
            this.emergencyContact = emergencyContact;
            this.medicalHistory = medicalHistory;
            this.enableBiometric = false;
            this.profileImageUrl = "";
        }

        // Getters and Setters for Extended Patient Fields
        public String getBloodGroup() { return bloodGroup; }
        public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

        public String getAllergies() { return allergies; }
        public void setAllergies(String allergies) { this.allergies = allergies; }

        public String getEmergencyContact() { return emergencyContact; }
        public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }

        public String getMedicalHistory() { return medicalHistory; }
        public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }

        public boolean isEnableBiometric() { return enableBiometric; }
        public void setEnableBiometric(boolean enableBiometric) { this.enableBiometric = enableBiometric; }

        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    }
}
