package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ProfileActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final float DEFAULT_MAP_ZOOM = 15f;
    private static final double DEFAULT_LATITUDE = 29.456885f;
    private static final double DEFAULT_LONGITUDE = -98.357193f;


    private GoogleMap locMap;
    private ParseGeoPoint nextMeetingLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initMap();
        hideAdminContent();
    }

    public void hideAdminContent() {
        TextView adminLabel = findViewById(R.id.adminOptionsLabel);
        Button scheduleButton = findViewById(R.id.scheduleMeetingButton);
        Button editButton = findViewById(R.id.editMeetingButton);
        Button viewUsersButton = findViewById(R.id.viewUsersButton);
        Button delPrevMeetingsButton = findViewById(R.id.deleteAllPreviousMeetingsButton);
        Button delAllMeetingsButton = findViewById(R.id.deleteAllMeetingsButton);

        adminLabel.setVisibility(View.GONE);
        scheduleButton.setVisibility(View.GONE);
        editButton.setVisibility(View.GONE);
        viewUsersButton.setVisibility(View.GONE);
        delPrevMeetingsButton.setVisibility(View.GONE);
        delAllMeetingsButton.setVisibility(View.GONE);

    }


    public void populateUserInfo() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getApplicationContext(), "The app may have had connection issues. Please try starting the app again!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String name = currentUser.getString("name");
        String department = currentUser.getString("department");
        boolean isAdmin = currentUser.getBoolean("isAdmin");

        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);

        TextView nameTextView = (TextView) (findViewById(R.id.displayName));
        nameTextView.setText("Name: " + name);
        nameTextView.startAnimation(fadeIn);

        TextView departmentTextView = (TextView) (findViewById(R.id.displayDepartment));
        departmentTextView.setText("Department: " + department);
        departmentTextView.startAnimation(fadeIn);

        Button signInToMeetingBtn = findViewById(R.id.signInToMeetingButton);
        signInToMeetingBtn.startAnimation(fadeIn);

        displayNextMeetingInfo();

        if (isAdmin)
            displayAdminOptions();
    }

    public void displayNextMeetingInfo() {
        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);

        TextView meetingLabel = (TextView) (findViewById(R.id.nextMeetingLabel));
        TextView meetingDescription = (TextView) (findViewById(R.id.displayNextMeetingDescription));
        TextView meetingTime = (TextView) (findViewById(R.id.displayNextMeetingTime));
        TextView meetingLocation = (TextView) (findViewById(R.id.displayNextMeetingLocation));

        Date currentDate = new Date();
        ParseUser currentUser = ParseUser.getCurrentUser();
        String[] acceptableDepartments = {"General", currentUser.getString("department")};

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Meeting");
        query.whereContainedIn("audience", Arrays.asList(acceptableDepartments));
        query.whereGreaterThanOrEqualTo("meetingDate", currentDate);
        query.orderByAscending("meetingDate");
        try {
            List<ParseObject> meetings = query.find();
            if (meetings.size() == 0)
                throw new Exception("failed");

            ParseObject nextMeeting = meetings.get(0);

            meetingDescription.setText("Description: " + nextMeeting.getString("meetingDescription"));

            Date nextMeetingDate = nextMeeting.getDate("meetingDate");
            DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
            meetingTime.setText("Time: " + df.format(nextMeetingDate));

            boolean meetingIsRemote = nextMeeting.getBoolean("isRemote");

            if (!meetingIsRemote) {
                nextMeetingLocation = nextMeeting.getParseGeoPoint("meetingLocation");
                String loc = String.format("Location: (%.6f,%.6f)", nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
                meetingLocation.setText(loc);

                setMapLocation();
                LinearLayout mapLayout = findViewById(R.id.mapLinLayout);
                mapLayout.setVisibility(View.VISIBLE);
                mapLayout.startAnimation(fadeIn);
            } else {
                meetingLocation.setText(R.string.locationRemoteMessage);
                setMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
                LinearLayout mapLayout = findViewById(R.id.mapLinLayout);
                mapLayout.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            meetingDescription.setText(R.string.noMeetingsScheduledMessage);
            meetingTime.setText("");
            meetingLocation.setText("");
            setMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
            LinearLayout mapLayout = findViewById(R.id.mapLinLayout);
            mapLayout.setVisibility(View.GONE);
        }


        meetingLabel.startAnimation(fadeIn);
        meetingDescription.startAnimation(fadeIn);
        meetingTime.startAnimation(fadeIn);
        meetingLocation.startAnimation(fadeIn);
    }

    public void displayAdminOptions() {
        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);

        TextView adminLabel = findViewById(R.id.adminOptionsLabel);
        Button scheduleButton = findViewById(R.id.scheduleMeetingButton);
        Button editButton = findViewById(R.id.editMeetingButton);
        Button viewUsersButton = findViewById(R.id.viewUsersButton);
        Button delPrevMeetingsButton = findViewById(R.id.deleteAllPreviousMeetingsButton);
        Button delAllMeetingsButton = findViewById(R.id.deleteAllMeetingsButton);

        adminLabel.setVisibility(View.VISIBLE);
        adminLabel.startAnimation(fadeIn);

        scheduleButton.setVisibility(View.VISIBLE);
        scheduleButton.startAnimation(fadeIn);

        editButton.setVisibility(View.VISIBLE);
        editButton.startAnimation(fadeIn);

        viewUsersButton.setVisibility(View.VISIBLE);
        viewUsersButton.startAnimation(fadeIn);

        delPrevMeetingsButton.setVisibility(View.VISIBLE);
        delPrevMeetingsButton.startAnimation(fadeIn);

        delAllMeetingsButton.setVisibility(View.VISIBLE);
        delAllMeetingsButton.startAnimation(fadeIn);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void setMapLocation() {
        if (nextMeetingLocation != null) {
            LatLng loc = new LatLng(nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
            locMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_MAP_ZOOM));
            locMap.addMarker(new MarkerOptions().position(loc).title("Meeting Location"));
        }
    }

    private void setMapLocation(double latitude, double longitude) {
        LatLng loc = new LatLng(latitude, longitude);
        locMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_MAP_ZOOM));
        locMap.addMarker(new MarkerOptions().position(loc).title("Meeting Location"));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        locMap = googleMap;
        populateUserInfo();
    }

    public void logOut(View view) {
        ParseUser.logOut();
        Toast.makeText(getApplicationContext(), "Come back soon!", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void scheduleMeeting(View view) {
        Intent goToScheduler = new Intent(this, ScheduleMeetingActivity.class);
        goToScheduler.putExtra("meetingID", "NONE");
        startActivity(goToScheduler);
    }

    public void signInToMeeting(View view) {
        Intent goToMeetingSignIn = new Intent(this, MeetingSignInActivity.class);
        startActivity(goToMeetingSignIn);
    }

    public void editExistingMeeting(View view) {
        Intent editMeetings = new Intent(this, ViewAllMeetings.class);
        editMeetings.putExtra("purpose", "READ-WRITE");
        startActivity(editMeetings);
    }

    public void viewAllUsers(View view) {
        Intent goToUserList = new Intent(this, UserListActivity.class);
        startActivity(goToUserList);
    }

    public void delAllPreviousMeetings(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setCancelable(true);
        builder.setTitle("WARNING");
        builder.setMessage("Are you sure you want to delete all the previous meetings in memory?");
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ParseQuery<ParseObject> getAllPreviousMeetings = ParseQuery.getQuery("Meeting");
                        getAllPreviousMeetings.whereLessThan("meetingDate", new Date());
                        try {
                            List<ParseObject> meetings = getAllPreviousMeetings.find();
                            ParseUser currentUser = ParseUser.getCurrentUser();
                            ParseLogger.log("User \"" + currentUser.getUsername() + "\" with name \"" + currentUser.get("name") + "\" has attempted to delete all previous meetings from memory.", "HIGH");
                            for (ParseObject current : meetings) {
                                current.deleteEventually();
                            }
                            ParseLogger.log("User \"" + currentUser.getUsername() + "\" with name \"" + currentUser.get("name") + "\" has deleted all previous meetings from memory.", "HIGH");
                            Toast.makeText(getApplicationContext(), "Successfully queued all previous meetings for deletion from memory.", Toast.LENGTH_LONG).show();
                            initMap();  //Resets profile screen to reflect new changes (only updates if pressed a second time, because when pressed the first time, the deleteEventually() operation has not yet finished completely)
                        } catch (ParseException e) {
                            Toast.makeText(getApplicationContext(), "Could not retrieve meetings at this moment. Please try again later!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Delete operation successfully avoided.", Toast.LENGTH_LONG).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void delAllMeetings(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setCancelable(true);
        builder.setTitle("WARNING");
        builder.setMessage("Are you sure you want to delete ALL meetings in memory?");
        builder.setPositiveButton("Confirm",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ParseQuery<ParseObject> getAllMeetings = ParseQuery.getQuery("Meeting");
                        try {
                            List<ParseObject> meetings = getAllMeetings.find();
                            ParseUser currentUser = ParseUser.getCurrentUser();
                            ParseLogger.log("User \"" + currentUser.getUsername() + "\" with name \"" + currentUser.get("name") + "\" has attempted to delete all meetings from memory.", "HIGH");
                            for (ParseObject current : meetings) {
                                current.deleteEventually();
                            }
                            ParseLogger.log("User \"" + currentUser.getUsername() + "\" with name \"" + currentUser.get("name") + "\" has deleted all meetings from memory.", "HIGH");
                            Toast.makeText(getApplicationContext(), "Successfully queued all meetings for deletion from memory.", Toast.LENGTH_LONG).show();
                            initMap();  //Resets profile screen to reflect new changes (only updates if pressed a second time, because when pressed the first time, the deleteEventually() operation has not yet finished completely)
                        } catch (ParseException e) {
                            Toast.makeText(getApplicationContext(), "Could not retrieve meetings at this moment. Please try again later!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Delete operation successfully avoided.", Toast.LENGTH_LONG).show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}