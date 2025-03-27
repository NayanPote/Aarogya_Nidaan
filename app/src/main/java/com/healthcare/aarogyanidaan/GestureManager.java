// GestureManager.java
package com.healthcare.aarogyanidaan;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.view.GestureDetectorCompat;

public class GestureManager implements View.OnTouchListener {
    private static final String TAG = "GestureManager";
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private final Activity activity;
    private final GestureDetectorCompat gestureDetector;

    public GestureManager(Activity activity) {
        this.activity = activity;
        this.gestureDetector = new GestureDetectorCompat(activity, new SwipeGestureListener());
    }

    public void attachToView(View view) {
        if (view != null) {
            view.setOnTouchListener(this);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (e1 == null || e2 == null) {
                    return false;
                }

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                float absXDiff = Math.abs(diffX);
                float absYDiff = Math.abs(diffY);

                // Log the swipe values for debugging
                Log.d(TAG, "Swipe values - diffX: " + diffX + ", diffY: " + diffY +
                        ", velocityX: " + velocityX + ", velocityY: " + velocityY);

                if (absXDiff > absYDiff &&
                        absXDiff > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX > 0) {
                        // Right swipe
                        Log.d(TAG, "Right swipe detected");
                        activity.onBackPressed();
                        return true;
                    } else {
                        // Left swipe
                        Log.d(TAG, "Left swipe detected");
                        return handleLeftSwipe();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onFling", e);
            }
            return false;
        }

        private boolean handleLeftSwipe() {
            Intent intent = null;

            if (activity instanceof patientdashboard) {
                intent = new Intent(activity, patienthealthdata.class);
            } else if (activity instanceof patienthealthdata) {
                intent = new Intent(activity, patientchat.class);
            }

            if (intent != null) {
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return false;
        }
    }
}