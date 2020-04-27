package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

    public static final int MILLISECONDS_PER_MINUTE = 60_000;

    private static long marginInMinutes;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location currentLoc;
    private ParseObject clickedMeeting;
    private float locationMargin;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_sign_in);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMeetingsList();
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

    private void setUpMeetingsList() {
        LinearLayout meetingsLayout = findViewById(R.id.meetingsLayout);
        meetingsLayout.removeAllViews();

        long marginInMilliseconds = 24 * 60 * MILLISECONDS_PER_MINUTE; //milliseconds in a day
        Date earliestDate = new Date();
        earliestDate.setTime(new Date().getTime() - marginInMilliseconds);

        ParseUser currentUser = ParseUser.getCurrentUser();
        try {
            currentUser = ParseUser.getCurrentUser();
            if(currentUser==null)
                throw new Exception();
        } catch(Exception e) {
            //Perhaps the user is zooming through the app without giving the app enough time to load.
            Toast.makeText(getApplicationContext(), "Something wrong happened... Please try again later!", Toast.LENGTH_LONG).show();
            goToProfile();
            finish();
        }
        String[] acceptableDepartments = {"General", "general", currentUser.getString("department")};

        ParseQuery<ParseObject> findEligibleMeetings = ParseQuery.getQuery("Meeting");
        findEligibleMeetings.whereContainedIn("audience", Arrays.asList(acceptableDepartments));
        findEligibleMeetings.whereGreaterThanOrEqualTo("meetingDate", earliestDate);
        findEligibleMeetings.orderByDescending("meetingDate");

        try {
            List<ParseObject> meetings = findEligibleMeetings.find();
            if (meetings.size() == 0)
                throw new Exception("failed");

            for (ParseObject current : meetings) {
                Button nextButton = new Button(this);
                nextButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                nextButton.setGravity(Gravity.START);

                String meetingDescription = current.getString("meetingDescription");
                Date nextMeetingDate = current.getDate("meetingDate");
                DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
                String date = df.format(nextMeetingDate);
                String html = "<b>Description:</b> " + meetingDescription + "<br/><b>Time:</b> " + date + "<br/><b>ID:</b> " + current.getObjectId();
                Spanned durationSpanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
                nextButton.setText(durationSpanned);
                nextButton.setId(View.generateViewId());
                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Button clickedButton = (Button) v;
                        String btnText = clickedButton.getText().toString();
                        String meetingId = btnText.split("ID: ")[1];
                        displayMeetingDetails(meetingId);
                    }
                });

                meetingsLayout.addView(nextButton);
            }

        } catch (Exception e) {
            TextView noMeetingsTextView = new TextView(this);
            noMeetingsTextView.setGravity(Gravity.CENTER);
            noMeetingsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            noMeetingsTextView.setText(R.string.noMeetingsScheduledMessage);
            noMeetingsTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            noMeetingsTextView.setId(View.generateViewId());
            meetingsLayout.addView(noMeetingsTextView);
        }
    }

    private void displayMeetingDetails(String meetingId) {
        Intent displayMeeting = new Intent(this, DisplayMeetingActivity.class);
        displayMeeting.putExtra("meetingId", meetingId);
        startActivity(displayMeeting);
    }

    private void goToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }

    private void refreshScreen() {
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }
}
