package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.text.HtmlCompat;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserArrayAdapter extends ArrayAdapter<ParseUser> {
    private Context context;
    private List<ParseUser> values;
    private List<ParseUser> lateUsers = new ArrayList<>();

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
        boolean flag = false;
        //See if the user is contained within the lateUsers list
        for (ParseUser latePerson : lateUsers)
            if (latePerson.getObjectId().equals(user.getObjectId()))
                flag = true;

        if (flag)
            textView.setBackgroundColor(Color.parseColor("#FFCCCB"));

        String name = user.getString("name");
        String department = user.getString("department");
        String html = "<b>Name:</b> " + name + "<br/><b>Department:</b> " + department + "<br/><b>ID:</b> " + user.getObjectId();
        if(flag)
            html+="<br/><b>WAS LATE TO MEETING</b>";

        Spanned durationSpanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        textView.setText(durationSpanned);
        return rowView;
    }

    public void addLate(List<ParseUser> late) {
        if (late != null)
            lateUsers = late;
    }
}