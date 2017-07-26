package com.example.android.uamp.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

/**
 * Created by asbridged on 09/08/2016.
 */
public class SettingsFragment extends PreferenceFragment {
    private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
