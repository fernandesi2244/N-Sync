package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
        if (checked) {
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
        EditText email = findViewById(R.id.signInEmail);
        EditText password = findViewById(R.id.signInPassword);
        EditText passwordConfirm = findViewById(R.id.signInConfirmPassword);
        EditText adminPass = findViewById(R.id.signInAdminPass);

        //Make sure all fields are filled in
        if (name.getText().toString().isEmpty() ||
                email.getText().toString().isEmpty() ||
                password.getText().toString().isEmpty() ||
                passwordConfirm.getText().toString().isEmpty() ||
                department.isEmpty()) {

            Toast.makeText(getApplicationContext(), "Make sure all fields are filled in and try again!", Toast.LENGTH_LONG).show();
            return;

        }

        // Ensure email address is valid according to RFC 5322
        if (!email.getText().toString().matches("^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
            Toast.makeText(getApplicationContext(), "Please enter a valid email address!", Toast.LENGTH_LONG).show();
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

        if (isAdmin && !adminPass.getText().toString().equals(ADMIN_PASS)) {
            Toast.makeText(getApplicationContext(), "Admin passphrase is incorrect! Please try again.", Toast.LENGTH_LONG).show();
            return;
        }

        if (password.getText().toString().equals(passwordConfirm.getText().toString())) {
            String emailString = email.getText().toString().trim();
            user.setUsername(emailString);
            user.setEmail(emailString);
            user.setPassword(password.getText().toString());

            user.put("name", titleCase(name.getText().toString()).trim());
            user.put("department", department);
            user.put("noMeetingsAttended", 0L);
            user.put("meetingsAttended", new ArrayList<ParseObject>());
            user.put("isAdmin", isAdmin);

            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        //Toast.makeText(getApplicationContext(), "Successful Sign Up! Welcome "+titleCase(name.getText().toString())+"!", Toast.LENGTH_LONG).show();
                        //ParseUser currentUser = ParseUser.getCurrentUser();
                        //////////////////////////////////////////////////////////////////////////////////

                        ParseUser.logOut();
                        alertDisplayer("Account created successfully!", "Please verify your email before logging in.", false);
                        /*boolean successful = true;
                        try {
                            currentUser.save();
                        } catch (Exception e2) {
                            successful = false;
                        }
                        ParseUser.logOut();
                        if (successful)
                            alertDisplayer("Account created successfully!", "Please verify your email before logging in.", false);
                        else
                            alertDisplayer("Account created unsuccessfully.", "Something went wrong when creating your account. Please contact an administrator to resolve this issue. After contacting an admin, please verify your email before logging in.", false);
                        */
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
        for (String word : words) {
            if (!word.isEmpty()) {
                String temp = Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
                newString += temp + " ";
            }
        }
        return newString.trim();
    }

    private void alertDisplayer(String title, String message, final boolean error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (!error) {
                            Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                });
        AlertDialog ok = builder.create();
        ok.show();
    }
}
