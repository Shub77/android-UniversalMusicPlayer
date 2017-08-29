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

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import com.example.android.uamp.ui.tv.TvPlaybackActivity;
import com.example.android.uamp.utils.LogHelper;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.i(TAG, "onCreate");
        Intent newIntent;
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            LogHelper.i(TAG, "Running on a TV Device");
            newIntent = new Intent(this, TvPlaybackActivity.class);
        } else {
            LogHelper.i(TAG, "Running on a non-TV Device");
            newIntent = new Intent(this, FullScreenPlayQueueActivity.class);
        }
        startActivity(newIntent);
        finish();
    }
}
