package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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

    /**
     * Used to set up refresh button.
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.base_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Determine whether the user wanted to go back to the previous screen or refresh the existing screen.
     *
     * @param item the item selected from the bar at the top of the app's screen
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // User wants to go back to the previous screen. In this case, the previous screen is the log in page, so log the user out.
                logOut();
                return true;
            case R.id.menu_refresh:
                // User wants to refresh the page
                refreshScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Initially hide the administrator options from the user so that a normal user cannot mess with them.
     */
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


    /**
     * Load the user's profile from the server onto the screen.
     */
    public void populateUserInfo() {
        ParseUser currentUser = ParseUser.getCurrentUser();

        if(userIsNull())
            return;

        String name = currentUser.getString("name");
        String department = currentUser.getString("department");
        long noMeetings = 0;
        try {
            noMeetings = currentUser.getLong("noMeetingsAttended");
        } catch (Exception e) {
            // This guy must have attended QUINTILLIONS of meetings :)
            // In this case, don't do anything (it may also be some retrieval error).
        }

        // Animate the profile data, using HTML to make category names bold
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

        // If user is admin, display admin options
        boolean isAdmin = currentUser.getBoolean("isAdmin");
        if (isAdmin)
            displayAdminOptions();
    }

    /**
     * Retrieves and displays information about the next meeting scheduled.
     */
    public void displayNextMeetingInfo() {
        if(userIsNull())
            return;

        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);

        TextView meetingLabel = findViewById(R.id.nextMeetingLabel);
        TextView meetingDescription = findViewById(R.id.displayNextMeetingDescription);
        TextView meetingTime = findViewById(R.id.displayNextMeetingTime);
        TextView meetingLocation = findViewById(R.id.displayNextMeetingLocation);

        // Show next meeting up to 5 minutes late (however, user can still sign into meeting as late or as early as the time margin specified on the server-which is also an admin option)
        Date currentDate = new Date();
        currentDate.setTime(currentDate.getTime() - 5 * MeetingSignInActivity.MILLISECONDS_PER_MINUTE);

        // Only display meeting that is open to the user's department
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

    /**
     * If the user is an admin, display their respective options.
     */
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

    /**
     * Turn latitude and longitude pair into an actual address (if it can be done).
     *
     * @param latitude  the latitude coordinate to transform
     * @param longitude the longitude coordinate to transform
     */
    private void getAddressFromLocation(double latitude, double longitude) {
        TextView meetingLocation = findViewById(R.id.displayNextMeetingLocation);
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses.size() > 0) {
                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder(fetchedAddress.getAddressLine(0));

                String locHtml = String.format("<b>Approximate location used for attendance verification:</b> %s", strAddress);
                Spanned durationSpannedLoc = Html.fromHtml(locHtml, Html.FROM_HTML_MODE_LEGACY);
                meetingLocation.setText(durationSpannedLoc);

            } else {
                // Lat/long pair couldn't be transformed; just display coordinates
                String locHtml = String.format("<b>Location used for attendance verification:</b> (%.6f,%.6f)", nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
                Spanned durationSpannedLoc = Html.fromHtml(locHtml, Html.FROM_HTML_MODE_LEGACY);
                meetingLocation.setText(durationSpannedLoc);
            }

        } catch (Exception e) {
            // Lat/long pair couldn't be transformed; just display coordinates
            String locHtml = String.format("<b>Location used for attendance verification:</b> (%.6f,%.6f)", nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
            Spanned durationSpannedLoc = Html.fromHtml(locHtml, Html.FROM_HTML_MODE_LEGACY);
            meetingLocation.setText(durationSpannedLoc);
        }
    }

    /**
     * Initialize the Google Map.
     */
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Set the location of the map to that of the next meeting.
     */
    private void setMapLocation() {
        if (nextMeetingLocation != null) {
            LatLng loc = new LatLng(nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
            locMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_MAP_ZOOM));
            locMap.addMarker(new MarkerOptions().position(loc).title("Meeting Location"));
        }
    }

    /**
     * Set location of map to custom lat/long pair.
     *
     * @param latitude the latitude coordinate of the location to center the map on
     * @param longitude the longitude coordinate of the location to center the map on
     */
    private void setMapLocation(double latitude, double longitude) {
        LatLng loc = new LatLng(latitude, longitude);
        locMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_MAP_ZOOM));
        locMap.addMarker(new MarkerOptions().position(loc).title("Meeting Location"));
    }

    /**
     * Only populate user profile info once map is ready (to prevent null error later).
     *
     * @param googleMap the map that's being readied up
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        locMap = googleMap;
        populateUserInfo();
    }

    /**
     * Log the user out of the app (from the log out button).
     *
     * @param view the log out button
     */
    public void logOut(View view) {
        if(userIsNull())
            return;

        ParseUser.logOut();
        Toast.makeText(getApplicationContext(), "Come back soon!", Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Log the user out of the app (from the back arrow).
     */
    public void logOut() {
        if(userIsNull())
            return;

        ParseUser.logOut();
        Toast.makeText(getApplicationContext(), "Come back soon!", Toast.LENGTH_LONG).show();
        Intent forceLogInScreen = new Intent(this, MainActivity.class);
        forceLogInScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(forceLogInScreen);
        finish();
    }

    /**
     * Go to screen to schedule a new meeting.
     *
     * @param view schedule meeting button
     */
    public void scheduleMeeting(View view) {
        if(userIsNull())
            return;

        Intent goToScheduler = new Intent(this, ScheduleMeetingActivity.class);
        goToScheduler.putExtra("meetingID", "NONE");
        startActivity(goToScheduler);
    }

    /**
     * Go to screen to sign into an existing meeting.
     *
     * @param view meeting sign in button
     */
    public void signInToMeeting(View view) {
        if(userIsNull())
            return;

        Intent goToMeetingSignIn = new Intent(this, MeetingSignInActivity.class);
        startActivity(goToMeetingSignIn);
    }

    /**
     * Go to screen to edit an existing meeting.
     *
     * @param view the edit meeting button
     */
    public void editExistingMeeting(View view) {
        if(userIsNull())
            return;

        Intent editMeetings = new Intent(this, ViewAllMeetings.class);
        editMeetings.putExtra("purpose", "READ-WRITE");
        startActivity(editMeetings);
    }

    /**
     * View all users who have an account on the app.
     *
     * @param view the view users button
     */
    public void viewAllUsers(View view) {
        if(userIsNull())
            return;

        Intent goToUserList = new Intent(this, UserListActivity.class);
        goToUserList.putExtra("purpose", "VIEW_ALL");
        startActivity(goToUserList);
    }

    /**
     * Deletes all previous meetings from the server.
     *
     * @param view the delete previous meetings button
     */
    public void delAllPreviousMeetings(View view) {
        if(userIsNull())
            return;

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

    /**
     * Deletes all meetings from the server.
     *
     * @param view the delete meetings button
     */
    public void delAllMeetings(View view) {
        if(userIsNull())
            return;

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

    /**
     * Go to screen to see all past meetings that the current user attended.
     *
     * @param view the view past meetings button
     */
    public void viewPastMeetings(View view) {
        if(userIsNull())
            return;

        //For some reason, this takes a few moments if the user has attended at least 1 meeting that has been deleted
        Toast.makeText(getApplicationContext(), "Loading... May take a few moments.", Toast.LENGTH_LONG).show();
        ParseUser currentUser = ParseUser.getCurrentUser();
        String userId = currentUser.getObjectId();
        Intent displayMeetings = new Intent(this, ViewAllMeetings.class);
        displayMeetings.putExtra("purpose", "READ-USER_SPECIFIC");
        displayMeetings.putExtra("userId", userId);
        startActivity(displayMeetings);
    }

    /**
     * Go to screen to view meeting attendance for all meetings in memory.
     *
     * @param view the view meeting attendance button
     */
    public void viewMeetingAttendance(View view) {
        if(userIsNull())
            return;

        Intent goToMeetingList = new Intent(this, ViewAllMeetings.class);
        goToMeetingList.putExtra("purpose", "READ-VIEW_ATTENDANCE");
        startActivity(goToMeetingList);
    }

    /**
     * Go to log.
     *
     * @param view the view log button
     */
    public void viewLog(View view) {
        if(userIsNull())
            return;

        Intent goToLogActivity = new Intent(this, LogActivity.class);
        startActivity(goToLogActivity);
    }

    /**
     * Go to screen to change the time and location margins for meeting attendance.
     *
     * @param view the change margins button
     */
    public void changeMargins(View view) {
        if(userIsNull())
            return;

        Intent goToMarginsActivity = new Intent(this, EditMarginsActivity.class);
        startActivity(goToMarginsActivity);
    }

    /**
     * Refresh the profile screen (in case new meeting was created during current session).
     */
    private void refreshScreen() {
        if(userIsNull())
            return;

        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }

    /**
     * Check for rare instance when user may be not be logged in but still has access to this screen (this bug may have already been fixed by the time you see this).
     * If the current user is null, finish() all activities.
     *
     * @return whether current user is null
     */
    private boolean userIsNull() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getApplicationContext(), "The app may have had connection issues. Attempting to shut down app. Please restart the app if this does not work!", Toast.LENGTH_LONG).show();
            finishAffinity();
            return true;
        }
        return false;
    }
}