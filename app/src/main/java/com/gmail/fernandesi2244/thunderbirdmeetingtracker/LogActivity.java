package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class LogActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setUpMenu();
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

    /**
     * Set up drop-down menu for choosing which types of log entries to display.
     */
    private void setUpMenu() {
        Spinner spinner = findViewById(R.id.chooseSeverityMenu);
        spinner.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.logArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        String firstWordAllCaps = parent.getItemAtPosition(pos).toString().split("\\s+")[0].toUpperCase();
        setUpList(firstWordAllCaps);
    }

    public void onNothingSelected(AdapterView<?> parent) {
        setUpList("HIGH");
    }

    private void setUpList(String severity) {
        ParseQuery<ParseObject> getLogs = ParseQuery.getQuery("Log");
        if(!severity.equals("ALL"))
            getLogs.whereEqualTo("severity", severity);
        getLogs.orderByDescending("createdAt");
        List<ParseObject> logs;
        try {
            logs = getLogs.find();
            if(logs==null)
                throw new Exception();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not retrieve logs at this moment. Please try again later!", Toast.LENGTH_LONG).show();
            goBackToProfile();
            return;
        }

        ListView listview = findViewById(R.id.logListView);
        final LogArrayAdapter adapter = new LogArrayAdapter(this,
                android.R.layout.simple_list_item_1, logs);
        listview.setAdapter(adapter);

        if (logs.size() == 0) {
            Toast.makeText(getApplicationContext(), "There are currently no logs in memory.", Toast.LENGTH_LONG).show();
        }
    }

    private void goBackToProfile() {
        Intent goToProfile = new Intent(this, ProfileActivity.class);
        startActivity(goToProfile);
    }
}
