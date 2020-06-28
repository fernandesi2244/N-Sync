package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the sign up screen from which the user can create a new account.
 */
public class SignInActivity extends AppCompatActivity implements OnItemSelectedListener {

    private String department;
    private boolean isAdmin;

    private static String ADMIN_PASS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Enable back button
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Set up departments drop-down list
        Spinner spinner = findViewById(R.id.departmentMenu);

        // Set up department spinner
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.departmentsArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Initially hide password box for administrators
        EditText userAdminPass = findViewById(R.id.signInAdminPass);
        userAdminPass.setVisibility(View.GONE);
    }

    /**
     * Go back to the previous screen if the back button is selected.
     *
     * @param item the menu option that was selected (in our case: back button)
     * @return whether back button successfully worked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Back button clicked; go to parent activity
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Update department choice when department selection is updated.
     *
     * @param parent
     * @param view
     * @param pos
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        department = parent.getItemAtPosition(pos).toString();
    }

    /**
     * Required for implementation; not used.
     *
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    /**
     * If the user selects that they are an admin, show the admin passcode entry box.
     *
     * @param view the checkbox to select whether or not the new user is an admin
     */
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

    /**
     * Attempt to sign in the user given the information that they provided about themselves.
     *
     * @param view the 'create account' button
     */
    public void signIn(View view) {
        // Get info user provided
        final EditText name = findViewById(R.id.signInName);
        EditText email = findViewById(R.id.signInEmail);
        EditText password = findViewById(R.id.signInPassword);
        EditText passwordConfirm = findViewById(R.id.signInConfirmPassword);
        EditText adminPass = findViewById(R.id.signInAdminPass);

        //Make sure all fields are filled in
        if (name.getText().toString().trim().isEmpty() ||
                email.getText().toString().trim().isEmpty() ||
                password.getText().toString().isEmpty() ||
                passwordConfirm.getText().toString().isEmpty() ||
                department.isEmpty()) {

            Toast.makeText(getApplicationContext(), "Make sure all fields are filled in and try again!", Toast.LENGTH_LONG).show();
            return;
        }

        // Ensure email address is valid according to RFC 5322 standard
        if (!email.getText().toString().trim().matches("^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
            Toast.makeText(getApplicationContext(), "Please enter a valid email address!", Toast.LENGTH_LONG).show();
            return;
        }

        // If the user said that they are an admin, check the passcode they provided
        if (isAdmin) {
            // Get the admin passcode if it hasn't already been obtained
            if (ADMIN_PASS == null) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("AdminPass");
                query.whereEqualTo("objectId", "Xb43zKXoSU");
                try {
                    List<ParseObject> passList = query.find();
                    if (passList.size() != 1)
                        throw new Exception();
                    ADMIN_PASS = passList.get(0).getString("pass");
                } catch (Exception e) {
                    // Most likely, something is wrong with the connection
                    Toast.makeText(getApplicationContext(), "Error: Admin password retrieval failed. Please try again later.", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Check actual passcode against user input
            if (!adminPass.getText().toString().equals(ADMIN_PASS)) {
                Toast.makeText(getApplicationContext(), "Admin passphrase is incorrect! Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Make sure password and confirmed password match
        if (!password.getText().toString().equals(passwordConfirm.getText().toString())) {
            Toast.makeText(getApplicationContext(), "The passwords do not match, please try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // All checks have passed; sign user in
        ParseUser user = new ParseUser();
        String emailString = email.getText().toString().trim();
        user.setUsername(emailString);
        user.setEmail(emailString);
        user.setPassword(password.getText().toString());

        user.put("name", titleCase(name.getText().toString().trim()));
        user.put("department", department);
        user.put("noMeetingsAttended", 0L);
        user.put("meetingsAttended", new ArrayList<ParseObject>());
        user.put("isAdmin", isAdmin);

        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    ParseUser.logOut();
                    alertDisplayer("Account created successfully!", "Please verify your email before logging in.", false);
                } else {
                    try {
                        ParseUser.logOut();
                    } catch (Exception e2) {
                        Toast.makeText(SignInActivity.this, "Something's not right... After reading the next message, please restart the app to prevent malfunction.", Toast.LENGTH_LONG).show();
                    }
                    Toast.makeText(SignInActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * For each word in input, only capitalize the first letter.
     *
     * @param input the string to format
     * @return the formatted string
     */
    private String titleCase(String input) {
        String[] words = input.split("\\s+");
        String newString = "";
        for (String word : words) {
            // Watch for if the user enters multiple spaces consecutively
            if (!word.isEmpty()) {
                String temp = Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
                newString += temp + " ";
            }
        }
        return newString.trim();
    }

    /**
     * For a given alert, display it to the user in a professional manner.
     *
     * @param title   title of alert
     * @param message details of alert/prompt to take action
     * @param error   if true, do not attempt leaving the sign up page, as an error occurred
     */
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
