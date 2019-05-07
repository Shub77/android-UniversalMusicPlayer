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
    public static final String PREF_HISTORY_SIZE = "historysize";
    public static final String PREF_MINDURATIONINSECONDS = "minsonglength";
    public static final String PREF_TIME_TO_GO_TO_SLEEP = "timetogotosleep";

    public static int getPlayQueueSize(Context context) {

        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String playQueueSize = defaultSharedPref.getString(PREF_PLAYQUEUE_SIZE, "10");

        return Integer.parseInt(playQueueSize);
    }

    public static int getHistorySize(Context context) {

        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String historySize = defaultSharedPref.getString(PREF_HISTORY_SIZE, "8");

        return Integer.parseInt(historySize);
    }

    public static int getMinDurationInSeconds(Context context) {

        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String mindurSize = defaultSharedPref.getString(PREF_MINDURATIONINSECONDS, "30");

        return Integer.parseInt(mindurSize);
    }

    public static long getTimeToGoToSleep(Context context) {
        SharedPreferences defaultSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        long timeToSleep = defaultSharedPref.getLong(PREF_TIME_TO_GO_TO_SLEEP, (long)0);
        return timeToSleep;
    }

    public static void setTimeToGoToSleep(Context context, long t) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(PREF_TIME_TO_GO_TO_SLEEP, t);
        editor.commit();
    }



}
