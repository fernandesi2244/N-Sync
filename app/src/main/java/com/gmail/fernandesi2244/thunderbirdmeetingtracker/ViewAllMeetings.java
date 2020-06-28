package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewAllMeetings extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private String purpose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_meetings);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeToRefreshViewAllMeetings);
        swipeRefreshLayout.setOnRefreshListener(this);

        // "purpose" defines functionality of this activity
        Intent receivedIntent = getIntent();
        purpose = receivedIntent.getStringExtra("purpose");

        switch (purpose) {
            case "READ-WRITE":
                // For editing existing meetings
                initializeListWithEditingCapabilities();
                break;
            case "READ-USER_SPECIFIC":
                // For seeing meetings that a specific user attended
                String userId = receivedIntent.getStringExtra("userId");
                initializeListWithReadingCapabilities(userId);
                break;
            case "READ-VIEW_ATTENDANCE":
                // For viewing attendance of a specific meeting
                initializeListForViewingMeetingAttendance();
                break;
            default:
                Toast.makeText(getApplicationContext(), "Something went wrong...", Toast.LENGTH_LONG).show();
                goBackToProfile();
        }
    }

    /**
     * Allows user to use the back arrow.
     *
     * @param item
     * @return
     */
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

    /**
     * Initialize list of meetings from server so that admin can choose one to edit.
     */
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

    /**
     * Initialize list of meetings that a specific user attended.
     *
     * @param userId the ID of the user whose meetings are being requested
     */
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

        // Meetings requested above are actually pointers, not the ParseObjects themselves; therefore, we need to retrieve the actual ParseObjects associated with those pointers.
        List<ParseObject> userMeetings = transform(pointers);

        // Show most recent meetings first (sorted by meeting date in descending order)
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

    /**
     * Given a list of pointers (that point to Meeting objects), transform them into a list of Meeting ParseObjects.
     *
     * @param pointers the list of pointers to transform
     * @return the transformed list of Meeting ParseObjects
     */
    private List<ParseObject> transform(List<Object> pointers) {
        List<ParseObject> meetings = new ArrayList<>();
        for (Object current : pointers) {
            // Downcast to get general ParseObject
            ParseObject obj = (ParseObject) current;
            String id = obj.getObjectId();

            // Find Meeting ParseObject with the retrieved object ID
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

    /**
     * Initialize list of past meetings so that admin can see who attended.
     */
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

    /**
     * Go to meeting scheduler activity for editing purposes.
     *
     * @param meetingID the ID of the Meeting ParseObject to edit
     */
    private void goToScheduleMeetingActivity(String meetingID) {
        Intent goToScheduler = new Intent(this, ScheduleMeetingActivity.class);
        goToScheduler.putExtra("meetingID", meetingID);
        startActivity(goToScheduler);
    }

    /**
     * Go back to the profile screen.
     */
    private void goBackToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }

    /**
     * Pull up the user list screen with the intent of displaying the users who attended a specific meeting.
     *
     * @param meetingId the meeting ID to check attendance for
     */
    private void goToUserListActivity(String meetingId) {
        Intent goToUserList = new Intent(this, UserListActivity.class);
        goToUserList.putExtra("purpose", "VIEW_SPECIFIC");
        goToUserList.putExtra("meetingId", meetingId);
        startActivity(goToUserList);
    }

    /**
     * Refresh screen part 1
     */
    @Override
    public void onRefresh() {
        refreshScreen();
    }

    /**
     * Refresh screen part 2
     */
    private void refreshScreen() {
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
    }
}
