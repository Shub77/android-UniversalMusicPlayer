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
package com.example.android.uamp.ui.MediaBrowserClient;

import android.Manifest;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.android.uamp.R;
import com.example.android.uamp.playback.PlaybackManager;
import com.example.android.uamp.ui.BaseActivity;
import com.example.android.uamp.ui.FullScreenRecyclerPlayQueueActivity;
import com.example.android.uamp.ui.MainLauncherActivity;
import com.example.android.uamp.utils.LogHelper;

/**
 * Browsing activity activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 *
 * The activity will always be displaying a fragment (root manu, artists, songs etc)
 * The Fragment calls back to this activity via the MediaChooserFragmentListener when something is chosen
 * - for example "Add track x to playqueue"
 * - "Add artist y to playqueue"
 * - "Show list of artists"
 * - "Show all tracks on album x"
 */
public class MediaBrowserClientActivity extends BaseActivity
        implements MediaBrowserClientFragmentListener {

    private static final String TAG = LogHelper.makeLogTag("MedBroClActivity");
    // saves the info from a voice search (e.g. play frank zappa) to be used when the media session is conneced
    private static final String SAVED_MEDIA_ID="com.example.android.uamp.SAVED_MEDIA_ID";
    private static final String SAVED_TITLE="com.example.android.uamp.SAVED_TITLE";
    private static final String SAVED_ROTATEFLAG="com.example.android.uamp.SAVED_ROTATE_FLAG";
    private static final String FRAGMENT_TAG = "uamp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.example.android.uamp.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link FullScreenRecyclerPlayQueueActivity}, speeding up the screen rendering
     * while the {@link MediaControllerCompat} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
        "com.example.android.uamp.CURRENT_MEDIA_DESCRIPTION";

    private Bundle mVoiceSearchParams;
    private String SavedState_MediaId; // the current media Id being displayed. Used to save state
    private String SavedState_Title; // the current title Id being displayed. Used to save state
    private Boolean SavedState_Rotated;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.i(TAG, "Activity onCreate");

        LogHelper.i(TAG, "check permissiions");
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission. So start the mainlauncher activity
            startActivity(new Intent(MediaBrowserClientActivity.this, MainLauncherActivity.class));
            finish();
        }

        setContentView(R.layout.activity_music_chooser);

        initializeToolbar();
        initializeFromParams(savedInstanceState, getIntent());

        // Only check if full screen is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LogHelper.i(TAG, "onSaveInstanceState: mediaId=", SavedState_MediaId);
        if (SavedState_MediaId != null) {
            outState.putString(SAVED_MEDIA_ID, SavedState_MediaId);
            outState.putString(SAVED_TITLE, SavedState_Title);
            outState.putBoolean(SAVED_ROTATEFLAG, true);
        }
        super.onSaveInstanceState(outState);
    }

    // Callbacks from the MediaChooserFragment(s)

    /**
     * Called by the browser fragment when user clicks on add for an item (track, artist, album)
     * @param mediaId A string identifying the item to add. Could be just (e.g. "_ALBUM_2" or
     *                "__TRACK_189")
    */
    @Override
    public void onAddBrowsableItemToQueueByMediaIdFromRecyclerView(String mediaId) {
        LogHelper.i(TAG, "onAddBrowsableItemToQueueByMediaIdFromRecyclerView. mediaId = ", mediaId);
        MediaControllerCompat.TransportControls controls = MediaControllerCompat.getMediaController(this).getTransportControls();
        Bundle bundle = new Bundle();
        if (mediaId.startsWith("__ALBUM__")) {
            Long albumId = Long.parseLong(mediaId.substring(9));
            bundle.putLong(PlaybackManager.CUSTOM_EXTRA_MEDIA_ID, albumId);
            controls.sendCustomAction(PlaybackManager.CUSTOM_ACTION_ADD_ALBUM_TO_QUEUE, bundle);
        } else if (mediaId.startsWith("__ARTIST__")) {
                Long artistId = Long.parseLong(mediaId.substring(10));
                bundle.putLong(PlaybackManager.CUSTOM_EXTRA_MEDIA_ID, artistId);
                controls.sendCustomAction(PlaybackManager.CUSTOM_ACTION_ADD_ARTIST_TO_QUEUE,bundle);
        } else { // should use __TRACK__
            Long trackId = Long.parseLong(mediaId);
            bundle.putLong(PlaybackManager.CUSTOM_EXTRA_TRACK_ID, trackId);
            controls.sendCustomAction(PlaybackManager.CUSTOM_ACTION_ADD_TRACK_TO_QUEUE,bundle);
        }
    }


    /**
     * Called by Fragment when user clicks on background of a browsable item
     * So we should display the children
     * @param mediaId A string identifying the item to browse. Could be just "__ALBUMS__" or
     *                may include an id (e.g. "_ALBUM_2")
     * @param title   A user friendly string describing the item. Will be the title of the new list
     *                E.g. ""Albums" or "Dark Side of the moon"
     */
    @Override
    public void onBrowseItemFromRecyclerView(String mediaId, String title) {
        LogHelper.i(TAG, "onBrowseItemFromRecyclerView:mediaId = ", mediaId);
        browseItem(mediaId, title, true);
    }

    private void browseItem(String mediaId, String title, boolean addToBackStack) {
        LogHelper.i(TAG, "browseItem:mediaId = ", mediaId);
        MediaBrowserClientFragment fragment = new MediaBrowserClientFragment();
        fragment.setBrowseParameters(mediaId, title);
        SavedState_MediaId = mediaId;
        SavedState_Title = title;
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
        transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
        // If this is not the top level media (root), we add it to the fragment back stack,
        // so that actionbar toggle and Back will work appropriately:
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    @Override
    public void setTitleString (String title) {
        setTitle(title);
    }
    ////////////////////////////////////////////////
    //// End of callbacks from MediaChooserFragments
    ////////////////////////////////////////////////

    @Override
    protected void onMediaControllerConnected() {
        if (mVoiceSearchParams != null) {
            // If there is a bootstrap parameter to start from a search query, we
            // send it to the media session and set it to null, so it won't play again
            // when the activity is stopped/started or recreated:
            String query = mVoiceSearchParams.getString(SearchManager.QUERY);
            MediaControllerCompat.getMediaController(this).getTransportControls()
                    .playFromSearch(query, mVoiceSearchParams);
            mVoiceSearchParams = null;
        }
        //getBrowseFragment().onConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        LogHelper.i(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.browser_toolbar, menu);

        return true;
    }

    // handle user interaction with the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
/*
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
*/
        Intent fullScreenIntent;
        switch (item.getItemId()) {
            case R.id.action_show_now_playing:
                fullScreenIntent = new Intent(this, FullScreenRecyclerPlayQueueActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(fullScreenIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        LogHelper.d(TAG, "onNewIntent, intent=" + intent);
        initializeFromParams(null, intent);
        startFullScreenActivityIfNeeded(intent);
    }

    private void startFullScreenActivityIfNeeded(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_START_FULLSCREEN, false)) {
            Intent fullScreenIntent = new Intent(this, FullScreenRecyclerPlayQueueActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
                            intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));
            startActivity(fullScreenIntent);
        }
    }

    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        LogHelper.i(TAG,"initializeFromParams" );

        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        if (intent.getAction() != null
                && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            mVoiceSearchParams = intent.getExtras();
            LogHelper.d(TAG, "Starting from voice search query=",
                    mVoiceSearchParams.getString(SearchManager.QUERY));
        } else {
            if (false && savedInstanceState != null) {
                // If there is a saved media ID, use it
                SavedState_MediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
                SavedState_Title = savedInstanceState.getString(SAVED_TITLE);
                SavedState_Rotated = savedInstanceState.getBoolean(SAVED_ROTATEFLAG);
                LogHelper.i(TAG, "savedInstanceState != null: mediaId=", SavedState_MediaId);
            } else {
                SavedState_MediaId = "__ROOT__";
                SavedState_Title = "Browse Music";
                SavedState_Rotated = false;
            }
        }
    }

    /**
     * Overridden from the base class (BaseActivity)
     * Will be called when the media browser is connected by the base activity
     * So we should wait for this before trying to do any browsing
     */
    @Override
    protected  void onBrowserConnected() {
        LogHelper.i(TAG, "onBrowserConnected");
        if (!SavedState_Rotated) {
            LogHelper.i(TAG, "SavedState_MediaId=", SavedState_MediaId);
            // Standard start... show the main menu
            //browseItem(SavedState_MediaId, SavedState_Title, false);
            MediaBrowserClientFragment fragment = new MediaBrowserClientFragment();
            fragment.setBrowseParameters(SavedState_MediaId, SavedState_Title);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                    R.animator.slide_in_from_left, R.animator.slide_out_to_right);
            transaction.add(R.id.container, fragment, FRAGMENT_TAG);
            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (false) { // addToBackStack) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

}
