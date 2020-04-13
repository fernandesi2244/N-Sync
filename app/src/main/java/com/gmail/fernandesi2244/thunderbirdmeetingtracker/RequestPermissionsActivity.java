package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

public class RequestPermissionsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static int tries = 0;
    private String sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permissions);

        Intent receivedIntent = getIntent();
        sender = receivedIntent.getStringExtra("sender");

        getLocationPermissions();
    }

    private void getLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //Double check
            goBackToCallingActivity();
        } else {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            if (tries >= 3)
                                return;
                            tries++;
                            Toast.makeText(getApplicationContext(), "Please note that these location permissions are not optional for the continued operation of the app! Thank you for your understanding.", Toast.LENGTH_LONG).show();
                            getLocationPermissions();
                            return;
                        }
                    }
                    goBackToCallingActivity();
                } else {
                    Toast.makeText(getApplicationContext(), "An error occurred. Please try again!", Toast.LENGTH_LONG).show();
                    tries++;
                    getLocationPermissions();
                }
        }
    }

    private void goBackToCallingActivity() {
        Intent goBack;

        switch(sender) {
            case ScheduleMeetingActivity.ScheduleMeetingActivityID:
                goBack = new Intent(this, ScheduleMeetingActivity.class);
                break;
            case MeetingSignInActivity.MeetingSignInActivityID:
                goBack = new Intent(this, MeetingSignInActivity.class);
                break;
            default:
                goBack = new Intent(this, ProfileActivity.class);
        }
        goBack.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(goBack);
    }
}
