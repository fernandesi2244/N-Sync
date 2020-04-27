package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import android.Manifest;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Close the app if it cannot be used successfully
        if(!isServicesOK()) {
            finish();
        }

        logoAnimation();
        editTextAnimations();
        buttonAnimations();

        // Save the current Installation to Back4App
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

    public void logoAnimation() {
        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.spin_and_zoom);
        ImageView logo = (ImageView) (findViewById(R.id.wagnerLogo));
        logo.startAnimation(anim);
    }

    public void logIn(View view) {
        EditText email = findViewById(R.id.enterEmailLogin);
        EditText password = findViewById(R.id.enterPasswordLogin);

        ParseUser.logInInBackground(email.getText().toString(), password.getText().toString(), new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (parseUser != null) {
                    Toast.makeText(getApplicationContext(), "Successful Login. Welcome back " + parseUser.getString("name") + "!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                    startActivity(intent);
                } else {
                    ParseUser.logOut();
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void signUp(View view) {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }

    /**
     * Check google play services version
     *
     * @return whether google play services version is up to date
     */
    public boolean isServicesOK(){

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occurred but we can resolve it
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "Because your phone does not have the ability to connect to Google Play Services, you cannot use this app. Sorry for the inconvenience.", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void editTextAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);
        EditText username = findViewById(R.id.enterEmailLogin);
        EditText password = findViewById(R.id.enterPasswordLogin);
        username.startAnimation(fadeIn);
        password.startAnimation(fadeIn);
    }

    private void buttonAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in_two_seconds);
        Button logInButton = findViewById(R.id.loginButton);
        Button signUpButton = findViewById(R.id.signUpButton);
        logInButton.startAnimation(fadeIn);
        signUpButton.startAnimation(fadeIn);
    }

}
