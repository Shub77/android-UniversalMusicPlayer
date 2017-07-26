package com.example.android.uamp.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * Created by David on 19/12/2015.
 */
public class Settings {

    private static final String TAG = "Settings";

    public static final String PREF_PLAYQUEUE_SIZE = "playqueuesize";
    public static final String PREF_MINDURATIONINSECONDS = "minsonglength";

    public static int getPlayQueueSize(Context context) {

        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String playQueueSize = defaultSharedPref.getString(PREF_PLAYQUEUE_SIZE, "10");

        return Integer.parseInt(playQueueSize);
    }

    public static int getMinDurationInSeconds(Context context) {

        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String playQueueSize = defaultSharedPref.getString(PREF_MINDURATIONINSECONDS, "30");

        return Integer.parseInt(playQueueSize);
    }

}
