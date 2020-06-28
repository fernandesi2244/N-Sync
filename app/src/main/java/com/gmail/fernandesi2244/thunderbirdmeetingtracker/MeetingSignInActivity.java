package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.content.Intent;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MeetingSignInActivity extends AppCompatActivity {

    public static final int MILLISECONDS_PER_MINUTE = 60_000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_sign_in);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Whenever user resumes this activity, make sure to update the meetings list.
     */
    @Override
    public void onResume() {
        super.onResume();
        setUpMeetingsList();
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

    /**
     * Sets up the meeting list to choose a meeting to sign in to.
     */
    private void setUpMeetingsList() {
        if(userIsNull())
            return;

        LinearLayout meetingsLayout = findViewById(R.id.meetingsLayout);
        meetingsLayout.removeAllViews();

        // Only show meetings that are at least as recent as 24 hours ago
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

        // Only display meetings that are for the user (not for a different department)
        String[] acceptableDepartments = {"General", "general", currentUser.getString("department")};

        ParseQuery<ParseObject> findEligibleMeetings = ParseQuery.getQuery("Meeting");
        findEligibleMeetings.whereContainedIn("audience", Arrays.asList(acceptableDepartments));
        findEligibleMeetings.whereGreaterThanOrEqualTo("meetingDate", earliestDate);
        findEligibleMeetings.orderByDescending("meetingDate");

        try {
            List<ParseObject> meetings = findEligibleMeetings.find();
            if (meetings.size() == 0)
                throw new Exception("failed");

            // Adds button for each meeting available
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
            // No meetings available; display appropriate message
            TextView noMeetingsTextView = new TextView(this);
            noMeetingsTextView.setGravity(Gravity.CENTER);
            noMeetingsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            noMeetingsTextView.setText(R.string.noMeetingsScheduledMessage);
            noMeetingsTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            noMeetingsTextView.setId(View.generateViewId());
            meetingsLayout.addView(noMeetingsTextView);
        }
    }

    /**
     * Go to screen to sign into the meeting that was selected.
     *
     * @param meetingId
     */
    private void displayMeetingDetails(String meetingId) {
        if(userIsNull())
            return;

        Intent displayMeeting = new Intent(this, DisplayMeetingActivity.class);
        displayMeeting.putExtra("meetingId", meetingId);
        startActivity(displayMeeting);
    }

    /**
     * Go back to the profile screen.
     */
    private void goToProfile() {
        if(userIsNull())
            return;

        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }

    /**
     * Refresh the list of available meetings.
     */
    private void refreshScreen() {
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
