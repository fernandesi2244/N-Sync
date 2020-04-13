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

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        initUserList();
    }

    private void initUserList() {
        LinearLayout usersLayout = findViewById(R.id.usersLayout);
        usersLayout.removeAllViews();

        List<ParseUser> users;

        ParseQuery<ParseUser> queryAllUsers = ParseUser.getQuery();
        queryAllUsers.orderByAscending("name");
        try {
            users = queryAllUsers.find();
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve users. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        if (users == null) {
            Toast.makeText(getApplicationContext(), "Error: Could not retrieve users. Please try again later.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        for (ParseUser current : users) {
            Button nextButton = new Button(this);
            nextButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            String name = current.getString("name");
            String department = current.getString("department");

            nextButton.setText("Name: " + name + "; Department: " + department + "; ID: " + current.getObjectId());
            nextButton.setId(View.generateViewId());
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Button clickedButton = (Button) v;
                    String btnText = clickedButton.getText().toString();
                    String objectId = btnText.split("ID: ")[1];

                    displayMeetingsUserAttended(objectId);
                }
            });

            usersLayout.addView(nextButton);
        }

        if(users.size()==0) {
            TextView noUsersTextView = new TextView(this);
            noUsersTextView.setGravity(Gravity.CENTER);
            noUsersTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            noUsersTextView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            noUsersTextView.setText(R.string.noUsersFoundMessage);
            noUsersTextView.setId(View.generateViewId());
            usersLayout.addView(noUsersTextView);
        }
    }

    private void displayMeetingsUserAttended(String objectId) {
        Intent displayMeetings = new Intent(this, ViewAllMeetings.class);
        displayMeetings.putExtra("purpose", "READ");
        displayMeetings.putExtra("userId", objectId);
        startActivity(displayMeetings);
    }

    private void goBackToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }
}
