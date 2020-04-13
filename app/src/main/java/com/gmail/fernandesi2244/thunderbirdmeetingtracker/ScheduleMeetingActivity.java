package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ScheduleMeetingActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    public static final String ScheduleMeetingActivityID = "ScheduleMeetingActivity";
    static final int TIME_DIALOG_ID = 1111;

    private int hour, minute, year, month, dayOfMonth;
    private boolean remoteChecked, usingDeviceLocation, usingExistingLocation;
    private String audience, meetingID;
    private final int idGenerated = 314159265;

    private ParseGeoPoint locIfUsingExistingLocation;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location currentLoc;

    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minutes) {
            hour = hourOfDay;
            minute = minutes;
            setDateLabel();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_meeting);

        Intent receivedIntent = getIntent();
        meetingID = receivedIntent.getStringExtra("meetingID");

        initDateAndTime();
        setUpMenu();

        switch (meetingID) {
            case "NONE":
                usingDeviceLocation = true;
                break;
            default:
                usingDeviceLocation = false;
                setUpWithMeetingID();
        }
    }

    /**
     * Set up drop-down menu for choosing who the meeting is intended for.
     */
    private void setUpMenu() {
        Spinner spinner = findViewById(R.id.chooseAudienceMenu);
        spinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.audienceArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        audience = parent.getItemAtPosition(pos).toString();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    private void initDateAndTime() {
        Calendar currentDate = Calendar.getInstance();
        hour = currentDate.get(Calendar.HOUR_OF_DAY);
        minute = currentDate.get(Calendar.MINUTE);
        year = currentDate.get(Calendar.YEAR);
        month = currentDate.get(Calendar.MONTH);
        dayOfMonth = currentDate.get(Calendar.DAY_OF_MONTH);

        setDateLabel();

        CalendarView calendar = findViewById(R.id.calendarView);
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int yr, int mo, int day) {
                year = yr;
                month = mo;
                dayOfMonth = day;

                setDateLabel();
            }
        });
    }

    public void setDateLabel() {
        GregorianCalendar dateToDisplay = new GregorianCalendar(year, month, dayOfMonth, hour, minute);
        DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
        df.setCalendar(dateToDisplay);
        String labelText = df.format(dateToDisplay.getTime());
        TextView dateLabel = findViewById(R.id.dateChosenLabel);
        dateLabel.setText(labelText);
    }

    public void onRemoteCheckboxClicked(View view) {
        remoteChecked = ((CheckBox) view).isChecked();

        Animation fadeIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in);

        TextView chooseLocationLabel = findViewById(R.id.chooseLocationLabel);
        RadioGroup locRadiogroup = findViewById(R.id.locationRadioGroup);

        if (remoteChecked) {
            chooseLocationLabel.clearAnimation();
            chooseLocationLabel.setVisibility(View.GONE);

            locRadiogroup.clearAnimation();
            locRadiogroup.setVisibility(View.GONE);
        } else {
            chooseLocationLabel.setVisibility(View.VISIBLE);
            chooseLocationLabel.startAnimation(fadeIn);

            locRadiogroup.setVisibility(View.VISIBLE);
            locRadiogroup.startAnimation(fadeIn);
        }
    }

    public void changeTime(View view) {
        Dialog dialog = createdDialog(TIME_DIALOG_ID);
        if (dialog != null)
            dialog.show();
    }

    protected Dialog createdDialog(int id) {
        switch (id) {
            case TIME_DIALOG_ID:
                return new TimePickerDialog(this, timePickerListener, hour, minute, false);
        }
        return null;
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.customLocationRadioButton:
                if (checked) {
                    usingDeviceLocation = false;
                    usingExistingLocation = false;
                    //Show custom location items
                }
                break;
            case R.id.currentLocationRadioButton:
                if (checked) {
                    usingDeviceLocation = true;
                    usingExistingLocation = false;
                    //Hide custom location items
                }
                break;
            case idGenerated:
                if (checked) {
                    usingExistingLocation = true;
                    usingDeviceLocation = false;
                    //Hide custom location items
                }
        }
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            Task locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        currentLoc = (Location) task.getResult();
                    }
                    if(meetingID==null||meetingID.equals("NONE"))
                        resumeSubmitRequestWithDeviceLocation();
                    else
                        resumeUpdateRequestWithDeviceLocation();
                }
            });
        } catch (
                SecurityException e) {
            return;
        }
    }

    protected void checkPermissions() {
        if (!hasLocationPermissions()) {
            Intent goToRequestPage = new Intent(this, RequestPermissionsActivity.class);
            goToRequestPage.putExtra("sender", ScheduleMeetingActivityID);
            startActivity(goToRequestPage);
        }
    }

    private boolean hasLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //All good; user may proceed
            return true;
        } else {
            //User needs to be redirected to permissions activity
            return false;
        }
    }

    public void submitMeeting(View view) {
        if (meetingID != null && !meetingID.equals("NONE")) {
            updateMeeting();
            return;
        }
        ParseObject meeting = new ParseObject("Meeting");
        GregorianCalendar dateToLoad = new GregorianCalendar(year, month, dayOfMonth, hour, minute);
        meeting.put("meetingDate", dateToLoad.getTime());
        if (remoteChecked) {
            meeting.put("isRemote", true);
        } else {
            meeting.put("isRemote", false);
            if (usingDeviceLocation) {
                if (!hasLocationPermissions()) {
                    checkPermissions();
                    return;
                }
                getDeviceLocation();
                return;
            } else {
                Toast.makeText(getApplicationContext(), "Custom location is not supported at the moment", Toast.LENGTH_LONG).show();
                return;
            }
        }
        EditText meetingDescription = findViewById(R.id.chooseDescription);
        String meetingDescriptionText = meetingDescription.getText().toString();
        meeting.put("meetingDescription", meetingDescriptionText.isEmpty() ? "None" : meetingDescriptionText);
        meeting.put("audience", audience.isEmpty() ? "General" : audience);

        meeting.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // Success
                    goBackToProfile();
                } else {
                    // Error
                    Toast.makeText(getApplicationContext(), "The meeting was unable to be created at this time. Please try again later!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void resumeSubmitRequestWithDeviceLocation() {
        ParseObject meeting = new ParseObject("Meeting");
        GregorianCalendar dateToLoad = new GregorianCalendar(year, month, dayOfMonth, hour, minute);
        meeting.put("meetingDate", dateToLoad.getTime());
        meeting.put("isRemote", false);

        if (currentLoc != null) {
            ParseGeoPoint location = new ParseGeoPoint(currentLoc.getLatitude(), currentLoc.getLongitude());
            meeting.put("meetingLocation", location);
        } else {
            Toast.makeText(getApplicationContext(), "Could not retrieve phone's location. Please try again!", Toast.LENGTH_LONG).show();
            return;
        }

        EditText meetingDescription = findViewById(R.id.chooseDescription);
        String meetingDescriptionText = meetingDescription.getText().toString();
        meeting.put("meetingDescription", meetingDescriptionText.isEmpty() ? "None" : meetingDescriptionText);

        meeting.put("audience", audience.isEmpty() ? "General" : audience);

        meeting.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // Success
                    goBackToProfile();
                } else {
                    // Error
                    Toast.makeText(getApplicationContext(), "The meeting was unable to be created at this time. Please try again later!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateMeeting() {
        ParseObject meeting;
        ParseQuery<ParseObject> findMeeting = ParseQuery.getQuery("Meeting");
        findMeeting.whereEqualTo("objectId", meetingID);
        try {
            List<ParseObject> parseObjects = findMeeting.find();
            if (parseObjects == null)
                throw new Exception();

            if (parseObjects.size() == 1) {
                meeting = parseObjects.get(0);
                if (meeting == null)
                    throw new Exception();
            } else {
                throw new Exception();
            }
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meeting information.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        } catch (Exception e2) {
            Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meeting information.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        GregorianCalendar dateToLoad = new GregorianCalendar(year, month, dayOfMonth, hour, minute);
        meeting.put("meetingDate", dateToLoad.getTime());
        if (remoteChecked) {
            meeting.put("isRemote", true);
        } else {
            meeting.put("isRemote", false);
            if (usingDeviceLocation) {
                if (!hasLocationPermissions()) {
                    checkPermissions();
                    return;
                }
                getDeviceLocation();
                return;
            } else if (usingExistingLocation) {
                meeting.put("meetingLocation", locIfUsingExistingLocation);
            } else {
                Toast.makeText(getApplicationContext(), "Custom location is not supported at the moment", Toast.LENGTH_LONG).show();
                return;
            }
        }
        EditText meetingDescription = findViewById(R.id.chooseDescription);
        String meetingDescriptionText = meetingDescription.getText().toString();
        meeting.put("meetingDescription", meetingDescriptionText.isEmpty() ? "None" : meetingDescriptionText);
        meeting.put("audience", audience.isEmpty() ? "General" : audience);

        meeting.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // Success
                    Toast.makeText(getApplicationContext(), "The meeting was successfully updated!", Toast.LENGTH_LONG).show();
                    goBackToProfile();
                } else {
                    // Error
                    Toast.makeText(getApplicationContext(), "The meeting was unable to be updated at this time. Please try again later!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void resumeUpdateRequestWithDeviceLocation() {
        ParseObject meeting;
        ParseQuery<ParseObject> findMeeting = ParseQuery.getQuery("Meeting");
        findMeeting.whereEqualTo("objectId", meetingID);
        try {
            List<ParseObject> parseObjects = findMeeting.find();
            if (parseObjects == null)
                throw new Exception();

            if (parseObjects.size() == 1) {
                meeting = parseObjects.get(0);
                if (meeting == null)
                    throw new Exception();
            } else {
                throw new Exception();
            }
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meeting information.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        } catch (Exception e2) {
            Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meeting information.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        GregorianCalendar dateToLoad = new GregorianCalendar(year, month, dayOfMonth, hour, minute);
        meeting.put("meetingDate", dateToLoad.getTime());
        meeting.put("isRemote", false);

        if (currentLoc != null) {
            ParseGeoPoint location = new ParseGeoPoint(currentLoc.getLatitude(), currentLoc.getLongitude());
            meeting.put("meetingLocation", location);
        } else {
            Toast.makeText(getApplicationContext(), "Could not retrieve phone's location. Please try again!", Toast.LENGTH_LONG).show();
            return;
        }

        EditText meetingDescription = findViewById(R.id.chooseDescription);
        String meetingDescriptionText = meetingDescription.getText().toString();
        meeting.put("meetingDescription", meetingDescriptionText.isEmpty() ? "None" : meetingDescriptionText);

        meeting.put("audience", audience.isEmpty() ? "General" : audience);

        meeting.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // Success
                    Toast.makeText(getApplicationContext(), "The meeting was successfully updated!", Toast.LENGTH_LONG).show();
                    goBackToProfile();
                } else {
                    // Error
                    Toast.makeText(getApplicationContext(), "The meeting was unable to be updated at this time. Please try again later!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void goBackToProfile() {
        Intent goBackToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goBackToProfile);
    }

    private void setUpWithMeetingID() {
        //Get meeting
        if (meetingID == null) {
            Toast.makeText(getApplicationContext(), "Something went wrong... Please try again later!", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        ParseObject meeting;
        ParseQuery<ParseObject> findMeeting = ParseQuery.getQuery("Meeting");
        findMeeting.whereEqualTo("objectId", meetingID);
        try {
            List<ParseObject> parseObjects = findMeeting.find();
            if (parseObjects == null)
                throw new Exception();

            if (parseObjects.size() == 1) {
                meeting = parseObjects.get(0);
                if (meeting == null)
                    throw new Exception();
            } else {
                throw new Exception();
            }
        } catch (ParseException e) {
            Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meeting information.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        } catch (Exception e2) {
            Toast.makeText(getApplicationContext(), "Something went wrong when retrieving the meeting information.", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }


        //Set up visual components
        EditText meetingDescription = findViewById(R.id.chooseDescription);
        meetingDescription.setText(meeting.getString("meetingDescription"));

        Spinner spinner = findViewById(R.id.chooseAudienceMenu);
        spinner.setSelection(((ArrayAdapter) spinner.getAdapter()).getPosition(meeting.getString("audience")));
        audience = meeting.getString("audience");

        CalendarView calendar = findViewById(R.id.calendarView);
        Date meetingDate = meeting.getDate("meetingDate");
        calendar.setDate(meetingDate.getTime(), true, true);
        Calendar cal = Calendar.getInstance();
        cal.setTime(meetingDate);
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        setDateLabel();

        boolean meetingIsRemote = meeting.getBoolean("isRemote");
        CheckBox remoteCheckBox = findViewById(R.id.remoteCheckBox);
        remoteCheckBox.setChecked(meetingIsRemote);
        onRemoteCheckboxClicked(remoteCheckBox);
        RadioGroup locationRadioGroup = findViewById(R.id.locationRadioGroup);
        if (!meetingIsRemote) {
            locIfUsingExistingLocation = meeting.getParseGeoPoint("meetingLocation");
            //Add radio button for using existing location and choose it by default (MAKE APPROPRIATE CHANGES TO SUBMIT() METHOD)
            RadioButton useExistingLocationRB = new RadioButton(this);
            useExistingLocationRB.setText(R.string.useExistingLocationMessage);
            //idGenerated = View.generateViewId();
            useExistingLocationRB.setId(idGenerated);
            locationRadioGroup.addView(useExistingLocationRB);
            locationRadioGroup.check(idGenerated);
            onRadioButtonClicked(useExistingLocationRB);
            locationRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    onRadioButtonClicked(findViewById(checkedId));
                }
            });
        } else {
            usingDeviceLocation = true;
        }
    }
}