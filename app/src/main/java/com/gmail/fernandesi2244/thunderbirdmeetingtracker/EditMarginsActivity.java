package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

public class EditMarginsActivity extends AppCompatActivity {

    public static final long DEFAULT_LOCATION_MARGIN = 250L;
    public static final long DEFAULT_TIME_MARGIN = 10;
    public static final String LocationClassObjectId = "s5qyTqdbHY";
    public static final String timeClassObjectId = "ZCHh4cadL1";

    private ParseObject locationClassObject;
    private ParseObject timeClassObject;

    private long locMarginInMeters = -1;
    private long timeMarginInMinutes = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_margins);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setUpMargins();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUpMargins() {
        EditText locEditText = findViewById(R.id.chooseMeters);
        EditText timeEditText = findViewById(R.id.chooseMinutes);

        List<ParseObject> locationMarginObjects;
        ParseQuery<ParseObject> getLocMargin = ParseQuery.getQuery("LocationMargin");
        getLocMargin.whereEqualTo("objectId", LocationClassObjectId);
        try {
            locationMarginObjects = getLocMargin.find();
            if (locationMarginObjects.size() == 0)
                throw new Exception();
        } catch (Exception e) {
            goToProfile("Sorry, but the margins couldn't be retrieved at this moment. Please try again later!");
            return;
        }

        locationClassObject = locationMarginObjects.get(0);
        locMarginInMeters = locationClassObject.getNumber("locationMarginInMeters").longValue();

        List<ParseObject> timeMarginObjects;
        ParseQuery<ParseObject> getTimeMargin = ParseQuery.getQuery("MeetingAttendanceMarginOfError");
        getTimeMargin.whereEqualTo("objectId", timeClassObjectId);
        try {
            timeMarginObjects = getTimeMargin.find();
            if (timeMarginObjects.size() == 0)
                throw new Exception();
        } catch (Exception e) {
            goToProfile("Sorry, but the margins couldn't be retrieved at this moment. Please try again later!");
            return;
        }

        timeClassObject = timeMarginObjects.get(0);
        timeMarginInMinutes = timeClassObject.getNumber("marginInMinutes").longValue();

        String locEditTextString = String.format("%d", locMarginInMeters);
        String timeEditTextString = String.format("%d", timeMarginInMinutes);
        locEditText.setText(locEditTextString);
        timeEditText.setText(timeEditTextString);
    }

    public void updateMargins(View view) {
        if (locationClassObject == null || timeClassObject == null || locMarginInMeters == -1 || timeMarginInMinutes == -1) {
            goToProfile("Sorry, but the margins couldn't be retrieved at this moment. Please try again later!");
            return;
        }
        EditText locEditText = findViewById(R.id.chooseMeters);
        EditText timeEditText = findViewById(R.id.chooseMinutes);

        long locMargin, timeMargin;
        try {
            locMargin = Long.parseLong(locEditText.getText().toString());
            timeMargin = Long.parseLong(timeEditText.getText().toString());
            if (locMarginInMeters == locMargin && timeMarginInMinutes == timeMargin) {
                goToProfile("No changes made.");
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "You need to enter non-negative, non-decimal numerical values!", Toast.LENGTH_LONG).show();
            return;
        }

        if (locMargin < 0 || timeMargin < 0) {
            Toast.makeText(getApplicationContext(), "You need to enter non-negative, non-decimal numerical values!", Toast.LENGTH_LONG).show();
            return;
        }

        locationClassObject.put("locationMarginInMeters", locMargin);
        timeClassObject.put("marginInMinutes", timeMargin);

        locationClassObject.saveEventually();
        timeClassObject.saveEventually();

        goToProfile("Margins successfully updated.");
    }

    private void goToProfile(String toastMessage) {
        Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }


}
