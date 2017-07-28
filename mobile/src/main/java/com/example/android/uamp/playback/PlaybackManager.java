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

package com.example.android.uamp.playback;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;


import com.example.android.uamp.R;
import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;
import com.example.android.uamp.utils.WearHelper;

import java.util.Calendar;

/**
 * Manage the interactions among the container service, the queue manager and the actual playback.
 */
public class PlaybackManager implements Playback.Callback {

    private static final String TAG = LogHelper.makeLogTag(PlaybackManager.class);
    // Action to thumbs up a media item
    private static final String CUSTOM_ACTION_THUMBS_UP = "com.example.android.uamp.THUMBS_UP";
    public static final String CUSTOM_ACTION_ADD_MUSIC_TO_QUEUE = "uk.me.asbridge.uamp.ADD_MUSIC_TO_QUEUE";
    public static final String CUSTOM_EXTRA_MEDIA_ID = "uk.me.asbridge.uamp.CUSTOM_EXTRA_MEDIA_ID";
    public static final String CUSTOM_ACTION_ADD_TRACK_TO_QUEUE = "uk.me.asbridge.uamp.ADD_TRACK_TO_QUEUE";
    public static final String CUSTOM_EXTRA_TRACK_ID = "uk.me.asbridge.uamp.CUSTOM_EXTRA_TRACK_ID";
    public static final String CUSTOM_ACTION_ADD_ARTIST_TO_QUEUE = "uk.me.asbridge.uamp.CUSTOM_ACTION_ADD_ARTIST_TO_QUEUE";
    public static final String CUSTOM_ACTION_ADD_ALBUM_TO_QUEUE = "uk.me.asbridge.uamp.CUSTOM_ACTION_ADD_ALBUM_TO_QUEUE";

    public static final String COMMAND_REMOVE_FROM_PLAYQUEUE_BY_QUEUEID= "uk.me.asbridge.uamp.COMMAND_REMOVE_FROM_PLAYQUEUE_BY_QUEUEID";
    public static final String COMMAND_PLAYQUEUE_MOVE_TO_TOP_BY_QUEUEID= "uk.me.asbridge.uamp.COMMAND_PLAYQUEUE_MOVE_TO_TOP_BY_QUEUEID";

    public static final String COMMAND_SET_SLEEP_TIMER = "uk.me.asbridge.uamp.COMMAND_SET_SLEEP_TIMER";
    public static final String COMMAND_EXTRA_PARAMETER = "uk.me.asbridge.uamp.COMMAND_EXTRA_PARAMETER";

    private MusicProvider mMusicProvider;
    private QueueManager mQueueManager;
    private Resources mResources;
    private Playback mPlayback;
    private PlaybackServiceCallback mServiceCallback;
    private MediaSessionCallback mMediaSessionCallback;

    public PlaybackManager(PlaybackServiceCallback serviceCallback, Resources resources,
                           MusicProvider musicProvider, QueueManager queueManager,
                           Playback playback) {
        mMusicProvider = musicProvider;
        mServiceCallback = serviceCallback;
        mResources = resources;
        mQueueManager = queueManager;
        mMediaSessionCallback = new MediaSessionCallback();
        mPlayback = playback;
        mPlayback.setCallback(this);
    }

    public Playback getPlayback() {
        return mPlayback;
    }

    public MediaSessionCompat.Callback getMediaSessionCallback() {
        return mMediaSessionCallback;
    }

