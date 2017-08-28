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

import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.*;
import com.example.android.uamp.AlbumArtCache;
import com.example.android.uamp.MusicService;
import com.example.android.uamp.R;
import com.example.android.uamp.model.PlayQueueAdapter;
import com.example.android.uamp.playback.PlaybackManager;
import com.example.android.uamp.settings.Settings;
import com.example.android.uamp.ui.dialogs.SetTimerDialog;
import com.example.android.uamp.utils.LogHelper;
import android.support.design.widget.NavigationView;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static java.lang.System.currentTimeMillis;

/**
 * A full screen player that shows the current playing music with a list of the current play queue
 * The activity also has controls to seek/pause/play the audio.
 * The activity implements PlayQueueAdapter.PlayQueueActionsListener to receive callbacks
 * from playqueue item buttons (e.g. remove item from playqueue)
 */
public class FullScreenPlayQueueActivity extends ActionBarCastActivity
        implements PlayQueueAdapter.PlayQueueActionsListener,
        SetTimerDialog.OnSleepTimerChangedListener

{
    private static final String TAG = LogHelper.makeLogTag(FullScreenPlayQueueActivity.class);
    private static final long PROGRESS_UPDATE_INTERNAL = 1000;
    private static final long PROGRESS_UPDATE_INITIAL_INTERVAL = 100;

    private ImageView mSkipPrev;
    private ImageView mSkipNext;
    private ImageView mPlayPause;
    private TextView mStart;
    private TextView mEnd;
    private SeekBar mSeekbar;
    private TextView mLine1;
    private TextView mLine2;
    private TextView mLine3;
    private ProgressBar mLoading;
    private View mControllers;
    private TextView mSleepIndicator;
    private Drawable mPauseDrawable;
    private Drawable mPlayDrawable;
    private ListView mPlayqueueList;

    private String mCurrentArtUrl;
    private final Handler mHandler = new Handler();
    private MediaBrowserCompat mMediaBrowser;

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final ScheduledExecutorService mExecutorService =
        Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<?> mScheduleFuture;
    private PlaybackStateCompat mLastPlaybackState;

    private PlayQueueAdapter playQueueAdapter;

    private  MediaControllerCompat mediaController;

    private final MediaControllerCompat.Callback mCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            LogHelper.i(TAG, "onPlaybackstate changed", state);
            updatePlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            if (metadata != null) {
                updateMediaDescription(metadata.getDescription());
                updateDuration(metadata);
            }
        }

        /**
         * I addded this.
         * Callback from the media session when the queue changes
         * @param queue
         */
        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            LogHelper.i(TAG, "onQueueChanger (mediaControllerCompat Callback");
            playQueueAdapter.clear();
            for (MediaSessionCompat.QueueItem item : queue) {
                playQueueAdapter.add(item);
            }
            playQueueAdapter.notifyDataSetChanged();
        }
    };

    private final MediaBrowserCompat.ConnectionCallback mConnectionCallback =
            new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            LogHelper.i(TAG, "MediaBrowserCompat.ConnectionCallback: onConnected");
            try {
                connectToSession(mMediaBrowser.getSessionToken());
            } catch (RemoteException e) {
                LogHelper.e(TAG, e, "could not connect media controller");
            }
            LogHelper.i(TAG, "onConnected: Connected to media controller");
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        LogHelper.d(TAG, "onCreateOptionsMenu");
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.player_toolbar, menu);
        return true;
    }

    // handle user interaction with the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent fullScreenIntent;
        switch (item.getItemId()) {
            case R.id.action_show_now_playing:
                fullScreenIntent= new Intent(this, FullScreenPlayQueueActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(fullScreenIntent);

                return true;
            case R.id.action_show_choose:
                fullScreenIntent = new Intent(this, MusicChooserActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(fullScreenIntent);
                return true;
                /*
            case R.id.action_timer:
                showTimerDialog();
                return true;*/
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogHelper.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_playqueue);
        initializeToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mPlayqueueList = (ListView) findViewById(R.id.playqueuelist);
        mPauseDrawable = ContextCompat.getDrawable(this, R.drawable.uamp_ic_pause_white_48dp);
        mPlayDrawable = ContextCompat.getDrawable(this, R.drawable.uamp_ic_play_arrow_white_48dp);
        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        mSkipNext = (ImageView) findViewById(R.id.next);
        mSkipPrev = (ImageView) findViewById(R.id.prev);
        mStart = (TextView) findViewById(R.id.startText);
        mEnd = (TextView) findViewById(R.id.endText);
        mSeekbar = (SeekBar) findViewById(R.id.seekBar1);
        mLine1 = (TextView) findViewById(R.id.line1);
        mLine2 = (TextView) findViewById(R.id.line2);
        mLine3 = (TextView) findViewById(R.id.line3);
        mLoading = (ProgressBar) findViewById(R.id.progressBar1);
        mControllers = findViewById(R.id.controllers);

        mSleepIndicator = (TextView) findViewById(R.id.sleepIndicator);
        if (getMsTillSleep() < 0) {
            mSleepIndicator.setVisibility(View.INVISIBLE);
        } else {
            mSleepIndicator.setVisibility(View.VISIBLE);
        }


        playQueueAdapter = new PlayQueueAdapter(this);
        mPlayqueueList.setAdapter(playQueueAdapter);



        mSkipNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogHelper.i(TAG, "mSkipNext onClickListener");
                MediaControllerCompat.TransportControls controls =
                    getSupportMediaController().getTransportControls();
                controls.skipToNext();
            }
        });

        mSkipPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControllerCompat.TransportControls controls =
                    getSupportMediaController().getTransportControls();
                controls.skipToPrevious();
            }
        });

        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlaybackStateCompat state = getSupportMediaController().getPlaybackState();
                if (state != null) {
                    MediaControllerCompat.TransportControls controls =
                            getSupportMediaController().getTransportControls();
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING: // fall through
                        case PlaybackStateCompat.STATE_BUFFERING:
                            controls.pause();
                            stopSeekbarUpdate();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                        case PlaybackStateCompat.STATE_STOPPED:
                            controls.play();
                            scheduleSeekbarUpdate();
                            break;
                        default:
                            LogHelper.i(TAG, "onClick with state ", state.getState());
                    }
                }
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mStart.setText(DateUtils.formatElapsedTime(progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekbarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getSupportMediaController().getTransportControls().seekTo(seekBar.getProgress());
                scheduleSeekbarUpdate();
            }
        });

        // Only update from the intent if we are not recreating from a config change:
        if (savedInstanceState == null) {
            updateFromParams(getIntent());
        }

        mMediaBrowser = new MediaBrowserCompat(this,
            new ComponentName(this, MusicService.class), mConnectionCallback, null);
    }

    private void connectToSession(MediaSessionCompat.Token token) throws RemoteException {
        LogHelper.i(TAG, "connectToSession");
        /* moved to class MediaControllerCompat */ mediaController = new MediaControllerCompat(
                FullScreenPlayQueueActivity.this, token);

        // get the queue and show in the list
        List< MediaSessionCompat.QueueItem> queue = mediaController.getQueue();
        LogHelper.i(TAG, "connectToSession: queue == null?", queue==null);
        if (queue != null) {
            LogHelper.i(TAG, "connectToSession: queue size = ", queue.size());
            playQueueAdapter.clear();
            for (MediaSessionCompat.QueueItem item : queue) {
                playQueueAdapter.add(item);
            }
            playQueueAdapter.notifyDataSetChanged();
        } else {
            LogHelper.i(TAG, "Queue is null");
        }

        setSupportMediaController(mediaController);
        mediaController.registerCallback(mCallback);
        PlaybackStateCompat state = mediaController.getPlaybackState();
        updatePlaybackState(state);

        if (mediaController.getMetadata() == null) {
            // Here we don't have anything playing (no current item)
            // We enable the 'skip to next' control only, as this will start playing the first item on the queue
            LogHelper.i(TAG, "connectToSession, mediacontroller.getMetadata() is null");
            // Don't finish() just because nothing is playing. We still want to see the queue and the play controls.
            mPlayPause.setVisibility(INVISIBLE);
            mControllers.setVisibility(VISIBLE);
            mSkipNext.setVisibility(VISIBLE);
            mSeekbar.setVisibility(INVISIBLE);
            mSkipPrev.setVisibility(INVISIBLE);

            return;
        }

        MediaMetadataCompat metadata = mediaController.getMetadata();
        if (metadata != null) {
            updateMediaDescription(metadata.getDescription());
            updateDuration(metadata);
        } else {
            LogHelper.i(TAG, "Metadata is null");
        }
        updateProgress();
        if (state != null && (state.getState() == PlaybackStateCompat.STATE_PLAYING ||
                state.getState() == PlaybackStateCompat.STATE_BUFFERING)) {
            scheduleSeekbarUpdate();
        }
    }

    private void updateFromParams(Intent intent) {
        if (intent != null) {
            MediaDescriptionCompat description = intent.getParcelableExtra(
                MusicPlayerActivity.EXTRA_CURRENT_MEDIA_DESCRIPTION);
            if (description != null) {
                updateMediaDescription(description);
            }
        }
    }

    private void scheduleSeekbarUpdate() {
        stopSeekbarUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(mUpdateProgressTask);
                        }
                    }, PROGRESS_UPDATE_INITIAL_INTERVAL,
                    PROGRESS_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopSeekbarUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    @Override
    public void onStart() {
        LogHelper.i(TAG, "onStart");
        super.onStart();
        if (mMediaBrowser != null) {
            mMediaBrowser.connect();
        }
    }

    @Override
    public void onStop() {
        LogHelper.i(TAG, "onStop");
        super.onStop();
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        if (getSupportMediaController() != null) {
            getSupportMediaController().unregisterCallback(mCallback);
        }
    }

    @Override
    public void onDestroy() {
        LogHelper.i(TAG, "onDestroy");
        super.onDestroy();
        stopSeekbarUpdate();
        mExecutorService.shutdown();
    }

    private void fetchImageAsync(@NonNull MediaDescriptionCompat description) {
        if (description.getIconUri() == null) {
            return;
        }
        String artUrl = description.getIconUri().toString();
        mCurrentArtUrl = artUrl;
        AlbumArtCache cache = AlbumArtCache.getInstance();
        Bitmap art = cache.getBigImage(artUrl);
        if (art == null) {
            art = description.getIconBitmap();
        }
        if (art != null) {
            // if we have the art cached or from the MediaDescription, use it:
//            mBackgroundImage.setImageBitmap(art);
        } else {
            // otherwise, fetch a high res version and update:
            cache.fetch(artUrl, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    // sanity check, in case a new fetch request has been done while
                    // the previous hasn't yet returned:
                    if (artUrl.equals(mCurrentArtUrl)) {
//                        mBackgroundImage.setImageBitmap(bitmap);
                    }
                }
            });
        }
    }

    private void updateMediaDescription(MediaDescriptionCompat description) {
        if (description == null) {
            return;
        }
        LogHelper.i(TAG, "updateMediaDescription called ");
        mLine1.setText(description.getTitle());
        mLine2.setText(description.getSubtitle());
//        fetchImageAsync(description);
    }

    private void updateDuration(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        LogHelper.i(TAG, "updateDuration called ");
        int duration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mSeekbar.setMax(duration);
        mEnd.setText(DateUtils.formatElapsedTime(duration/1000));
    }

    private void updatePlaybackState(PlaybackStateCompat state) {
        LogHelper.i(TAG, "updatePlaybackState called, state =  ", state);
        if (state == null) {
            return;
        }
        mLastPlaybackState = state;
        if (getSupportMediaController() != null && getSupportMediaController().getExtras() != null) {
            String castName = getSupportMediaController()
                    .getExtras().getString(MusicService.EXTRA_CONNECTED_CAST);
            String line3Text = castName == null ? "" : getResources()
                        .getString(R.string.casting_to_device, castName);
            mLine3.setText(line3Text);
        }
        mSeekbar.setVisibility(VISIBLE);
        switch (state.getState()) {
            case PlaybackStateCompat.STATE_PLAYING:
                mLoading.setVisibility(INVISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPauseDrawable);
                mControllers.setVisibility(VISIBLE);
                scheduleSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                mControllers.setVisibility(VISIBLE);
                mLoading.setVisibility(INVISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_NONE:
                LogHelper.i(TAG, "STATE_NONE");
                mControllers.setVisibility(VISIBLE);
                mLoading.setVisibility(INVISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_STOPPED:
                LogHelper.i(TAG, "STATE_STOPPED");
                mControllers.setVisibility(VISIBLE); // CHANGED
                mLoading.setVisibility(INVISIBLE);
                mPlayPause.setVisibility(VISIBLE);
                mPlayPause.setImageDrawable(mPlayDrawable);
                stopSeekbarUpdate();
                break;
            case PlaybackStateCompat.STATE_BUFFERING:
                mPlayPause.setVisibility(INVISIBLE);
                mLoading.setVisibility(VISIBLE);
                mLine3.setText(R.string.loading);
                stopSeekbarUpdate();
                break;
            default:
                LogHelper.i(TAG, "Unhandled state ", state.getState());
        }

        mSkipNext.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) == 0
            ? INVISIBLE : VISIBLE );
        mSkipPrev.setVisibility((state.getActions() & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) == 0
            ? INVISIBLE : VISIBLE );
    }

    private void updateProgress() {
        if (mLastPlaybackState == null) {
            return;
        }
        long currentPosition = mLastPlaybackState.getPosition();
        if (mLastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            // Calculate the elapsed time between the last position update and now and unless
            // paused, we can assume (delta * speed) + current position is approximately the
            // latest position. This ensure that we do not repeatedly call the getPlaybackState()
            // on MediaControllerCompat.
            long timeDelta = SystemClock.elapsedRealtime() -
                    mLastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
        }
        mSeekbar.setProgress((int) currentPosition);
    }

    /**
     * Implements method from PlayQueueAdapter.PlayQueueActionsListener
     * Called by the PlayQueueAdapter when remove button is clicked on an item
     * @param queueId
     * @param description
     */
    @Override
    public void onRemoveSongClicked(long queueId, MediaDescriptionCompat description) {
        LogHelper.i(TAG, "onRemoveSongClicked ",  queueId, "description =", description);
        // this will cause the media session to call MediaSessionCallback.onRemoveQueueItem in PlaybackManager
        //mediaController.removeQueueItem(description);
        Bundle bundle = new Bundle();
        bundle.putLong(PlaybackManager.COMMAND_EXTRA_PARAMETER, queueId);
        mediaController.sendCommand(PlaybackManager.COMMAND_REMOVE_FROM_PLAYQUEUE_BY_QUEUEID,bundle,null);
    }

    /**
     * Implements method from PlayQueueAdapter.PlayQueueActionsListener
     * Called by the PlayQueueAdapter when move to top button is clicked on an item
     * @param queueId

     */
    @Override
    public void onMoveSongToTopClicked(long queueId) {
        LogHelper.i(TAG, "onMoveSongToTopClicked ",  queueId);
        // this will cause the media session to call MediaSessionCallback.onMoveQueueItemToTop in PlaybackManager
        Bundle bundle = new Bundle();
        bundle.putLong(PlaybackManager.COMMAND_EXTRA_PARAMETER, queueId);
        mediaController.sendCommand(PlaybackManager.COMMAND_PLAYQUEUE_MOVE_TO_TOP_BY_QUEUEID,bundle,null);
    }

    @Override
    public void handleExtraDrawerItems(int itemToOpenWhenDrawerCloses) {
        LogHelper.i(TAG, "handleExtraDrawerItems ");
        switch (itemToOpenWhenDrawerCloses) {
            case R.id.navigation_sleep:
                showTimerDialog();
                break;
        }
    }

    @Override
    public void handleDrawerOpening() {
        super.handleDrawerOpening();

        LogHelper.i(TAG, "opening");
        long timeToGoToSleep = Settings.getTimeToGoToSleep(this);
        String title;

        if (timeToGoToSleep == 0) {
            title = "Set sleep timer";
        } else {
            title = "Cancel sleep timer";
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.navigation_sleep).setTitle(title);

    }

    /**
     * Returns the number of ms until we should sleep
     * @return 0 if no sleep timer set
     */
    private long getMsTillSleep() {
        long timeToGoToSleep = Settings.getTimeToGoToSleep(this);
        if (timeToGoToSleep == 0) {
            return 0;
        }
        long currentTimeInMS = System.currentTimeMillis();
        long msTillSleep = timeToGoToSleep - currentTimeInMS;
        return msTillSleep;
    }

    // For the sleep timer dialog
    public void showTimerDialog() {
        LogHelper.i(TAG, "showTimerDialog: ");
        FragmentManager fm = getFragmentManager();

        long msTillSleep = getMsTillSleep();

        if (msTillSleep < 0) {
            SetTimerDialog setSleepTimerDialog = new SetTimerDialog();
            setSleepTimerDialog.setOnSetSleepTimerListener(this);
            setSleepTimerDialog.show(fm, "fragment_settimer_dialog");
        } else {
            // sleep timer is active ... allow user to cancel
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            String msg;
            if (msTillSleep < 0) {
                msg = "Sleep at end of playing song";
            } else if (msTillSleep < 60 * 1000) {
                msg = "Sleep in less than one minute";
            } else {
                msg = "Sleep in " + Long.toString(msTillSleep/1000) + " seconds ";
            }

            builder.setTitle("Cancel sleep timer")
                    .setMessage(msg + "\nCancel?")
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LogHelper.i(TAG, "Positive button onClick: ");

                            Settings.setTimeToGoToSleep(FullScreenPlayQueueActivity.this , 0);
                            mSleepIndicator.setVisibility(View.INVISIBLE);
                            dialog.dismiss();
                        }
                    });

            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onSleepTimerChanged(int minsTillSleep) {
        LogHelper.i(TAG, "onSleepTimerChanged: ", minsTillSleep);

        long currentTimeinMS = System.currentTimeMillis();
        long timeToGoToSleep = currentTimeinMS + minsTillSleep * 60 * 1000;
        Settings.setTimeToGoToSleep(this , timeToGoToSleep);
        mSleepIndicator.setVisibility(View.VISIBLE);
    }
}
