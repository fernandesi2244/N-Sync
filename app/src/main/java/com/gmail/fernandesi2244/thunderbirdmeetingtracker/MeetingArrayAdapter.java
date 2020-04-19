package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.parse.ParseObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MeetingArrayAdapter extends ArrayAdapter<ParseObject> {
    private Context context;
    private List<ParseObject> values;

    public MeetingArrayAdapter(Context context, int simple_list_item_1, List<ParseObject> values) {
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

        ParseObject currentMeeting = values.get(position);
        String meetingDescription = currentMeeting.getString("meetingDescription");
        Date nextMeetingDate = currentMeeting.getDate("meetingDate");
        DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
        String date = df.format(nextMeetingDate);

        textView.setText("Description: " + meetingDescription + "; Time: " + date + "; ID: " + currentMeeting.getObjectId());

        return rowView;
    }
}