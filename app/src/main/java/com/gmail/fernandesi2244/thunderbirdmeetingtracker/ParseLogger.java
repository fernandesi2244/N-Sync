package com.gmail.fernandesi2244.thunderbirdmeetingtracker;

import com.parse.ParseObject;

class ParseLogger {

    public static void log(String message, String severity) {
        ParseObject newLog = new ParseObject("Log");
        newLog.put("logMessage", message);
        newLog.put("severity", severity);
        newLog.saveEventually();
    }
}
