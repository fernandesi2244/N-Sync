package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final float DEFAULT_MAP_ZOOM = 16f;
    private static final double DEFAULT_LATITUDE = 29.456885f;
    private static final double DEFAULT_LONGITUDE = -98.357193f;

    private GoogleMap locMap;
    private ParseGeoPoint nextMeetingLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        initMap();
        hideAdminContent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.base_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go to parent activity.
                logOut();
                return true;
            case R.id.menu_refresh:
                refreshScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void hideAdminContent() {
        TextView adminLabel = findViewById(R.id.adminOptionsLabel);
        Button scheduleButton = findViewById(R.id.scheduleMeetingButton);
        Button editButton = findViewById(R.id.editMeetingButton);
        Button viewMeetingAttendanceButton = findViewById(R.id.viewMeetingAttendanceButton);
        Button viewUsersButton = findViewById(R.id.viewUsersButton);
        Button changeMarginsButton = findViewById(R.id.changeMarginsButton);
        Button viewLogButton = findViewById(R.id.viewLogButton);
        Button delPrevMeetingsButton = findViewById(R.id.deleteAllPreviousMeetingsButton);
        Button delAllMeetingsButton = findViewById(R.id.deleteAllMeetingsButton);

        adminLabel.setVisibility(View.GONE);
        scheduleButton.setVisibility(View.GONE);
        editButton.setVisibility(View.GONE);
        viewMeetingAttendanceButton.setVisibility((View.GONE));
        viewUsersButton.setVisibility(View.GONE);
        changeMarginsButton.setVisibility(View.GONE);
        viewLogButton.setVisibility(View.GONE);
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
        long noMeetings = 0;
        try {
            noMeetings = currentUser.getLong("noMeetingsAttended");
        } catch (Exception e) {
            //This guy must have attended QUINTILLIONS of meetings :)
            //In this case, don't do anything (it may also be some retrieval error).
        }
        boolean isAdmin = currentUser.getBoolean("isAdmin");

        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);

        TextView nameTextView = findViewById(R.id.displayName);
        String nameHtml = "<b>Name:</b> " + name;
        Spanned durationSpannedName = Html.fromHtml(nameHtml, Html.FROM_HTML_MODE_LEGACY);
        nameTextView.setText(durationSpannedName);
        nameTextView.startAnimation(fadeIn);

        TextView departmentTextView = findViewById(R.id.displayDepartment);
        String departmentHtml = "<b>Department:</b> " + department;
        Spanned durationSpannedDepartment = Html.fromHtml(departmentHtml, Html.FROM_HTML_MODE_LEGACY);
        departmentTextView.setText(durationSpannedDepartment);
        departmentTextView.startAnimation(fadeIn);

        TextView noMeetingsTextView = findViewById(R.id.displayNoMeetings);
        String noMeetingsHtml = "<b>Number of meetings attended:</b> " + noMeetings;
        Spanned durationSpannedNoMeetings = Html.fromHtml(noMeetingsHtml, Html.FROM_HTML_MODE_LEGACY);
        noMeetingsTextView.setText(durationSpannedNoMeetings);
        noMeetingsTextView.startAnimation(fadeIn);

        Button signInToMeetingBtn = findViewById(R.id.signInToMeetingButton);
        signInToMeetingBtn.startAnimation(fadeIn);

        Button viewPastMeetingsBtn = findViewById(R.id.viewPastMeetingsButton);
        viewPastMeetingsBtn.startAnimation(fadeIn);

        displayNextMeetingInfo();

        if (isAdmin)
            displayAdminOptions();
    }

    public void displayNextMeetingInfo() {
        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);

        TextView meetingLabel = findViewById(R.id.nextMeetingLabel);
        TextView meetingDescription = findViewById(R.id.displayNextMeetingDescription);
        TextView meetingTime = findViewById(R.id.displayNextMeetingTime);
        TextView meetingLocation = findViewById(R.id.displayNextMeetingLocation);

        Date currentDate = new Date();
        currentDate.setTime(currentDate.getTime() - 5 * MeetingSignInActivity.MILLISECONDS_PER_MINUTE); //Show next meeting up to 5 minutes late
        ParseUser currentUser = ParseUser.getCurrentUser();
        String[] acceptableDepartments = {"General", "general", currentUser.getString("department")};

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Meeting");
        query.whereContainedIn("audience", Arrays.asList(acceptableDepartments));
        query.whereGreaterThanOrEqualTo("meetingDate", currentDate);
        query.orderByAscending("meetingDate");
        try {
            List<ParseObject> meetings = query.find();
            if (meetings.size() == 0)
                throw new ParseException(ParseException.OBJECT_NOT_FOUND, "retrieval of object failed");

            ParseObject nextMeeting = meetings.get(0);

            String descriptionHtml = "<b>Description:</b> " + nextMeeting.getString("meetingDescription");
            Spanned durationSpannedDescription = Html.fromHtml(descriptionHtml, Html.FROM_HTML_MODE_LEGACY);
            meetingDescription.setText(durationSpannedDescription);

            Date nextMeetingDate = nextMeeting.getDate("meetingDate");
            DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
            String timeHtml = "<b>Time:</b> " + df.format(nextMeetingDate);
            Spanned durationSpannedTime = Html.fromHtml(timeHtml, Html.FROM_HTML_MODE_LEGACY);
            meetingTime.setText(durationSpannedTime);

            boolean meetingIsRemote = nextMeeting.getBoolean("isRemote");

            if (!meetingIsRemote) {
                nextMeetingLocation = nextMeeting.getParseGeoPoint("meetingLocation");
                getAddressFromLocation(nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
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

        } catch (ParseException e) {
            meetingDescription.setText(R.string.noMeetingsScheduledMessage);
            meetingTime.setText("");
            meetingLocation.setText("");
            setMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
            LinearLayout mapLayout = findViewById(R.id.mapLinLayout);
            mapLayout.setVisibility(View.GONE);
        } catch (Exception e) {
            meetingDescription.setText(R.string.nextMeetingDisplayError);
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
        Button viewMeetingAttendanceButton = findViewById(R.id.viewMeetingAttendanceButton);
        Button viewUsersButton = findViewById(R.id.viewUsersButton);
        Button changeMarginsButton = findViewById(R.id.changeMarginsButton);
        Button viewLogButton = findViewById(R.id.viewLogButton);
        Button delPrevMeetingsButton = findViewById(R.id.deleteAllPreviousMeetingsButton);
        Button delAllMeetingsButton = findViewById(R.id.deleteAllMeetingsButton);

        adminLabel.setVisibility(View.VISIBLE);
        adminLabel.startAnimation(fadeIn);

        scheduleButton.setVisibility(View.VISIBLE);
        scheduleButton.startAnimation(fadeIn);

        editButton.setVisibility(View.VISIBLE);
        editButton.startAnimation(fadeIn);

        viewMeetingAttendanceButton.setVisibility(View.VISIBLE);
        viewMeetingAttendanceButton.startAnimation(fadeIn);

        viewUsersButton.setVisibility(View.VISIBLE);
        viewUsersButton.startAnimation(fadeIn);

        changeMarginsButton.setVisibility(View.VISIBLE);
        changeMarginsButton.startAnimation(fadeIn);

        viewLogButton.setVisibility(View.VISIBLE);
        viewLogButton.startAnimation(fadeIn);

        delPrevMeetingsButton.setVisibility(View.VISIBLE);
        delPrevMeetingsButton.startAnimation(fadeIn);

        delAllMeetingsButton.setVisibility(View.VISIBLE);
        delAllMeetingsButton.startAnimation(fadeIn);
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        TextView meetingLocation = findViewById(R.id.displayNextMeetingLocation);
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses.size() > 0) {
                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder(fetchedAddress.getAddressLine(0));

                String locHtml = String.format("<b>Approximate location used for attendance verification:</b> %s", strAddress);
                Spanned durationSpannedLoc = Html.fromHtml(locHtml, Html.FROM_HTML_MODE_LEGACY);
                meetingLocation.setText(durationSpannedLoc);

            } else {
                String locHtml = String.format("<b>Location used for attendance verification:</b> (%.6f,%.6f)", nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
                Spanned durationSpannedLoc = Html.fromHtml(locHtml, Html.FROM_HTML_MODE_LEGACY);
                meetingLocation.setText(durationSpannedLoc);
            }

        } catch (Exception e) {
            String locHtml = String.format("<b>Location used for attendance verification:</b> (%.6f,%.6f)", nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
            Spanned durationSpannedLoc = Html.fromHtml(locHtml, Html.FROM_HTML_MODE_LEGACY);
            meetingLocation.setText(durationSpannedLoc);
        }
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
        finish();
    }

    public void logOut() {
        ParseUser.logOut();
        Toast.makeText(getApplicationContext(), "Come back soon!", Toast.LENGTH_LONG).show();
        Intent forceLogInScreen = new Intent(this, MainActivity.class);
        forceLogInScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(forceLogInScreen);
        finish();
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
        goToUserList.putExtra("purpose", "VIEW_ALL");
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
                            for (ParseObject current : meetings) {
                                current.deleteEventually();
                            }
                            ParseLogger.log("User \"" + currentUser.get("name") + "\" with email \"" + currentUser.getEmail() + "\" has deleted all previous meetings from memory.", "HIGH");
                            Toast.makeText(getApplicationContext(), "Successfully queued all previous meetings for deletion from memory.", Toast.LENGTH_LONG).show();
                            refreshScreen(); //Resets profile screen to reflect new changes (only updates if pressed a second time, because when pressed the first time, the deleteEventually() operation has not yet finished completely)
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
                            for (ParseObject current : meetings) {
                                current.deleteEventually();
                            }
                            ParseLogger.log("User \"" + currentUser.get("name") + "\" with email \"" + currentUser.getEmail() + "\" has deleted all meetings from memory.", "HIGH");
                            Toast.makeText(getApplicationContext(), "Successfully queued all meetings for deletion from memory.", Toast.LENGTH_LONG).show();
                            refreshScreen(); //Resets profile screen to reflect new changes (only updates if pressed a second time, because when pressed the first time, the deleteEventually() operation has not yet finished completely)
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

    public void viewPastMeetings(View view) {
        //For some reason, this takes a few moments if the user has attended at least 1 meeting that has been deleted
        Toast.makeText(getApplicationContext(), "Loading... May take a few moments.", Toast.LENGTH_LONG).show();
        ParseUser currentUser = ParseUser.getCurrentUser();
        String userId = currentUser.getObjectId();
        Intent displayMeetings = new Intent(this, ViewAllMeetings.class);
        displayMeetings.putExtra("purpose", "READ-USER_SPECIFIC");
        displayMeetings.putExtra("userId", userId);
        startActivity(displayMeetings);
    }

    public void viewMeetingAttendance(View view) {
        Intent goToMeetingList = new Intent(this, ViewAllMeetings.class);
        goToMeetingList.putExtra("purpose", "READ-VIEW_ATTENDANCE");
        startActivity(goToMeetingList);
    }

    public void viewLog(View view) {
        Intent goToLogActivity = new Intent(this, LogActivity.class);
        startActivity(goToLogActivity);
    }

    public void changeMargins(View view) {
        Intent goToMarginsActivity = new Intent(this, EditMarginsActivity.class);
        startActivity(goToMarginsActivity);
    }

    private void refreshScreen() {
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }
}