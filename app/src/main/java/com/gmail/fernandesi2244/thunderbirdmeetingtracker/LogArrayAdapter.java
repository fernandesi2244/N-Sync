package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.parse.ParseObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LogArrayAdapter extends ArrayAdapter<ParseObject> {
    private Context context;
    private List<ParseObject> values;

    public LogArrayAdapter(Context context, int simple_list_item_1, List<ParseObject> values) {
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

        ParseObject logRow = values.get(position);
        String message = logRow.getString("logMessage");
        String severity = logRow.getString("severity");
        Date createdAt = logRow.getCreatedAt();
        DateFormat df = new SimpleDateFormat("M/dd/yy @ h:mm a");
        String date = df.format(createdAt);

        switch(severity) {
            case "LOW":
                textView.setBackgroundColor(Color.parseColor("#FFFFCC"));
                //textView.setBackgroundResource(R.drawable.textview_border_yellow);
                break;
            case "MEDIUM":
                textView.setBackgroundColor(Color.parseColor("#FFCC99"));
                //textView.setBackgroundResource(R.drawable.textview_border_orange);
                break;
            case "HIGH":
                textView.setBackgroundColor(Color.parseColor("#FF9966"));
                //textView.setBackgroundResource(R.drawable.textview_border_red);
        }

        String html = "<b>Message:</b> " + message + "<br/><b>Time:</b> " + date;
        Spanned durationSpanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        textView.setText(durationSpanned);

        return rowView;
    }
}