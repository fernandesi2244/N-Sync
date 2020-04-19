package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
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
            case "READ-USER_SPECIFIC":
                String userId = receivedIntent.getStringExtra("userId");
                initializeListWithReadingCapabilities(userId);
                break;
            case "READ-VIEW_ATTENDANCE":
                initializeListForViewingMeetingAttendance();
                break;
            default:
                Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_LONG).show();
                goBackToProfile();
        }
    }

    private void initializeListWithEditingCapabilities() {
        TextView activityLabel = findViewById(R.id.viewMeetingsLabel);
        activityLabel.setText(R.string.editMeetingsMessage);
        ListView listview = findViewById(R.id.listView);

        List<ParseObject> meetings;

        ParseQuery<ParseObject> queryAllMeetings = ParseQuery.getQuery("Meeting");
        queryAllMeetings.orderByDescending("meetingDate");
        try {
            meetings = queryAllMeetings.find();
            if (meetings == null)
                throw new Exception();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        final MeetingArrayAdapter adapter = new MeetingArrayAdapter(this,
                android.R.layout.simple_list_item_1, meetings);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                ParseObject item = (ParseObject) parent.getItemAtPosition(position);
                goToScheduleMeetingActivity(item.getObjectId());
            }

        });

        if (meetings.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.noMeetingsFoundMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void initializeListWithReadingCapabilities(String userId) {
        TextView activityLabel = findViewById(R.id.viewMeetingsLabel);
        activityLabel.setText(R.string.viewMeetingsMessage);
        ListView listview = findViewById(R.id.listView);

        List<ParseUser> users;
        ParseQuery<ParseUser> findUser = ParseUser.getQuery();
        findUser.whereEqualTo("objectId", userId);
        try {
            users = findUser.find();
            if (users == null || users.size() != 1)
                throw new Exception();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve user's meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        ParseUser user = users.get(0);
        List<Object> pointers = user.getList("meetingsAttended");
        if (pointers == null) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve user's meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        List<ParseObject> userMeetings = transform(pointers);

        Collections.sort(userMeetings, new Comparator<ParseObject>() {

            @Override
            public int compare(ParseObject first, ParseObject second) {
                if (first.getDate("meetingDate").before(second.getDate("meetingDate")))
                    return 1;
                else if (first.getDate("meetingDate").after(second.getDate("meetingDate")))
                    return -1;
                return 0;
            }
        });


        final MeetingArrayAdapter adapter = new MeetingArrayAdapter(this,
                android.R.layout.simple_list_item_1, userMeetings);
        listview.setAdapter(adapter);

        if (userMeetings.size() == 0) {
            Toast.makeText(getApplicationContext(), "The user has not attended any meetings.", Toast.LENGTH_LONG).show();
        }
    }

    private List<ParseObject> transform(List<Object> pointers) {
        List<ParseObject> meetings = new ArrayList<>();
        for (Object current : pointers) {
            ParseObject obj = (ParseObject) current;
            String id = obj.getObjectId();

            ParseQuery<ParseObject> getMeeting = ParseQuery.getQuery("Meeting");
            getMeeting.whereEqualTo("objectId", id);
            try {
                List<ParseObject> returnedResults = getMeeting.find();
                if (returnedResults == null) {
                    throw new Exception();
                }
                if (returnedResults.size() != 0)
                    meetings.add(returnedResults.get(0));
            } catch (ParseException e) {
                //Do nothing (Most likely, the meeting could not be found, indicating that it was probably deleted by an admin)
                //Therefore, we should keep iterating over the meetings to see if there are any meetings that haven't been deleted.
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meetings. Please try again later!", Toast.LENGTH_LONG).show();
                goBackToProfile();
                return new ArrayList<>();
            }
        }
        return meetings;
    }

    private void initializeListForViewingMeetingAttendance() {
        TextView activityLabel = findViewById(R.id.viewMeetingsLabel);
        activityLabel.setText(R.string.viewMeetingAttendanceMessage);
        ListView listview = findViewById(R.id.listView);

        List<ParseObject> meetings;

        ParseQuery<ParseObject> queryAllMeetings = ParseQuery.getQuery("Meeting");
        queryAllMeetings.orderByDescending("meetingDate");
        try {
            meetings = queryAllMeetings.find();
            if (meetings == null)
                throw new Exception();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve meetings. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        final MeetingArrayAdapter adapter = new MeetingArrayAdapter(this,
                android.R.layout.simple_list_item_1, meetings);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                ParseObject item = (ParseObject) parent.getItemAtPosition(position);
                goToUserListActivity(item.getObjectId());
            }

        });

        if (meetings.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.noMeetingsFoundMessage, Toast.LENGTH_LONG).show();
        }

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

    private void goToUserListActivity(String meetingId) {
        Intent goToUserList = new Intent(this, UserListActivity.class);
        goToUserList.putExtra("purpose", "VIEW_SPECIFIC");
        goToUserList.putExtra("meetingId", meetingId);
        startActivity(goToUserList);
    }
}
