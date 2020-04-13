package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class ViewAllMeetings extends AppCompatActivity {

    private String purpose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_meetings);

        Intent receivedIntent = getIntent();
        purpose = receivedIntent.getStringExtra("purpose");

        switch (purpose) {
            case "READ-WRITE":
                initializeListWithEditingCapabilities();
                break;
            case "READ":
                String userId = receivedIntent.getStringExtra("userId");
                initializeListWithReadingCapabilities(userId);
                break;
            default:
                Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_LONG).show();
                goBackToProfile();
        }
    }

    private void initializeListWithEditingCapabilities() {
        TextView activityLabel = findViewById(R.id.viewMeetingsLabel);
        activityLabel.setText(R.string.editMeetingsMessage);

        LinearLayout meetingsLayout = findViewById(R.id.meetingsLayout);
        meetingsLayout.removeAllViews();

        List<ParseObject> meetings;

        ParseQuery<ParseObject> queryAllMeetings = ParseQuery.getQuery("Meeting");
        queryAllMeetings.orderByDescending("meetingDate");
        try {
            meetings = queryAllMeetings.find();
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        if (meetings == null) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

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

                    goToScheduleMeetingActivity(objectId);
                }
            });

            meetingsLayout.addView(nextButton);
        }

        if(meetings.size()==0) {
            TextView noMeetingsView = new TextView(this);
            noMeetingsView.setGravity(Gravity.CENTER);
            noMeetingsView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            noMeetingsView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            noMeetingsView.setText(R.string.noMeetingsFoundMessage);
            noMeetingsView.setId(View.generateViewId());
            meetingsLayout.addView(noMeetingsView);
        }
    }

    private void initializeListWithReadingCapabilities(String userId) {
        TextView activityLabel = findViewById(R.id.viewMeetingsLabel);
        activityLabel.setText(R.string.viewMeetingsMessage);

        LinearLayout meetingsLayout = findViewById(R.id.meetingsLayout);
        meetingsLayout.removeAllViews();

        List<ParseUser> users;
        ParseQuery<ParseUser> findUser = ParseUser.getQuery();
        findUser.whereEqualTo("objectId", userId);
        try {
            users = findUser.find();
            if(users==null||users.size()!=1)
                throw new Exception();
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve user's meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        } catch (Exception e2) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve user's meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        ParseUser user = users.get(0);
        List<ParseObject> userMeetings = transform(user.getList("meetingsAttended"));

        Collections.sort(userMeetings, new Comparator<ParseObject>() {
            @Override
            public int compare(ParseObject first, ParseObject second) {
                if(first.getDate("meetingDate").before(second.getDate("meetingDate")))
                    return 1;
                return -1;
            }
        });

        for (ParseObject current : userMeetings) {
            TextView nextView = new TextView(this);
            nextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            nextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);

            String meetingDescription = current.getString("meetingDescription");
            Date nextMeetingDate = current.getDate("meetingDate");
            DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
            String date = df.format(nextMeetingDate);

            nextView.setText("Description: " + meetingDescription + "; Time: " + date + "; ID: " + current.getObjectId());
            nextView.setId(View.generateViewId());

            meetingsLayout.addView(nextView);

        }

        if(userMeetings.size()==0) {
            TextView noMeetingsView = new TextView(this);
            noMeetingsView.setGravity(Gravity.CENTER);
            noMeetingsView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            noMeetingsView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            noMeetingsView.setText(R.string.userHasNotAttendedAnyMeetingsMessage);
            noMeetingsView.setId(View.generateViewId());
            meetingsLayout.addView(noMeetingsView);
        }

    }

    private List<ParseObject> transform(List<Object> pointers) {
        List<ParseObject> meetings = new ArrayList<>();
        for(Object current: pointers) {
            ParseObject obj = (ParseObject)current;
            String id = obj.getObjectId();

            ParseQuery<ParseObject> getMeeting = ParseQuery.getQuery("Meeting");
            getMeeting.whereEqualTo("objectId", id);
            try {
                List<ParseObject> returnedResults = getMeeting.find();
                if(returnedResults==null||returnedResults.size()!=1) {
                    throw new Exception();
                }
                meetings.add(returnedResults.get(0));
            } catch(ParseException e){
                Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meetings. Please try again later!", Toast.LENGTH_LONG).show();
                goBackToProfile();
                return new ArrayList<>();
            } catch(Exception e2) {
                Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meetings. Please try again later!", Toast.LENGTH_LONG).show();
                goBackToProfile();
                return new ArrayList<>();
            }
        }
        return meetings;
    }

    private void goToScheduleMeetingActivity(String meetingID) {
        Intent goToScheduler = new Intent(this, ScheduleMeetingActivity.class);
        goToScheduler.putExtra("meetingID", meetingID);
        startActivity(goToScheduler);
    }

    private void goBackToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }
}