    /**
     * Handle a request to play music
     */
    public void handlePlayRequest() {
        LogHelper.i(TAG, "handlePlayRequest: mState=" + mPlayback.getState());
        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic != null) {
            mServiceCallback.onPlaybackStart();
            mPlayback.play(currentMusic);
        }
    }

    /**
     * Handle a request to pause music
     */
    public void handlePauseRequest() {
        LogHelper.d(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
        if (mPlayback.isPlaying()) {
            mPlayback.pause();
            mServiceCallback.onPlaybackStop();
        }
    }

    /**
     * Handle a request to stop music
     *
     * @param withError Error message in case the stop has an unexpected cause. The error
     *                  message will be set in the PlaybackState and will be visible to
     *                  MediaController clients.
     */
    public void handleStopRequest(String withError) {
        LogHelper.d(TAG, "handleStopRequest: mState=" + mPlayback.getState() + " error=", withError);
        mPlayback.stop(true);
        mServiceCallback.onPlaybackStop();
        updatePlaybackState(withError);
    }


    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    public void updatePlaybackState(String error) {
        LogHelper.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        //noinspection ResourceType
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        setCustomAction(stateBuilder);
        int state = mPlayback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        //noinspection ResourceType
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic != null) {
            stateBuilder.setActiveQueueItemId(currentMusic.getQueueId());
        }

        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING ||
                state == PlaybackStateCompat.STATE_PAUSED) {
            mServiceCallback.onNotificationRequired();
        }
    }

    private void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
        MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
        if (currentMusic == null) {
            return;
        }
        // Set appropriate "Favorite" icon on Custom action:
        String mediaId = currentMusic.getDescription().getMediaId();
        if (mediaId == null) {
            return;
        }
        String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
        LogHelper.i(TAG, "for mediaId = ", mediaId, " musicId = ", musicId);
        int favoriteIcon = mMusicProvider.isFavorite(musicId) ?
                R.drawable.ic_star_on : R.drawable.ic_star_off;
        LogHelper.d(TAG, "updatePlaybackState, setting Favorite custom action of music ",
                musicId, " current favorite=", mMusicProvider.isFavorite(musicId));
        Bundle customActionExtras = new Bundle();
        WearHelper.setShowCustomActionOnWear(customActionExtras, true);
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_THUMBS_UP, mResources.getString(R.string.favorite), favoriteIcon)
                .setExtras(customActionExtras)
                .build());
    }

    private long getAvailableActions() {
        long actions =
                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion() {
        LogHelper.i(TAG, "onCompletion");
        // The media player finished playing the current song, so we go ahead
        // and start the next. Use our new call 'go to next song' instead of skip(1)
        if (mQueueManager.goToNextSong()) {
            if (timeToGoToSleep()) {
                handleStopRequest(null);
                sleepTime = null;
            } else {
                handlePlayRequest();
            }
            mQueueManager.updateMetadata();
        } else {
            // If skipping was not possible, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    @Override
    public void setCurrentMediaId(String mediaId) {
        LogHelper.i(TAG, "setCurrentMediaId", mediaId);
        mQueueManager.setQueueFromMusic(mediaId);
    }


    /**
     * Switch to a different Playback instance, maintaining all playback state, if possible.
     *
     * @param playback switch to this playback
     */
    public void switchToPlayback(Playback playback, boolean resumePlaying) {
        if (playback == null) {
            throw new IllegalArgumentException("Playback cannot be null");
        }
        // Suspends current state.
        int oldState = mPlayback.getState();
        long pos = mPlayback.getCurrentStreamPosition();
        String currentMediaId = mPlayback.getCurrentMediaId();
        mPlayback.stop(false);
        playback.setCallback(this);
        playback.setCurrentMediaId(currentMediaId);
        playback.seekTo(pos < 0 ? 0 : pos);
        playback.start();
        // Swaps instance.
        mPlayback = playback;
        switch (oldState) {
            case PlaybackStateCompat.STATE_BUFFERING:
            case PlaybackStateCompat.STATE_CONNECTING:
            case PlaybackStateCompat.STATE_PAUSED:
                mPlayback.pause();
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
                if (resumePlaying && currentMusic != null) {
                    mPlayback.play(currentMusic);
                } else if (!resumePlaying) {
                    mPlayback.pause();
                } else {
                    mPlayback.stop(true);
                }
                break;
            case PlaybackStateCompat.STATE_NONE:
                break;
            default:
                LogHelper.d(TAG, "Default called. Old state is ", oldState);
        }
    }


    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        /**
         * This is called by the session after the media controller removeQueueItem method has been called
         * @param description
         */
        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            LogHelper.i(TAG, "onRemoveQueueItem ", description);
            mQueueManager.removeQueueItemByDescription(description);
        }

        @Override
        public void onPlay() {
            LogHelper.i(TAG, "onPlay");
            if (mQueueManager.getCurrentMusic() == null) {
                mQueueManager.fillRandomQueue();
            }
            handlePlayRequest();
        }
        @Override
        public void onSkipToQueueItem(long queueId) {
            LogHelper.i(TAG, "OnSkipToQueueItem:" + queueId);
            mQueueManager.setCurrentQueueItem(queueId);
            mQueueManager.updateMetadata();
        }

        @Override
        public void onSeekTo(long position) {
            LogHelper.i(TAG, "onSeekTo:", position);
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            LogHelper.i(TAG, "playFromMediaId mediaId:", mediaId, "  extras=", extras);
            mQueueManager.setQueueFromMusic(mediaId);
            // If we call handlePlayRequest here, then we start playing after setting the queue
            handlePlayRequest();
        }

        @Override
        public void onPause() {
            LogHelper.i(TAG, "pause. current state=" + mPlayback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            LogHelper.i(TAG, "stop. current state=" + mPlayback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            LogHelper.i(TAG, "onSkipToNext");
            // Use our new call go to next song, not 'skip position (1)
            if (mQueueManager.goToNextSong() /* .skipQueuePosition(1)*/ ) {
                handlePlayRequest();
            } else {
                // skipQueuePositiong returns false if the new position is not a playable media item
                handleStopRequest("Cannot skip");
            }
            mQueueManager.updateMetadata();
        }

        @Override
        public void onSkipToPrevious() {
            LogHelper.i(TAG, "onSkipToPrevious");
            if (mQueueManager.skipQueuePosition(-1)) {
                handlePlayRequest();
            } else {
                handleStopRequest("Cannot skip");
            }
            mQueueManager.updateMetadata();
        }

        @Override
        public void onCustomAction(@NonNull String action, Bundle extras) {
            if (CUSTOM_ACTION_THUMBS_UP.equals(action)) {
                LogHelper.i(TAG, "onCustomAction: favorite for current track");
                MediaSessionCompat.QueueItem currentMusic = mQueueManager.getCurrentMusic();
                if (currentMusic != null) {
                    String mediaId = currentMusic.getDescription().getMediaId();
                    if (mediaId != null) {
                        String musicId = MediaIDHelper.extractMusicIDFromMediaID(mediaId);
                        mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId));
                    }
                }
                // playback state needs to be updated because the "Favorite" icon on the
                // custom action will change to reflect the new favorite state.
                updatePlaybackState(null);
            } else if (CUSTOM_ACTION_ADD_MUSIC_TO_QUEUE.equals(action)) {
                // New custom action to set the queue, without starting to play any media
                // Media ID is the heiracrchy string (not the long track ID)
                String mediaId = extras.getString(CUSTOM_EXTRA_MEDIA_ID);
                LogHelper.i(TAG, "onCustomAction: ADD_MUSIC_TO_QUEUE, medaiId =",mediaId);
                mQueueManager.addMusicToQueue(mediaId);
            } else if (CUSTOM_ACTION_ADD_TRACK_TO_QUEUE.equals(action)) {
                // New custom action to set the queue, without starting to play any media
                long trackId = extras.getLong(CUSTOM_EXTRA_TRACK_ID);
                LogHelper.i(TAG, "onCustomAction: ADD_TRACK_TO_QUEUE, medaiId =",trackId);
                mQueueManager.addTrackToQueue(trackId);
            } else if (CUSTOM_ACTION_ADD_ARTIST_TO_QUEUE.equals(action)) {
                // New custom action to set the queue, without starting to play any media
                long artistId = extras.getLong(CUSTOM_EXTRA_MEDIA_ID);
                LogHelper.i(TAG, "onCustomAction: CUSTOM_ACTION_ADD_ARTIST_TO_QUEUE, artistId =",artistId);
                mQueueManager.addArtistToQueue(artistId);
            } else if (CUSTOM_ACTION_ADD_ALBUM_TO_QUEUE.equals(action)) {
                // New custom action to set the queue, without starting to play any media
                long albumId = extras.getLong(CUSTOM_EXTRA_MEDIA_ID);
                LogHelper.i(TAG, "onCustomAction: CUSTOM_ACTION_ADD_ALBUM_TO_QUEUE, artistId =",albumId);
                mQueueManager.addAlbumToQueue(albumId);
            } else {
                LogHelper.e(TAG, "Unsupported action: ", action);
            }

        }

        /**
         * Handle free and contextual searches.
         * <p/>
         * All voice searches on Android Auto are sent to this method through a connected
         * {@link android.support.v4.media.session.MediaControllerCompat}.
         * <p/>
         * Threads and async handling:
         * Search, as a potentially slow operation, should run in another thread.
         * <p/>
         * Since this method runs on the main thread, most apps with non-trivial metadata
         * should defer the actual search to another thread (for example, by using
         * an {@link AsyncTask} as we do here).
         **/
        @Override
        public void onPlayFromSearch(final String query, final Bundle extras) {
            LogHelper.i(TAG, "playFromSearch  query=", query, " extras=", extras);

            mPlayback.setState(PlaybackStateCompat.STATE_CONNECTING);
            boolean successSearch = mQueueManager.setQueueFromSearch(query, extras);
            if (successSearch) {
                handlePlayRequest();
                mQueueManager.updateMetadata();
            } else {
                updatePlaybackState("Could not find music");
            }
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            LogHelper.i(TAG, "COMMAND ", command);
            long queueId;
            switch (command) {
                case COMMAND_REMOVE_FROM_PLAYQUEUE_BY_QUEUEID:
                    queueId = extras.getLong(COMMAND_EXTRA_PARAMETER);
                    mQueueManager.removeQueueItemByQueueId(queueId);
                    break;
                case COMMAND_PLAYQUEUE_MOVE_TO_TOP_BY_QUEUEID:
                    queueId = extras.getLong(COMMAND_EXTRA_PARAMETER);
                    mQueueManager.moveQueueItemToTopByQueueId(queueId);
                    break;
                case COMMAND_SET_SLEEP_TIMER:
                    int minsTillSleep = extras.getInt(COMMAND_EXTRA_PARAMETER);
                    setSleepTime(minsTillSleep);
            }
        }
    }

    private Calendar sleepTime = null;

    public void setSleepTime(int mins) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, mins);
        sleepTime = c;
    }

    public long getSecsTillSleep() {
        if (sleepTime == null) return -1;
        Calendar currentTime = Calendar.getInstance();
        long diff = sleepTime.getTimeInMillis() - currentTime.getTimeInMillis();
        long mins = diff / 1000;
        return mins;
    }

    public boolean isSleepTimerActive() {
        return (sleepTime != null);
    }

    public void cancelSleepTimer() {
        LogHelper.v(TAG, "cancel sleep timer");
        sleepTime = null;
    }

    private boolean timeToGoToSleep() {
        LogHelper.i(TAG, "timeToGoToSleep ",((sleepTime==null)?" null":sleepTime.getTimeInMillis()));
        if (sleepTime == null)
            return false; // no sleep timer set
        Calendar currentTime = Calendar.getInstance();
        LogHelper.i(TAG, "timeToGoToSleep: sleeptime = ",sleepTime.getTimeInMillis(), " current=", currentTime.getTimeInMillis() );
        if (currentTime.after(sleepTime)) {
            LogHelper.i(TAG, "Bedtime!");
            return true;
        }
        LogHelper.i(TAG, "timeToGoToSleep - not time yet");
        return false;
    }

    public interface PlaybackServiceCallback {
        void onPlaybackStart();

        void onNotificationRequired();

        void onPlaybackStop();

        void onPlaybackStateUpdated(PlaybackStateCompat newState);
    }
}
