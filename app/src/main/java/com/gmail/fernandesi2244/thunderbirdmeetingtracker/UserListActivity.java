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

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Intent receivedIntent = getIntent();
        String purpose = receivedIntent.getStringExtra("purpose");

        switch (purpose) {
            case "VIEW_SPECIFIC":
                String meetingId = receivedIntent.getStringExtra("meetingId");
                showUsersOfMeeting(meetingId);
                break;
            case "VIEW_ALL":
            default:
                initUserList();
        }
    }

    private void initUserList() {
        ListView listview = findViewById(R.id.userListView);

        List<ParseUser> users;

        ParseQuery<ParseUser> queryAllUsers = ParseUser.getQuery();
        queryAllUsers.orderByAscending("name");
        try {
            users = queryAllUsers.find();
            if (users == null || users.size() == 0)
                throw new Exception();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve users. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        final UserArrayAdapter adapter = new UserArrayAdapter(this,
                android.R.layout.simple_list_item_1, users);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                ParseUser item = (ParseUser) parent.getItemAtPosition(position);
                displayMeetingsUserAttended(item.getObjectId());
            }

        });

        if (users.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.noUsersFoundMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void showUsersOfMeeting(String meetingId) {
        ListView listview = findViewById(R.id.userListView);

        List<ParseObject> meetings;
        ParseQuery<ParseObject> findMeeting = ParseQuery.getQuery("Meeting");
        findMeeting.whereEqualTo("objectId", meetingId);
        try {
            meetings = findMeeting.find();
            if (meetings.size() != 1)
                throw new Exception();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve meeting. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        ParseObject meeting = meetings.get(0);
        List<Object> pointers = meeting.getList("usersThatAttended");
        List<ParseUser> users = transform(pointers);

        Collections.sort(users, new Comparator<ParseUser>() {
            @Override
            public int compare(ParseUser first, ParseUser second) {
                return first.getString("name").compareTo(second.getString("name"));
            }
        });

        final UserArrayAdapter adapter = new UserArrayAdapter(this,
                android.R.layout.simple_list_item_1, users);
        listview.setAdapter(adapter);

        if (users.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.noUsersFoundForMeetingMessage, Toast.LENGTH_LONG).show();
        }
    }

    private List<ParseUser> transform(List<Object> pointers) {
        List<ParseUser> meetings = new ArrayList<>();
        for (Object current : pointers) {
            ParseUser obj = (ParseUser) current;
            String id = obj.getObjectId();

            ParseQuery<ParseUser> getUsers = ParseUser.getQuery();
            getUsers.whereEqualTo("objectId", id);
            try {
                List<ParseUser> returnedResults = getUsers.find(); //throws ParseException if no objects found
                if (returnedResults == null) {
                    throw new Exception();
                }
                if (returnedResults.size() != 0)
                    meetings.add(returnedResults.get(0));
            } catch (ParseException e) {
                //Do nothing (Most likely, the user could not be found, indicating that it was probably deleted at some point)
                //Therefore, we should keep iterating over the users to see if there are any users that haven't been deleted.
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the users. Please try again later!", Toast.LENGTH_LONG).show();
                goBackToProfile();
                return new ArrayList<>();
            }
        }
        return meetings;
    }

    private void displayMeetingsUserAttended(String objectId) {
        Intent displayMeetings = new Intent(this, ViewAllMeetings.class);
        displayMeetings.putExtra("purpose", "READ-USER_SPECIFIC");
        displayMeetings.putExtra("userId", objectId);
        startActivity(displayMeetings);
    }

    private void goBackToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }
}
