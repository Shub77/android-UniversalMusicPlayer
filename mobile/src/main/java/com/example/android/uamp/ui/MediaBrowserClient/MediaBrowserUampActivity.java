package com.example.android.uamp.ui.MediaBrowserClient;


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

        import android.app.FragmentTransaction;
        import android.app.SearchManager;
        import android.content.Intent;
        import android.os.Bundle;
        import android.provider.MediaStore;
        import android.support.v4.media.MediaBrowserCompat;
        import android.support.v4.media.session.MediaControllerCompat;
        import android.text.TextUtils;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;

        import com.example.android.uamp.R;
        import com.example.android.uamp.playback.PlaybackManager;
        import com.example.android.uamp.ui.BaseActivity;
        import com.example.android.uamp.ui.FullScreenRecyclerPlayQueueActivity;
        import com.example.android.uamp.utils.LogHelper;

/**
 * Main activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MediaBrowserUampActivity extends BaseActivity
        implements MediaBrowserUampRecyclerFragment.MediaFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MediaBrowserUampActivity.class);
    private static final String SAVED_MEDIA_ID="com.example.android.uamp.SAVED_MEDIA_ID";
    private static final String SAVED_TITLE="com.example.android.uamp.SAVED_TITLE";
    private static final String FRAGMENT_TAG = "uamp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.example.android.uamp.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the player activity, speeding up the screen rendering
     * while the {@link android.support.v4.media.session.MediaControllerCompat} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
            "com.example.android.uamp.CURRENT_MEDIA_DESCRIPTION";

    private Bundle mVoiceSearchParams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_mediabrowseruampactivity);

        initializeToolbar();
        initializeFromParams(savedInstanceState, getIntent());

        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        String mediaId = getMediaId();
        String title = getBrowseTitle();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        if (title != null)
        {
            outState.putString(SAVED_TITLE, title);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMediaItemSelectedForBrowse(MediaBrowserCompat.MediaItem item) {
        LogHelper.d(TAG, "onMediaItemSelectedForBrowse, mediaId=" + item.getMediaId());
        if (item.isBrowsable()) {
            navigateToBrowser(item.getMediaId(), item.getDescription().getTitle().toString());
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is not browsable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    //@Override
    public void onMediaItemSelectedForPlay(MediaBrowserCompat.MediaItem item) {
        LogHelper.d(TAG, "onMediaItemSelectedForPlay, mediaId=" + item.getMediaId());
        if (item.isPlayable()) {
            // original code...MediaControllerCompat.getMediaController(this).getTransportControls().playFromMediaId(item.getMediaId(), null);
            String mediaId = item.getMediaId();
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
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is not playable: ",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        LogHelper.i(TAG, "Setting toolbar title to ", title);
        if (title == null) {
            title = getString(R.string.app_name);
        }
        setTitle(title);
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
        String mediaId = null;
        String title =  "Browse Music";
        // check if we were started from a "Play XYZ" voice search. If so, we save the extras
        // (which contain the query details) in a parameter, so we can reuse it later, when the
        // MediaSession is connected.
        if (intent.getAction() != null
                && intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)) {
            mVoiceSearchParams = intent.getExtras();
            LogHelper.d(TAG, "Starting from voice search query=",
                    mVoiceSearchParams.getString(SearchManager.QUERY));
        } else {
            if (savedInstanceState != null) {
                // If there is a saved media ID, use it
                mediaId = savedInstanceState.getString(SAVED_MEDIA_ID);
                title = savedInstanceState.getString(SAVED_TITLE);
            }
        }
        navigateToBrowser(mediaId, title);
    }

    private void navigateToBrowser(String mediaId, CharSequence title) {
        LogHelper.d(TAG, "navigateToBrowser, mediaId=" + mediaId);
        MediaBrowserUampRecyclerFragment fragment = getBrowseFragment();

        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaBrowserUampRecyclerFragment();
            fragment.setMediaIdAndTitle(mediaId, title.toString());
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                    R.animator.slide_in_from_left, R.animator.slide_out_to_right);
            transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
            // If this is not the top level media (root), we add it to the fragment back stack,
            // so that actionbar toggle and Back will work appropriately:
            if (mediaId != null) {
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }
    }

    public String getMediaId() {
        MediaBrowserUampRecyclerFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    public String getBrowseTitle() {
        MediaBrowserUampRecyclerFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getTitle();
    }

    private MediaBrowserUampRecyclerFragment getBrowseFragment() {
        return (MediaBrowserUampRecyclerFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

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
        getBrowseFragment().onConnected();
    }
}

