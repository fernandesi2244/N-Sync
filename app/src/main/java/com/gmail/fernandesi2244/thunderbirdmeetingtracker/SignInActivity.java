package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.ArrayList;
import java.util.Arrays;

public class SignInActivity extends AppCompatActivity implements OnItemSelectedListener {

    private String department;
    private boolean isAdmin;

    private static String ADMIN_PASS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //Set up departments drop-down list
        Spinner spinner = findViewById(R.id.departmentMenu);

        // Spinner click listener
        spinner.setOnItemSelectedListener(this);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.departmentsArray, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);


        EditText userAdminPass = findViewById(R.id.signInAdminPass);
        userAdminPass.setVisibility(View.GONE);
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

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // On selecting a spinner item
        department = parent.getItemAtPosition(pos).toString();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public void onCheckboxClicked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        isAdmin = checked;

        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);

        EditText userAdminPass = findViewById(R.id.signInAdminPass);
        if(checked) {
            userAdminPass.setVisibility(View.VISIBLE);
            userAdminPass.startAnimation(fadeIn);
        } else {
            userAdminPass.clearAnimation();
            userAdminPass.setVisibility(View.GONE);
        }
    }

    public void signIn(View view) {
        ParseUser user = new ParseUser();
        // Set the user's general account info
        final EditText name = findViewById(R.id.signInName);
        EditText username = findViewById(R.id.signInUsername);
        EditText password = findViewById(R.id.signInPassword);
        EditText passwordConfirm = findViewById(R.id.signInConfirmPassword);
        EditText adminPass = findViewById(R.id.signInAdminPass);

        //Make sure all fields are filled in
        if (name.getText().toString().isEmpty() ||
            username.getText().toString().isEmpty() ||
            password.getText().toString().isEmpty() ||
            passwordConfirm.getText().toString().isEmpty() ||
            department.isEmpty()) {

                Toast.makeText(getApplicationContext(), "Make sure all fields are filled in and try again!", Toast.LENGTH_LONG).show();
                return;

        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("AdminPass");
        query.whereEqualTo("objectId", "Xb43zKXoSU");
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (e == null) {
                    ADMIN_PASS = object.getString("pass");
                } else {
                    // Something is wrong
                    Toast.makeText(getApplicationContext(), "Error: Admin password retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        });

        if(isAdmin&&!adminPass.getText().toString().equals(ADMIN_PASS)) {
            Toast.makeText(getApplicationContext(), "Admin passphrase is incorrect! Please try again.", Toast.LENGTH_LONG).show();
            return;
        }

        if(password.getText().toString().equals(passwordConfirm.getText().toString())) {
            final String USERNAME_STRING = username.getText().toString();
            user.setUsername(USERNAME_STRING);
            user.setPassword(password.getText().toString());

            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(getApplicationContext(), "Successful Sign Up! Welcome "+titleCase(name.getText().toString())+"!", Toast.LENGTH_LONG).show();
                        ParseUser currentUser = ParseUser.getCurrentUser();
                        //////////////////////////////////////////////////////////////////////////////////
                        currentUser.put("name", titleCase(name.getText().toString()));
                        currentUser.put("department", department);
                        currentUser.put("noMeetingsAttended", 0L);
                        currentUser.put("meetingsAttended", new ArrayList<ParseObject>());
                        if(isAdmin)
                            currentUser.put("isAdmin", true);
                        else
                            currentUser.put("isAdmin", false);

                        currentUser.saveInBackground();
                        //////////////////////////////////////////////////////////////////////////////////
                        Intent myIntent = new Intent(SignInActivity.this, ProfileActivity.class);
                        startActivity(myIntent);
                    } else {
                        ParseUser.logOut();
                        Toast.makeText(SignInActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "The passwords do not match, please try again.", Toast.LENGTH_LONG).show();
        }
    }

    private String titleCase(String input) {
        String[] words = input.split("\\s+");
        String newString = "";
        for(String word: words) {
            String temp = Character.toUpperCase(word.charAt(0))+word.substring(1).toLowerCase();
            newString+=temp+" ";
        }
        return newString.trim();
    }
}
