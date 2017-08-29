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
import com.example.android.uamp.constants.Constants;
import com.example.android.uamp.model.MediaChooserFragmentListener;
import com.example.android.uamp.playback.PlaybackManager;
import com.example.android.uamp.ui.MediaChooserFragments.MediaChooserGroupsFragment;
import com.example.android.uamp.ui.MediaChooserFragments.MediaChooserTracksFragment;
import com.example.android.uamp.ui.MediaChooserFragments.MediaChooserOptionsFragment;
import com.example.android.uamp.utils.LogHelper;

/**
 * Browsing activity activity for the music player.
 * This class hold the MediaBrowser and the MediaController instances. It will create a MediaBrowser
 * when it is created and connect/disconnect on start/stop. Thus, a MediaBrowser will be always
 * connected while this activity is running.
 */
public class MusicChooserActivity extends BaseActivity
        implements MediaChooserFragmentListener {

    private static final String TAG = LogHelper.makeLogTag(MusicChooserActivity.class);
    private static final String SAVED_MEDIA_ID="com.example.android.uamp.MEDIA_ID";
    private static final String FRAGMENT_TAG = "uamp_list_container";

    public static final String EXTRA_START_FULLSCREEN =
            "com.example.android.uamp.EXTRA_START_FULLSCREEN";

    /**
     * Optionally used with {@link #EXTRA_START_FULLSCREEN} to carry a MediaDescription to
     * the {@link FullScreenPlayerActivity}, speeding up the screen rendering
     * while the {@link MediaControllerCompat} is connecting.
     */
    public static final String EXTRA_CURRENT_MEDIA_DESCRIPTION =
        "com.example.android.uamp.CURRENT_MEDIA_DESCRIPTION";

    private Bundle mVoiceSearchParams;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogHelper.d(TAG, "Activity onCreate");

        setContentView(R.layout.activity_player);

        initializeToolbar();
        initializeFromParams(savedInstanceState, getIntent());

        // Only check if a full screen player is needed on the first time:
        if (savedInstanceState == null) {
            startFullScreenActivityIfNeeded(getIntent());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        /*
        String mediaId = getMediaId();
        if (mediaId != null) {
            outState.putString(SAVED_MEDIA_ID, mediaId);
        }
        */
        super.onSaveInstanceState(outState);
    }

    /**
     * A callback from the MediaBrowserFragment
     * We have clicked on add button for an item
     * Add all the music in the item's category to the queue
     * - Could be just one song, or a category (e.g. all songs for an artist)
     * //@param item

    @Override
    public void onAddMediaToQueue(MediaBrowserCompat.MediaItem item) {
        MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
        Bundle bundle = new Bundle();
        bundle.putString(PlaybackManager.CUSTOM_EXTRA_MEDIA_ID, item.getMediaId());
        controls.sendCustomAction(PlaybackManager.CUSTOM_ACTION_ADD_MUSIC_TO_QUEUE,bundle);
    }
     */

    @Override
    public void onAddAlbumToQueue(long albumId) {
        MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
        Bundle bundle = new Bundle();
        bundle.putLong(PlaybackManager.CUSTOM_EXTRA_MEDIA_ID, albumId);
        controls.sendCustomAction(PlaybackManager.CUSTOM_ACTION_ADD_ALBUM_TO_QUEUE,bundle);
    }

    @Override
    public void onAddArtistToQueue(long artistID) {
        MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
        Bundle bundle = new Bundle();
        bundle.putLong(PlaybackManager.CUSTOM_EXTRA_MEDIA_ID, artistID);
        controls.sendCustomAction(PlaybackManager.CUSTOM_ACTION_ADD_ARTIST_TO_QUEUE,bundle);
    }

    @Override
    public void onAddTrackToQueue(long trackId) {
        MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
        Bundle bundle = new Bundle();
        bundle.putLong(PlaybackManager.CUSTOM_EXTRA_TRACK_ID, trackId);
        controls.sendCustomAction(PlaybackManager.CUSTOM_ACTION_ADD_TRACK_TO_QUEUE,bundle);
    }

    @Override
    public void onBrowseGroup(String searchType, long albumId) {
        if (Constants.SEARCH_TYPE_ALBUM.equals(searchType)) {
            browseAlbum(albumId);
        } else {
            browseArtist(albumId);
        }
    }

    /**
     * A callback from the MediaBrowserFragment
     * We have clicked on the background of a list item.
     * So we browse down (if applicable)
     * Unlike in the example, we do NOT start playing any track
     * @param item
     */
    @Override
    public void onBrowseMediaItemSelected(MediaBrowserCompat.MediaItem item) {
        LogHelper.i(TAG, "onBrowseMediaItemSelected, mediaId=" + item.getMediaId());
        /*
        if (item.isPlayable()) {
            // My code using new custom action
            MediaControllerCompat.TransportControls controls = getSupportMediaController().getTransportControls();
            Bundle bundle = new Bundle();
            bundle.putString(CUSTOM_EXTRA_MEDIA_ID, item.getMediaId());
            controls.sendCustomAction("newq",bundle);
            This was the original code.
            play from media ID sets the queue and ALSO starts playing.
            which we dont really want.
            we just want to set the queue
            controls.playFromMediaId(item.getMediaId(), null);
        } else */
        if (item.isBrowsable()) {
            navigateToBrowser(item.getMediaId());
        } else {
            LogHelper.w(TAG, "Ignoring MediaItem that is not browsable",
                    "mediaId=", item.getMediaId());
        }
    }

    @Override
    public void setToolbarTitle(CharSequence title) {
        LogHelper.d(TAG, "Setting toolbar AlbumTitle to ", title);
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
            Intent fullScreenIntent = new Intent(this, FullScreenPlayQueueActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION,
                    intent.getParcelableExtra(EXTRA_CURRENT_MEDIA_DESCRIPTION));
            startActivity(fullScreenIntent);
        }
    }

    protected void initializeFromParams(Bundle savedInstanceState, Intent intent) {
        String mediaId = null;
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
            }
        }
        navigateToBrowser(mediaId);
    }

    public void onChooseTrack() {
        LogHelper.i(TAG, "onChooseTrack");
        MediaChooserTracksFragment fragment = null;
        fragment = new MediaChooserTracksFragment();
        //fragment.setMediaId(mediaId);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
        transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
        // If this is not the top level media (root), we add it to the fragment back stack,
        // so that actionbar toggle and Back will work appropriately:
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Show 'tracks list' fragment.
     * Filtered by album with specified id
     * @param albumId
     */
    public void browseAlbum(long albumId) {
        LogHelper.i(TAG, "browseAlbum with id=", albumId);
        MediaChooserTracksFragment fragment = null;
        fragment = new MediaChooserTracksFragment();
        fragment.setSearchParams(Constants.SEARCH_TYPE_ALBUM, Long.toString(albumId));
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
        transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
        // If this is not the top level media (root), we add it to the fragment back stack,
        // so that actionbar toggle and Back will work appropriately:
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Show 'tracks list' fragment.
     * Filtered by artist with specified id
     * @param artistId
     */
    public void browseArtist(long artistId) {
        LogHelper.i(TAG, "browseArtist with id=", artistId);
        MediaChooserTracksFragment fragment = null;
        fragment = new MediaChooserTracksFragment();
        fragment.setSearchParams(Constants.SEARCH_TYPE_ARTIST, Long.toString(artistId));
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
        transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
        // If this is not the top level media (root), we add it to the fragment back stack,
        // so that actionbar toggle and Back will work appropriately:
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Show 'groups' list fragment, where groups are albums
     */
    public void onChooseAlbum() {
        LogHelper.i(TAG, "onChooseAlbum");
        MediaChooserGroupsFragment fragment = new MediaChooserGroupsFragment();
        fragment.setSearchType(Constants.SEARCH_TYPE_ALBUM);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
        transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
        // If this is not the top level media (root), we add it to the fragment back stack,
        // so that actionbar toggle and Back will work appropriately:
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Show 'groups' list fragment, where groups are artists
     */
    public void onChooseArtist() {
        LogHelper.i(TAG, "onChooseArtist");
        MediaChooserGroupsFragment fragment = new MediaChooserGroupsFragment();
        fragment.setSearchType(Constants.SEARCH_TYPE_ARTIST);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                R.animator.slide_in_from_right, R.animator.slide_out_to_left,
                R.animator.slide_in_from_left, R.animator.slide_out_to_right);
        transaction.replace(R.id.container, fragment, FRAGMENT_TAG);
        // If this is not the top level media (root), we add it to the fragment back stack,
        // so that actionbar toggle and Back will work appropriately:
        transaction.addToBackStack(null);
        transaction.commit();
    }


    private void navigateToBrowser(String mediaId) {
        LogHelper.i(TAG, "navigateToBrowser, mediaId=" + mediaId);
        //MediaChooserFragment fragment = getBrowseFragment();
        MediaChooserOptionsFragment fragment = null;
        if (fragment == null || !TextUtils.equals(fragment.getMediaId(), mediaId)) {
            fragment = new MediaChooserOptionsFragment()/*MediaChooserFragment()*/;
            fragment.setMediaId(mediaId);
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
        MediaChooserTracksFragment fragment = getBrowseFragment();
        if (fragment == null) {
            return null;
        }
        return fragment.getMediaId();
    }

    private MediaChooserTracksFragment getBrowseFragment() {
        return (MediaChooserTracksFragment) getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    protected void onMediaControllerConnected() {
        if (mVoiceSearchParams != null) {
            // If there is a bootstrap parameter to start from a search query, we
            // send it to the media session and set it to null, so it won't play again
            // when the activity is stopped/started or recreated:
            String query = mVoiceSearchParams.getString(SearchManager.QUERY);
            getSupportMediaController().getTransportControls()
                    .playFromSearch(query, mVoiceSearchParams);
            mVoiceSearchParams = null;
        }
        //getBrowseFragment().onConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        LogHelper.d(TAG, "onCreateOptionsMenu");
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
                fullScreenIntent = new Intent(this, FullScreenPlayQueueActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(fullScreenIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
