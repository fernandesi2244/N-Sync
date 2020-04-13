package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MeetingSignInActivity extends AppCompatActivity {

    public static final String MeetingSignInActivityID = "MeetingSignInActivity";

    private static long marginInMinutes;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location currentLoc;
    private ParseObject clickedMeeting;
    private float locationMargin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_sign_in);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMeetingsList();
    }

    private void setUpMeetingsList() {
        LinearLayout meetingsLayout = findViewById(R.id.meetingsLayout);
        meetingsLayout.removeAllViews();

        ParseQuery<ParseObject> getMargin = ParseQuery.getQuery("MeetingAttendanceMarginOfError");
        getMargin.whereEqualTo("objectId", "ZCHh4cadL1");
        try {
            List<ParseObject> parseObjects = getMargin.find();
            if(parseObjects.size()>0) {
                marginInMinutes = parseObjects.get(0).getNumber("marginInMinutes").longValue();
            } else {
                Toast.makeText(getApplicationContext(), "Error: Meeting attendance time margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Error: Meeting attendance time margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
            return;
        }

        long marginInMilliseconds = marginInMinutes * 60_000;
        Date earliestDate = new Date();
        earliestDate.setTime(new Date().getTime() - marginInMilliseconds);

        ParseUser currentUser = ParseUser.getCurrentUser();
        String[] acceptableDepartments = {"General", currentUser.getString("department")};

        ParseQuery<ParseObject> findEligibleMeetings = ParseQuery.getQuery("Meeting");
        findEligibleMeetings.whereContainedIn("audience", Arrays.asList(acceptableDepartments));
        findEligibleMeetings.whereGreaterThanOrEqualTo("meetingDate", earliestDate);
        findEligibleMeetings.orderByAscending("meetingDate");

        try {
            List<ParseObject> meetings = findEligibleMeetings.find();
            if (meetings.size() == 0)
                throw new Exception("failed");

            for (ParseObject current : meetings) {
                Button nextButton = new Button(this);
                nextButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                String meetingDescription = current.getString("meetingDescription");
                Date nextMeetingDate = current.getDate("meetingDate");
                DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
                String date = df.format(nextMeetingDate);

                nextButton.setText("Description: " + meetingDescription + "; Time: " + date + "; ID: " + current.getObjectId());
                nextButton.setId(View.generateViewId());
                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Button clickedButton = (Button) v;
                        String btnText = clickedButton.getText().toString();
                        String objectId = btnText.split("ID: ")[1];

                        ParseQuery<ParseObject> getMeeting = ParseQuery.getQuery("Meeting");
                        getMeeting.whereEqualTo("objectId", objectId);
                        getMeeting.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> objects, ParseException e) {
                                if (e == null) {
                                    if (objects.size() == 1) {
                                        clickedMeeting = objects.get(0);
                                        verifyMeetingAttendance();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Something went wrong when accessing the desired meeting. Please try again!", Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "Something went wrong when accessing the desired meeting. Please try again!", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                });

                meetingsLayout.addView(nextButton);

            }

        } catch (Exception e) {
            TextView noMeetingsTextView = new TextView(this);
            noMeetingsTextView.setGravity(Gravity.CENTER);
            noMeetingsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            noMeetingsTextView.setText(R.string.noMeetingsScheduledMessage);
            noMeetingsTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            noMeetingsTextView.setId(View.generateViewId());
            meetingsLayout.addView(noMeetingsTextView);
        }
    }

    protected void checkPermissions() {
        if (!hasLocationPermissions()) {
            Intent goToRequestPage = new Intent(this, RequestPermissionsActivity.class);
            goToRequestPage.putExtra("sender", MeetingSignInActivityID);
            startActivity(goToRequestPage);
        }
    }

    private boolean hasLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //All good; user may proceed
            return true;
        } else {
            //User needs to be redirected to permissions activity
            return false;
        }
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            Task locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        currentLoc = (Location) task.getResult();
                    }
                    resumeVerificationProcessForDeviceLocation();
                }
            });
        } catch (
                SecurityException e) {
            return;
        }
    }

    private void verifyMeetingAttendance() {
        boolean isRemote = clickedMeeting.getBoolean("isRemote");

        ParseQuery<ParseObject> getMargin = ParseQuery.getQuery("MeetingAttendanceMarginOfError");
        getMargin.whereEqualTo("objectId", "ZCHh4cadL1");
        try {
            List<ParseObject> parseObjects = getMargin.find();
            if(parseObjects.size()>0) {
                marginInMinutes = parseObjects.get(0).getNumber("marginInMinutes").longValue();
            } else {
                Toast.makeText(getApplicationContext(), "Error: Meeting attendance time margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Error: Meeting attendance time margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
            return;
        }

        long marginInMilliseconds = marginInMinutes * 60_000;
        Date meetingDate = clickedMeeting.getDate("meetingDate");

        Date currentDate = new Date();
        Date beforeDate = new Date();
        beforeDate.setTime(currentDate.getTime() - marginInMilliseconds);
        Date afterDate = new Date();
        afterDate.setTime(currentDate.getTime() + marginInMilliseconds);

        boolean timingIsGood = false;

        if (beforeDate.getTime() <= meetingDate.getTime() && meetingDate.getTime() <= afterDate.getTime()) {
            timingIsGood = true;
        }

        ParseUser currentUser = ParseUser.getCurrentUser();

        if (isRemote) {
            if (timingIsGood) {
                ArrayList<ParseObject> meetingsAttendedByUser = (ArrayList<ParseObject>)currentUser.get("meetingsAttended");

                if(meetingsAttendedByUser==null) {
                    Toast.makeText(getApplicationContext(), "Something went wrong during online retrieval of data. Please try again later!", Toast.LENGTH_LONG).show();
                    return;
                }

                //Check if user already signed into the meeting
                for(ParseObject mtng: meetingsAttendedByUser) {
                    if(mtng.getObjectId().equals(clickedMeeting.getObjectId())) {
                        Toast.makeText(getApplicationContext(), "You already signed into this meeting!", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                meetingsAttendedByUser.add(clickedMeeting);

                currentUser.put("meetingsAttended", meetingsAttendedByUser);
                currentUser.put("noMeetingsAttended", currentUser.getInt("noMeetingsAttended") + 1);

                currentUser.saveInBackground();;//IMPORTANT

                Toast.makeText(getApplicationContext(), "You successfully signed into the meeting!", Toast.LENGTH_LONG).show();
                goToProfile();
            } else {
                Toast.makeText(getApplicationContext(), "Sorry, but you are either too early or too late to check into the meeting. Please contact an administrator if this is a concern.", Toast.LENGTH_LONG).show();
            }
        } else {
            if (!hasLocationPermissions()) {
                checkPermissions();
            } else {
                if (timingIsGood) {
                    getDeviceLocation();
                } else {
                    Toast.makeText(getApplicationContext(), "Sorry, but you are either too early or too late to check into the meeting. Please contact an administrator if this is a concern.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void resumeVerificationProcessForDeviceLocation() {
        if (currentLoc == null) {
            Toast.makeText(getApplicationContext(), "Could not retrieve phone's location. Please try again!", Toast.LENGTH_LONG).show();
            return;
        }

        ParseGeoPoint meetingLocInParse = clickedMeeting.getParseGeoPoint("meetingLocation");
        Location meetingLoc = new Location("");
        meetingLoc.setLatitude(meetingLocInParse.getLatitude());
        meetingLoc.setLongitude(meetingLocInParse.getLongitude());

        ParseQuery<ParseObject> getMargin = ParseQuery.getQuery("LocationMargin");
        getMargin.whereEqualTo("objectId", "s5qyTqdbHY");
        try {
            List<ParseObject> parseObjects = getMargin.find();
            if(parseObjects.size()>0) {
                locationMargin = parseObjects.get(0).getNumber("locationMarginInMeters").floatValue();
            } else {
                Toast.makeText(getApplicationContext(), "Error: Meeting location margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Error: Meeting location margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
            return;
        }

        ParseUser currentUser = ParseUser.getCurrentUser();

        float actualDistance = currentLoc.distanceTo(meetingLoc);
        if (actualDistance < locationMargin) {
            ArrayList<ParseObject> meetingsAttendedByUser = (ArrayList<ParseObject>)currentUser.get("meetingsAttended");

            if(meetingsAttendedByUser==null) {
                Toast.makeText(getApplicationContext(), "Something went wrong during online retrieval of data. Please try again later!", Toast.LENGTH_LONG).show();
                return;
            }

            //Check if user already signed into the meeting
            for(ParseObject mtng: meetingsAttendedByUser) {
                if(mtng.getObjectId().equals(clickedMeeting.getObjectId())) {
                    Toast.makeText(getApplicationContext(), "You already signed into this meeting!", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            meetingsAttendedByUser.add(clickedMeeting);

            currentUser.put("meetingsAttended", meetingsAttendedByUser);
            currentUser.put("noMeetingsAttended", currentUser.getInt("noMeetingsAttended") + 1);

            currentUser.saveInBackground();;//IMPORTANT

            Toast.makeText(getApplicationContext(), "You successfully signed into the meeting!", Toast.LENGTH_LONG).show();
            goToProfile();
        } else {
            Toast.makeText(getApplicationContext(), "Sorry, but you are not close enough to the meeting location to confirm your presence. Please contact an administrator if this is a concern.", Toast.LENGTH_LONG).show();
        }
    }

    private void goToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }
}
