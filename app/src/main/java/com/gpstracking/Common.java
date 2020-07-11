package com.gpstracking;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class Common {
    public static final String KEY_REQUESTING_LOCATION_UPDATES = "LocationUpdatesEnable";

    public static String getLocationText(Location location){
        return location == null ? "Unknown location" : new StringBuilder()
                .append("{\n")
                .append("\"latitude\":")
                .append(location.getLatitude())
                .append(",\n")
                .append("\"longitude\":")
                .append(location.getLongitude())
                .append(",\n")
                .append("\"date\":")
                .append("\"" +(Calendar.getInstance().getTime()).toString() + "\"")
                .append("\n")
                .append("}")
                .toString();
    }

    public static CharSequence getLocationTitle(MyService myService) {
        return String.format("Location Updated: @1$s", DateFormat.getDateInstance().format(new Date()));
    }

    public static void setRequestingLocationUpdates(Context context, boolean value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, value)
                .apply();
    }

    public static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }
}
