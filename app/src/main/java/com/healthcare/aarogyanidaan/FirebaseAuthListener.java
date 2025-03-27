package com.healthcare.aarogyanidaan;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthListener implements FirebaseAuth.AuthStateListener {

    private static final String TAG = "FirebaseAuthListener";
    private Context context;
    private LocalNotificationService notificationService;

    public FirebaseAuthListener(Context context) {
        this.context = context;
        this.notificationService = LocalNotificationService.getInstance(context);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            // User signed in
            Log.d(TAG, "User signed in: " + user.getUid());
            notificationService.checkAuthStatusAndReInitialize();
        } else {
            // User signed out
            Log.d(TAG, "User signed out");
            notificationService.checkAuthStatusAndReInitialize();
        }
    }

    public void register() {
        FirebaseAuth.getInstance().addAuthStateListener(this);
    }

    public void unregister() {
        FirebaseAuth.getInstance().removeAuthStateListener(this);
    }
}