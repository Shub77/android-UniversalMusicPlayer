/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.uamp.ui;

import android.Manifest;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.uamp.ui.tv.TvPlaybackActivity;
import com.example.android.uamp.utils.LogHelper;

import java.util.Random;


/**
 * The main activity launched when the app starts (in example was call "Now Playing Activity"
 * This just detects whether on TV or Mobile and launches the appropriate activity
 * In my case I am only testing/running on mobile, so really it just starts FullScreenPlayQueue Activity.
 * https://developer.android.com/training/tv/playback/now-playing.html
 *
 * This activity determines which activity to launch based on the current UI mode.
 */
public class MainLauncherActivity extends Activity {

    private static final String TAG = LogHelper.makeLogTag(MainLauncherActivity.class);

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    /**
     * The time that the splash screen will be on the screen in milliseconds.
     */
    private static int                 TIMEOUT_IN_MS       = 5000;

    /** The time when this {@link Activity} was created. */
    private long                startTimeMillis     = 0;

    /** A random number generator for the background colors. */
    private static final Random random              = new Random();

    /**
     * The TextView which is used to inform the user whether the permissions are
     * granted.
     */
    private TextView            textView            = null;
    private static final int    textViewID          = View.generateViewId();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.i(TAG, "onCreate");

        /** Create the layout that will hold the TextView. */
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        /** Add a TextView and set the initial text. */
        textView = new TextView(this);
        textView.setTextSize(50);
        textView.setId(textViewID);
        textView.setText("Waiting for permissions...");
        mainLayout.addView(textView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        /** Set the background color. */
        int off = 128;
        int rest = 256 - off;
        int color = Color.argb(255, off + random.nextInt(rest), off + random.nextInt(rest), off + random.nextInt(rest));
        mainLayout.setBackgroundColor(color);

        /** Set the mainLayout as the content view */
        setContentView(mainLayout);

        /**
         * Save the start time of this Activity, which will be used to determine
         * when the splash screen should timeout.
         */
        startTimeMillis = System.currentTimeMillis();

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            startNextActivity();
            Toast.makeText(this, "permission OK", Toast.LENGTH_LONG);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    Toast.makeText(this, "permission was granted, yay!", Toast.LENGTH_LONG);
                    startNextActivity();
                } else {
                    // permission denied, boo!"
                    Toast.makeText(this, "permission denied, boo!", Toast.LENGTH_LONG);
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }

    }

    private void startNextActivity() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                textView.setText("Permissions granted...");
            }
        });
        long delayMillis = TIMEOUT_IN_MS - (System.currentTimeMillis() - startTimeMillis);
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(MainLauncherActivity.this, getNextActivityClass()));
                finish();
            }
        }, delayMillis);
    }

    private Class getNextActivityClass() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            LogHelper.i(TAG, "Running on a TV Device");
            return TvPlaybackActivity.class;
        } else {
            LogHelper.i(TAG, "Running on a non-TV Device");
            return FullScreenPlayQueueActivity.class;
        }
    }



}
