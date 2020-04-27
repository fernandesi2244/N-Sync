package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DisplayMeetingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final float DEFAULT_MAP_ZOOM = 16f;
    private static final double DEFAULT_LATITUDE = 29.456885f;
    private static final double DEFAULT_LONGITUDE = -98.357193f;
    public static final String DisplayMeetingActivityID = "DisplayMeetingActivity";

    private ParseObject clickedMeeting;
    private ParseGeoPoint nextMeetingLocation;
    private boolean requiresPassword;
    private String meetingPassword;
    private Animation fadeIn;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location currentLoc;
    private boolean userLate;

    private GoogleMap locMap;
    private TextView meetingDescription;
    private TextView meetingTime;
    private TextView meetingLocation;
    private EditText passwordEditText;
    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_meeting);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        meetingDescription = findViewById(R.id.displayMeetingDescription);
        meetingTime = findViewById(R.id.displayMeetingTime);
        meetingLocation = findViewById(R.id.displayMeetingLocation);
        passwordEditText = findViewById(R.id.enterMeetingPassword);
        signInButton = findViewById(R.id.meetingPageSignInButton);

        fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);

        hideOptionalLayouts();
        fadeInMeetingDetails();

        Intent receivedIntent = getIntent();
        String meetingId = receivedIntent.getStringExtra("meetingId");

        findMeetingObject(meetingId);
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
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            case R.id.menu_refresh:
                refreshScreen();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void findMeetingObject(String meetingId) {
        ParseQuery<ParseObject> getMeeting = ParseQuery.getQuery("Meeting");
        getMeeting.whereEqualTo("objectId", meetingId);
        getMeeting.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (objects.size() == 1) {
                        clickedMeeting = objects.get(0);
                        initMap();
                    } else {
                        Toast.makeText(getApplicationContext(), "Something went wrong when accessing the desired meeting. Please try again!", Toast.LENGTH_LONG).show();
                        goBackToMeetingList();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Something went wrong when accessing the desired meeting. Please try again!", Toast.LENGTH_LONG).show();
                    goBackToMeetingList();
                }
            }
        });
    }

    private void displayMeetingDetails() {
        try {
            String descriptionHtml = "<b>Description:</b> " + clickedMeeting.getString("meetingDescription");
            Spanned durationSpannedDescription = Html.fromHtml(descriptionHtml, Html.FROM_HTML_MODE_LEGACY);
            meetingDescription.setText(durationSpannedDescription);

            Date nextMeetingDate = clickedMeeting.getDate("meetingDate");
            DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
            String timeHtml = "<b>Time:</b> " + df.format(nextMeetingDate);
            Spanned durationSpannedTime = Html.fromHtml(timeHtml, Html.FROM_HTML_MODE_LEGACY);
            meetingTime.setText(durationSpannedTime);

            boolean meetingIsRemote = clickedMeeting.getBoolean("isRemote");

            if (!meetingIsRemote) {
                nextMeetingLocation = clickedMeeting.getParseGeoPoint("meetingLocation");
                getAddressFromLocation(nextMeetingLocation.getLatitude(), nextMeetingLocation.getLongitude());
                setMapLocation();
                LinearLayout mapLayout = findViewById(R.id.mapDisplayLinLayout);
                mapLayout.setVisibility(View.VISIBLE);
                mapLayout.startAnimation(fadeIn);
            } else {
                meetingPassword = clickedMeeting.getString("meetingPassword");
                if (!meetingPassword.equalsIgnoreCase("none")) {
                    requiresPassword = true;
                    passwordLayoutFadeIn();
                }

                meetingLocation.setText(R.string.locationRemoteMessage);
                setMapLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
            }
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(getApplicationContext(), "Something went wrong when accessing the desired meeting. Please try again!", Toast.LENGTH_LONG).show();
            goBackToMeetingList();
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
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

    protected void checkPermissions() {
        if (!hasLocationPermissions()) {
            Intent goToRequestPage = new Intent(this, RequestPermissionsActivity.class);
            goToRequestPage.putExtra("sender", DisplayMeetingActivityID);
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

    public void verifyMeetingAttendance(View view) {
        boolean isRemote = clickedMeeting.getBoolean("isRemote");
        long marginInMinutes;

        ParseQuery<ParseObject> getMargin = ParseQuery.getQuery("MeetingAttendanceMarginOfError");
        getMargin.whereEqualTo("objectId", "ZCHh4cadL1");
        try {
            List<ParseObject> parseObjects = getMargin.find();
            if (parseObjects.size() > 0) {
                marginInMinutes = parseObjects.get(0).getNumber("marginInMinutes").longValue();
            } else {
                Toast.makeText(getApplicationContext(), "Error: Meeting attendance time margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Error: Meeting attendance time margin retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
            return;
        }

        long marginInMilliseconds = marginInMinutes * MeetingSignInActivity.MILLISECONDS_PER_MINUTE;
        Date meetingDate = clickedMeeting.getDate("meetingDate");

        Date currentDate = new Date();
        Date beforeDate = new Date();
        beforeDate.setTime(meetingDate.getTime() - marginInMilliseconds);
        Date afterDate = new Date();
        afterDate.setTime(meetingDate.getTime() + marginInMilliseconds);

        boolean timingIsGood = false;

        if (currentDate.getTime() >= beforeDate.getTime()) {
            timingIsGood = true;
        }

        userLate = currentDate.getTime() > afterDate.getTime();

        ParseUser currentUser = ParseUser.getCurrentUser();

        if (isRemote) {
            if (timingIsGood) {
                ArrayList<ParseObject> meetingsAttendedByUser = (ArrayList<ParseObject>) currentUser.get("meetingsAttended");

                if (meetingsAttendedByUser == null) {
                    Toast.makeText(getApplicationContext(), "Something went wrong during online retrieval of data. Please try again later!", Toast.LENGTH_LONG).show();
                    return;
                }

                //Check if user already signed into the meeting
                for (ParseObject mtng : meetingsAttendedByUser) {
                    if (mtng.getObjectId().equals(clickedMeeting.getObjectId())) {
                        Toast.makeText(getApplicationContext(), "You already signed into this meeting!", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                //Check if the user entered in the right meeting password
                String userPasswordAttempt = passwordEditText.getText().toString();
                if (requiresPassword) {
                    if (!userPasswordAttempt.equals(meetingPassword)) {
                        Toast.makeText(getApplicationContext(), "The meeting password is incorrect. Please try again!", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                //////////////////////////////////////////////////////////////////////
                // AT THIS POINT, THE USER CHECKS OUT AND IS AUTHORIZED TO SIGN IN! //
                //////////////////////////////////////////////////////////////////////

                meetingsAttendedByUser.add(clickedMeeting);

                currentUser.put("meetingsAttended", meetingsAttendedByUser);
                currentUser.put("noMeetingsAttended", currentUser.getLong("noMeetingsAttended") + 1);

                currentUser.saveEventually();

                ArrayList<ParseUser> meetingUsers = (ArrayList<ParseUser>) clickedMeeting.get("usersThatAttended");
                meetingUsers.add(currentUser);
                clickedMeeting.put("usersThatAttended", meetingUsers);

                if (userLate) {
                    ArrayList<ParseUser> lateMeetingUsers = (ArrayList<ParseUser>) clickedMeeting.get("usersThatAttendedLate");
                    lateMeetingUsers.add(currentUser);
                    clickedMeeting.put("usersThatAttendedLate", lateMeetingUsers);
                }

                clickedMeeting.saveEventually();

                if (userLate)
                    Toast.makeText(getApplicationContext(), "You successfully signed into the meeting late!", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(), "You successfully signed into the meeting on time!", Toast.LENGTH_LONG).show();
                goToProfile();
            } else {
                Toast.makeText(getApplicationContext(), "Sorry, but you are too early to check into the meeting. Please contact an administrator if this is a concern.", Toast.LENGTH_LONG).show();
            }
        } else {
            if (!hasLocationPermissions()) {
                checkPermissions();
            } else {
                if (timingIsGood) {
                    getDeviceLocation();
                } else {
                    Toast.makeText(getApplicationContext(), "Sorry, but you are too early to check into the meeting. Please contact an administrator if this is a concern.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void resumeVerificationProcessForDeviceLocation() {
        if (currentLoc == null) {
            Toast.makeText(getApplicationContext(), "Could not retrieve phone's location. Please try again!", Toast.LENGTH_LONG).show();
            return;
        }
        float locationMargin;
        try {
            ParseGeoPoint meetingLocInParse = clickedMeeting.getParseGeoPoint("meetingLocation");
            Location meetingLoc = new Location("");
            meetingLoc.setLatitude(meetingLocInParse.getLatitude());
            meetingLoc.setLongitude(meetingLocInParse.getLongitude());

            ParseQuery<ParseObject> getMargin = ParseQuery.getQuery("LocationMargin");
            getMargin.whereEqualTo("objectId", "s5qyTqdbHY");
            try {
                List<ParseObject> parseObjects = getMargin.find();
                if (parseObjects.size() > 0) {
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
            if (currentUser == null) {
                Toast.makeText(getApplicationContext(), "It appears as though you aren't signed in. Sign in and try again!", Toast.LENGTH_LONG).show();
                goToLoginPage();
            }

            float actualDistance = currentLoc.distanceTo(meetingLoc);
            if (actualDistance < locationMargin) {
                ArrayList<ParseObject> meetingsAttendedByUser = (ArrayList<ParseObject>) currentUser.get("meetingsAttended");

                if (meetingsAttendedByUser == null) {
                    Toast.makeText(getApplicationContext(), "Something went wrong during online retrieval of data. Please try again later!", Toast.LENGTH_LONG).show();
                    return;
                }

                //Check if user already signed into the meeting
                for (ParseObject mtng : meetingsAttendedByUser) {
                    if (mtng.getObjectId().equals(clickedMeeting.getObjectId())) {
                        Toast.makeText(getApplicationContext(), "You already signed into this meeting!", Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                //////////////////////////////////////////////////////////////////////
                // AT THIS POINT, THE USER CHECKS OUT AND IS AUTHORIZED TO SIGN IN! //
                //////////////////////////////////////////////////////////////////////

                meetingsAttendedByUser.add(clickedMeeting);

                currentUser.put("meetingsAttended", meetingsAttendedByUser);
                currentUser.put("noMeetingsAttended", currentUser.getLong("noMeetingsAttended") + 1);

                currentUser.saveEventually();

                ArrayList<ParseUser> meetingUsers = (ArrayList<ParseUser>) clickedMeeting.get("usersThatAttended");
                meetingUsers.add(currentUser);
                clickedMeeting.put("usersThatAttended", meetingUsers);

                if (userLate) {
                    ArrayList<ParseUser> lateMeetingUsers = (ArrayList<ParseUser>) clickedMeeting.get("usersThatAttendedLate");
                    lateMeetingUsers.add(currentUser);
                    clickedMeeting.put("usersThatAttendedLate", lateMeetingUsers);
                }

                clickedMeeting.saveEventually();

                if (userLate)
                    Toast.makeText(getApplicationContext(), "You successfully signed into the meeting late!", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(), "You successfully signed into the meeting on time!", Toast.LENGTH_LONG).show();
                goToProfile();
            } else {
                Toast.makeText(getApplicationContext(), "Sorry, but you are not close enough to the meeting location to confirm your presence. Please contact an administrator if this is a concern.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception loc_e) {
            Toast.makeText(getApplicationContext(), "Something went wrong while verifying your location. Please try again later or contact an administrator if this is a concern.", Toast.LENGTH_LONG).show();
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapDisplay);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        locMap = googleMap;
        displayMeetingDetails();
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

    private void hideOptionalLayouts() {
        LinearLayout passwordLinLayout = findViewById(R.id.meetingPasswordLinearLayout);
        LinearLayout mapLayout = findViewById(R.id.mapDisplayLinLayout);
        passwordLinLayout.setVisibility(View.GONE);
        mapLayout.setVisibility(View.GONE);
    }

    private void fadeInMeetingDetails() {
        meetingDescription.startAnimation(fadeIn);
        meetingTime.startAnimation(fadeIn);
        meetingLocation.startAnimation(fadeIn);
        signInButton.startAnimation(fadeIn);
    }

    private void passwordLayoutFadeIn() {
        LinearLayout passwordLinLayout = findViewById(R.id.meetingPasswordLinearLayout);
        passwordLinLayout.setVisibility(View.VISIBLE);
        passwordLinLayout.startAnimation(fadeIn);
    }

    private void goBackToMeetingList() {
        Intent goBackToMeetings = new Intent(this, MeetingSignInActivity.class);
        startActivity(goBackToMeetings);
    }

    private void refreshScreen() {
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }

    private void goToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }

    private void goToLoginPage() {
        Intent goToLogin = new Intent(this, MainActivity.class);
        goToLogin.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(goToLogin);
    }
}
