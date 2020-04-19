package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class UserArrayAdapter extends ArrayAdapter<ParseUser> {
    private Context context;
    private List<ParseUser> values;

    public UserArrayAdapter(Context context, int simple_list_item_1, List<ParseUser> values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.list_layout_row, parent, false);
        TextView textView = rowView.findViewById(R.id.rowTextView);

        ParseUser user = values.get(position);
        String name = user.getString("name");
        String department = user.getString("department");

        textView.setText("Name: " + name + "; Department: " + department + "; ID: " + user.getObjectId());

        return rowView;
    }
}