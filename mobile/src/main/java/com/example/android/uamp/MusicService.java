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

 package com.example.android.uamp;

 import android.app.PendingIntent;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.os.Bundle;
 import android.os.Handler;
 import android.os.Message;
 import android.os.RemoteException;
 import android.support.annotation.NonNull;
 import android.support.v4.media.MediaBrowserCompat;
 import android.support.v4.media.MediaBrowserCompat.MediaItem;
 import android.support.v4.media.MediaBrowserServiceCompat;
 import android.support.v4.media.MediaDescriptionCompat;
 import android.support.v4.media.MediaMetadataCompat;
 import android.support.v4.media.session.MediaButtonReceiver;
 import android.support.v4.media.session.MediaSessionCompat;
 import android.support.v4.media.session.PlaybackStateCompat;
 import android.support.v7.media.MediaRouter;

 import com.example.android.uamp.ui.MediaBrowserClient.BrowsableItem;
 /*
 import com.example.android.uamp.model.CursorBasedPagedAlbumByIdMediaProvider;
 import com.example.android.uamp.model.CursorBasedPagedAlbumsMediaProvider;
 import com.example.android.uamp.model.CursorBasedPagedArtistByIdMediaProvider;
 import com.example.android.uamp.model.CursorBasedPagedArtistsMediaProvider;
 */
 import com.example.android.uamp.model.MusicProvider;
 import com.example.android.uamp.playback.*;
 import com.example.android.uamp.settings.Settings;
 import com.example.android.uamp.ui.MainLauncherActivity;
 import com.example.android.uamp.ui.MediaBrowserClient.MediaIDUampHelper;
 import com.example.android.uamp.utils.CarHelper;
 import com.example.android.uamp.utils.LogHelper;
 import com.example.android.uamp.utils.TvHelper;
 import com.example.android.uamp.utils.WearHelper;
 import com.google.android.gms.cast.framework.CastContext;
 import com.google.android.gms.cast.framework.CastSession;
 import com.google.android.gms.cast.framework.SessionManager;
 import com.google.android.gms.cast.framework.SessionManagerListener;
 import com.google.android.gms.common.ConnectionResult;
 import com.google.android.gms.common.GoogleApiAvailability;

 import java.lang.ref.WeakReference;
 import java.util.ArrayList;
 import java.util.Iterator;
 import java.util.List;
 //import com.example.android.uamp.model.CursorBasedPagedMediaProvider;

 import static com.example.android.uamp.ui.MediaBrowserClient.MediaIDUampHelper.MEDIA_ID_EMPTY_ROOT;
 import static com.example.android.uamp.ui.MediaBrowserClient.MediaIDUampHelper.MEDIA_ID_ROOT;

 /**
  * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
  * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
  * exposes it through its MediaSession.Token, which allows the client to create a MediaController
  * that connects to and send control commands to the MediaSession remotely. This is useful for
  * user interfaces that need to interact with your media session, like Android Auto. You can
  * (should) also use the same service from your app's UI, which gives a seamless playback
  * experience to the user.
  *
  * To implement a MediaBrowserService, you need to:
  *
  * <ul>
  *
  * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
  *      related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
  *      {@link android.service.media.MediaBrowserService#onLoadChildren};
  * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
  *      with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
  *
  * <li> Set a callback on the
  *      {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
  *      The callback will receive all the user's actions, like play, pause, etc;
  *
  * <li> Handle all the actual music playing using any method your app prefers (for example,
  *      {@link android.media.MediaPlayer})
  *
  * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
  *      {@link android.media.session.MediaSession#setPlaybackState(android.media.session.PlaybackState)}
  *      {@link android.media.session.MediaSession#setMetadata(android.media.MediaMetadata)} and
  *      {android.media.session.MediaSession#setQueue(java.util.List)})
  *
  * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
  *      android.media.browse.MediaBrowserService
  *
  * </ul>
  *
  * To make your app compatible with Android Auto, you also need to:
  *
  * <ul>
  *
  * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
  *      with a &lt;automotiveApp&gt; root element. For a media app, this must include
  *      an &lt;uses name="media"/&gt; element as a child.
  *      For example, in AndroidManifest.xml:
  *          &lt;meta-data android:name="com.google.android.gms.car.application"
  *              android:resource="@xml/automotive_app_desc"/&gt;
  *      And in res/values/automotive_app_desc.xml:
  *          &lt;automotiveApp&gt;
  *              &lt;uses name="media"/&gt;
  *          &lt;/automotiveApp&gt;
  *
  * </ul>

  * @see <a href="README.md">README.md</a> for more details.
  *
  */
 public class MusicService extends MediaBrowserServiceCompat implements
         PlaybackManager.PlaybackServiceCallback {

     private static final String TAG = LogHelper.makeLogTag(MusicService.class);

     // Extra on MediaSession that contains the Cast device name currently connected to
     public static final String EXTRA_CONNECTED_CAST = "com.example.android.uamp.CAST_NAME";
     // The action of the incoming Intent indicating that it contains a command
     // to be executed (see {@link #onStartCommand})
     public static final String ACTION_CMD = "com.example.android.uamp.ACTION_CMD";
     // The key in the extras of the incoming Intent indicating the command that
     // should be executed (see {@link #onStartCommand})
     public static final String CMD_NAME = "CMD_NAME";
     // A value of a CMD_NAME key in the extras of the incoming Intent that
     // indicates that the music playback should be paused (see {@link #onStartCommand})
     public static final String CMD_PAUSE = "CMD_PAUSE";
     // A value of a CMD_NAME key that indicates that the music playback should switch
     // to local playback from cast playback.
     public static final String CMD_STOP_CASTING = "CMD_STOP_CASTING";
     // Delay stopSelf by using a handler.
     private static final int STOP_DELAY = 30000;

     private MusicProvider mMusicProvider;
     private PlaybackManager mPlaybackManager;

     private MediaSessionCompat mSession;
     private MediaNotificationManager mMediaNotificationManager;
     private Bundle mSessionExtras;
     private final DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
     private MediaRouter mMediaRouter;
     private PackageValidator mPackageValidator;
     private SessionManager mCastSessionManager;
     private SessionManagerListener<CastSession> mCastSessionManagerListener;

     private boolean mIsConnectedToCar;
     private BroadcastReceiver mCarConnectionReceiver;

     // Used by the onLoadChildren to get paginated data
     /*
     private CursorBasedPagedMediaProvider mCursorBasedPagedMediaProvider;
     private CursorBasedPagedAlbumsMediaProvider mCursorBasedPagedAlbumsMediaProvider;
     private CursorBasedPagedAlbumByIdMediaProvider mCursorBasedPagedAlbumByIdMediaProvider;
     private CursorBasedPagedArtistByIdMediaProvider mCursorBasedPagedArtistByIdMediaProvider;
     private CursorBasedPagedArtistsMediaProvider mCursorBasedPagedArtistsMediaProvider;
    */
     private ArrayList<MediaItem> mHistoryList = new ArrayList<>();

     private void addItemToHistory(MediaMetadataCompat metadata) {
         int historySize = Settings.getHistorySize(getApplicationContext());
         MediaItem mediaItem = new MediaItem(metadata.getDescription(), MediaItem.FLAG_PLAYABLE);
         mHistoryList.add(0, mediaItem); // add to top of history (most recent)
         if (mHistoryList.size() > historySize) {
             int index = 0;
             Iterator<MediaItem> itr = mHistoryList.iterator();
             while (itr.hasNext()) {
                 itr.next();
                 if (index++ > historySize) {
                     LogHelper.i(TAG, "remove item from history");
                     itr.remove();
                 }
             }
         }
     }

     /*
      * (non-Javadoc)
      * @see android.app.Service#onCreate()
      */
     @Override
     public void onCreate() {
         super.onCreate();
         LogHelper.i(TAG, "onCreate");

         mMusicProvider = new MusicProvider(this);

         // To make the app more responsive, fetch and cache catalog information now.
         // This can help improve the response time in the method
         // {@link #onLoadChildren(String, Result<List<MediaItem>>) onLoadChildren()}.

         // WE DONT'T NEED TO DO THIS. OUR CATALOGUE ISN'T ONLINE
         // mMusicProvider.retrieveMediaAsync(null /* Callback */);

         mPackageValidator = new PackageValidator(this);

         // Queue manager is just our local utility class, maintaining a queue of MediaSessionCompat.QueueItem
         // Any changes to the queue (current queue index updated or queue changed will be called back to this service
         // (using QueueManager.MetadataUpdateListener)
         // So in effect the sevice holds the queue, via the queuemanager
         QueueManager queueManager = new QueueManager(mMusicProvider, getResources(), getApplicationContext(),
                 new QueueManager.MetadataUpdateListener() {
                     @Override
                     public void onMetadataChanged(MediaMetadataCompat metadata) {
                         LogHelper.i(TAG, "Service MetadataUpdateListener onMetadataChanged ", metadata.getDescription().getTitle());
                         addItemToHistory(metadata);
                         notifyChildrenChanged("HISTORY");
                         mSession.setMetadata(metadata);
                     }

                     @Override
                     public void onMetadataRetrieveError() {
                         mPlaybackManager.updatePlaybackState(
                                 getString(R.string.error_no_metadata));
                     }
/* there is no queue index
    so onCurrentQueueIndexUpdated has been replaced by onNowPlayingChanged
                    @Override
                    public void onCurrentQueueIndexUpdated(int queueIndex) {
                        mPlaybackManager.handlePlayRequest();
                    }
*/

                     @Override
                     public void onNowPlayingChanged(MediaSessionCompat.QueueItem newNowPlaying) {
                         LogHelper.i(TAG, "Service MetadataUpdateListener onNowPlayingChanged ", newNowPlaying.getDescription().getTitle(), "id=,",
                                 newNowPlaying.getDescription().getMediaId());
                         mPlaybackManager.handlePlayRequest();
                     }

                     // I added this!
                     @Override
                     public void onPauseRequest() {
                         LogHelper.i(TAG, "Service MetadataUpdateListener onPauseRequest ");
                         mSession.getController().getTransportControls().pause();
                     }

                     @Override
                     public void onQueueUpdated(String title,
                                                List<MediaSessionCompat.QueueItem> newQueue) {
                         LogHelper.i(TAG, "Service MetadataUpdateListener onQueueUpdated: size= ", newQueue.size());
                         // So when the queue is updated we tell our mediaSessionCompat
                         // The media session has callbacks which are handled by mPlaybackManager
                         mSession.setQueue(newQueue);// DISABLED
                         mSession.setQueueTitle(title);// DISABLED
                     }
                 });


         Context context = getApplicationContext();

         StoragePlayback /*LocalPlayback*/ playback = new StoragePlayback /*LocalPlayback*/ (this, mMusicProvider);
         mPlaybackManager = new PlaybackManager(this, getResources(), mMusicProvider, queueManager,
                 playback, context);

         // Start a new MediaSession
         mSession = new MediaSessionCompat(this, "MusicService");
         setSessionToken(mSession.getSessionToken());
         mSession.setCallback(mPlaybackManager.getMediaSessionCallback());
         mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                 | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                 | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);


         Intent intent = new Intent(context, MainLauncherActivity.class);
         PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                 intent, PendingIntent.FLAG_UPDATE_CURRENT);
         mSession.setSessionActivity(pi);

         mSessionExtras = new Bundle();
         CarHelper.setSlotReservationFlags(mSessionExtras, true, true, true);
         WearHelper.setSlotReservationFlags(mSessionExtras, true, true);
         WearHelper.setUseBackgroundFromTheme(mSessionExtras, true);
         mSession.setExtras(mSessionExtras);

         mPlaybackManager.updatePlaybackState(null);

         try {
             mMediaNotificationManager = new MediaNotificationManager(this);
         } catch (RemoteException e) {
             throw new IllegalStateException("Could not create a MediaNotificationManager", e);
         }

         int playServicesAvailable =
                 GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

         if (!TvHelper.isTvUiMode(this) && playServicesAvailable == ConnectionResult.SUCCESS) {
             mCastSessionManager = CastContext.getSharedInstance(this).getSessionManager();
             mCastSessionManagerListener = new CastSessionManagerListener();
             mCastSessionManager.addSessionManagerListener(mCastSessionManagerListener,
                     CastSession.class);
         }

         mMediaRouter = MediaRouter.getInstance(getApplicationContext());

         registerCarConnectionReceiver();
         // fill an initial random queue. Does not set any song to play
         queueManager.fillRandomQueue();
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
      */
     @Override
     public int onStartCommand(Intent startIntent, int flags, int startId) {
         if (startIntent != null) {
             String action = startIntent.getAction();
             String command = startIntent.getStringExtra(CMD_NAME);
             if (ACTION_CMD.equals(action)) {
                 if (CMD_PAUSE.equals(command)) {
                     mPlaybackManager.handlePauseRequest();
                 } else if (CMD_STOP_CASTING.equals(command)) {
                     CastContext.getSharedInstance(this).getSessionManager().endCurrentSession(true);
                 }
             } else {
                 // Try to handle the intent as a media button event wrapped by MediaButtonReceiver
                 MediaButtonReceiver.handleIntent(mSession, startIntent);
             }
         }
         // Reset the delay handler to enqueue a message to stop the service if
         // nothing is playing.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
         return START_STICKY;
     }

     /**
      * (non-Javadoc)
      * @see android.app.Service#onDestroy()
      */
     @Override
     public void onDestroy() {
         LogHelper.d(TAG, "onDestroy");
         unregisterCarConnectionReceiver();
         // Service is being killed, so make sure we release our resources
         mPlaybackManager.handleStopRequest(null);
         mMediaNotificationManager.stopNotification();

         if (mCastSessionManager != null) {
             mCastSessionManager.removeSessionManagerListener(mCastSessionManagerListener,
                     CastSession.class);
         }

         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mSession.release();
     }

     @Override
     public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid,
                                  Bundle rootHints) {
         LogHelper.i(TAG, "OnGetRoot: clientPackageName=" + clientPackageName,
                 "; clientUid=" + clientUid + " ; rootHints=", rootHints);
         // To ensure you are not allowing any arbitrary app to browse your app's contents, you
         // need to check the origin:
         if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
             // If the request comes from an untrusted package, return an empty browser root.
             // If you return null, then the media browser will not be able to connect and
             // no further calls will be made to other media browsing methods.
             LogHelper.i(TAG, "OnGetRoot: Browsing NOT ALLOWED for unknown caller. "
                     + "Returning empty browser root so all apps can use MediaController."
                     + clientPackageName);
             return new MediaBrowserServiceCompat.BrowserRoot(MediaIDUampHelper.MEDIA_ID_EMPTY_ROOT, null);
         }
         //noinspection StatementWithEmptyBody
         if (CarHelper.isValidCarPackage(clientPackageName)) {
             // Optional: if your app needs to adapt the music library to show a different subset
             // when connected to the car, this is where you should handle it.
             // If you want to adapt other runtime behaviors, like tweak ads or change some behavior
             // that should be different on cars, you should instead use the boolean flag
             // set by the BroadcastReceiver mCarConnectionReceiver (mIsConnectedToCar).
         }
         //noinspection StatementWithEmptyBody
         if (WearHelper.isValidWearCompanionPackage(clientPackageName)) {
             // Optional: if your app needs to adapt the music library for when browsing from a
             // Wear device, you should return a different MEDIA ROOT here, and then,
             // on onLoadChildren, handle it accordingly.
         }
         LogHelper.i(TAG,"return root");
         return new BrowserRoot(MediaIDUampHelper.MEDIA_ID_ROOT, null);
     }

     private List<MediaBrowserCompat.MediaItem> mapToMediaItems(List<BrowsableItem> browsableItems) {
         List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
         for (BrowsableItem browsableItem : browsableItems) {
             MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                     .setTitle(browsableItem.title)
                     .setSubtitle(browsableItem.subtitle)
                     .setMediaId(browsableItem.mediaId)
                     .build();

             int flags = 0;
             if (browsableItem.isPlayable) {
                 flags |= MediaItem.FLAG_PLAYABLE;
             }
             if (browsableItem.isBrowsable) {
                 flags |= MediaItem.FLAG_BROWSABLE;
             }

             MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, flags);
             mediaItems.add(mediaItem);
         }

         return mediaItems;
     }

     /**
      * Overriden method of MediaBrowserServiceCompat
      * Used to get history
      * @param parentMediaId
      * @param result
      */
     @Override
     public void onLoadChildren(@NonNull final String parentMediaId,
                                @NonNull final Result<List<MediaItem>> result) {
         LogHelper.i(TAG, "OnLoadChildren id=", parentMediaId);
         if (parentMediaId.equals(MediaIDUampHelper.MEDIA_ID_HISTORY)) {
             result.sendResult(mHistoryList);
         } else if (parentMediaId.equals(MediaIDUampHelper.MEDIA_ID_ALL_SONGS)) {
             ArrayList<MediaItem> mediaItems = mMusicProvider.getAllSongs();
             result.sendResult(mediaItems);
         } else if (parentMediaId.equals(MediaIDUampHelper.MEDIA_ID_ARTISTS)) {
             ArrayList<MediaItem> mediaItems = mMusicProvider.getArtistMediaItems();
             LogHelper.i(TAG, "found ", mediaItems.size()," items");
             result.sendResult(mediaItems);
         } else if (parentMediaId.equals(MediaIDUampHelper.MEDIA_ID_ALBUMS)) {
             ArrayList<MediaItem> mediaItems = mMusicProvider.getAlbumMediaItems();
             LogHelper.i(TAG, "found ", mediaItems.size()," items");
             result.sendResult(mediaItems);
         } else if (parentMediaId.startsWith(MediaIDUampHelper.MEDIA_ID_ARTIST)) {
             String artistMediaID = MediaIDUampHelper.getArtistIdFromMediaId(parentMediaId);
             LogHelper.i(TAG, "id = ", artistMediaID);
             ArrayList<MediaItem> mediaItems = mMusicProvider.getMediaItemsByArtist(artistMediaID);
             result.sendResult(mediaItems);
         } else if (parentMediaId.startsWith(MediaIDUampHelper.MEDIA_ID_ALBUM)) {
             String AlbumMediaId = MediaIDUampHelper.getAlbumIdFromMediaId(parentMediaId);
             LogHelper.i(TAG, "id = ", AlbumMediaId);
             ArrayList<MediaItem> mediaItems = mMusicProvider.getMediaItemsByAlbum(AlbumMediaId);
             result.sendResult(mediaItems);
         } else if (parentMediaId.equals(MEDIA_ID_ROOT)) {
             ArrayList<BrowsableItem> mainMenuItems = new ArrayList<>();
             BrowsableItem s;
             s = new BrowsableItem("All Songs", "View All songs on the device", MediaIDUampHelper.MEDIA_ID_ALL_SONGS,false, true);
             mainMenuItems.add(s);
             s = new BrowsableItem("History", "View recently played songs", MediaIDUampHelper.MEDIA_ID_HISTORY, false, true);
             mainMenuItems.add(s);
             s = new BrowsableItem("Albums", "View all albums", MediaIDUampHelper.MEDIA_ID_ALBUMS, false, true);
             mainMenuItems.add(s);
             s = new BrowsableItem("Artists", "View all artists", MediaIDUampHelper.MEDIA_ID_ARTISTS, false, true);
             mainMenuItems.add(s);
             List<MediaBrowserCompat.MediaItem> mediaItems = mapToMediaItems(mainMenuItems);
             result.sendResult(mediaItems);
         } else {
             result.sendResult(null);
         }
     }

     /**
      * Callback method called from PlaybackManager whenever the music is about to play.
      */
     @Override
     public void onPlaybackStart() {
         mSession.setActive(true);

         mDelayedStopHandler.removeCallbacksAndMessages(null);

         // The service needs to continue running even after the bound client (usually a
         // MediaController) disconnects, otherwise the music playback will stop.
         // Calling startService(Intent) will keep the service running until it is explicitly killed.
         startService(new Intent(getApplicationContext(), MusicService.class));
     }


     /**
      * Callback method called from PlaybackManager whenever the music stops playing.
      */
     @Override
     public void onPlaybackStop() {
         mSession.setActive(false);
         // Reset the delayed stop handler, so after STOP_DELAY it will be executed again,
         // potentially stopping the service.
         mDelayedStopHandler.removeCallbacksAndMessages(null);
         mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
         stopForeground(true);
     }

     @Override
     public void onNotificationRequired() {
         mMediaNotificationManager.startNotification();
     }

     @Override
     public void onPlaybackStateUpdated(PlaybackStateCompat newState) {
         mSession.setPlaybackState(newState);
     }

     private void registerCarConnectionReceiver() {
         IntentFilter filter = new IntentFilter(CarHelper.ACTION_MEDIA_STATUS);
         mCarConnectionReceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
                 String connectionEvent = intent.getStringExtra(CarHelper.MEDIA_CONNECTION_STATUS);
                 mIsConnectedToCar = CarHelper.MEDIA_CONNECTED.equals(connectionEvent);
                 LogHelper.i(TAG, "Connection event to Android Auto: ", connectionEvent,
                         " isConnectedToCar=", mIsConnectedToCar);
             }
         };
         registerReceiver(mCarConnectionReceiver, filter);
     }

     private void unregisterCarConnectionReceiver() {
         unregisterReceiver(mCarConnectionReceiver);
     }

     /**
      * A simple handler that stops the service if playback is not active (playing)
      */
     private static class DelayedStopHandler extends Handler {
         private final WeakReference<MusicService> mWeakReference;

         private DelayedStopHandler(MusicService service) {
             mWeakReference = new WeakReference<>(service);
         }

         @Override
         public void handleMessage(Message msg) {
             MusicService service = mWeakReference.get();
             if (service != null && service.mPlaybackManager.getPlayback() != null) {
                 if (service.mPlaybackManager.getPlayback().isPlaying()) {
                     LogHelper.d(TAG, "Ignoring delayed stop since the media player is in use.");
                     return;
                 }
                 LogHelper.d(TAG, "Stopping service with delay handler.");
                 service.stopSelf();
             }
         }
     }

     /**
      * Session Manager Listener responsible for switching the Playback instances
      * depending on whether it is connected to a remote player.
      */
     private class CastSessionManagerListener implements SessionManagerListener<CastSession> {

         @Override
         public void onSessionEnded(CastSession session, int error) {
             LogHelper.d(TAG, "onSessionEnded");
             mSessionExtras.remove(EXTRA_CONNECTED_CAST);
             mSession.setExtras(mSessionExtras);
             Playback playback = new StoragePlayback /*LocalPlayback*/ (MusicService.this, mMusicProvider);
             mMediaRouter.setMediaSessionCompat(null);
             mPlaybackManager.switchToPlayback(playback, false);
         }

         @Override
         public void onSessionResumed(CastSession session, boolean wasSuspended) {
         }

         @Override
         public void onSessionStarted(CastSession session, String sessionId) {
             // In case we are casting, send the device name as an extra on MediaSession metadata.
             mSessionExtras.putString(EXTRA_CONNECTED_CAST,
                     session.getCastDevice().getFriendlyName());
             mSession.setExtras(mSessionExtras);
             // Now we can switch to CastPlayback
             Playback playback = new CastPlayback(mMusicProvider, MusicService.this);
             mMediaRouter.setMediaSessionCompat(mSession);
             mPlaybackManager.switchToPlayback(playback, true);
         }

         @Override
         public void onSessionStarting(CastSession session) {
         }

         @Override
         public void onSessionStartFailed(CastSession session, int error) {
         }

         @Override
         public void onSessionEnding(CastSession session) {
             // This is our final chance to update the underlying stream position
             // In onSessionEnded(), the underlying CastPlayback#mRemoteMediaClient
             // is disconnected and hence we update our local value of stream position
             // to the latest position.
             mPlaybackManager.getPlayback().updateLastKnownStreamPosition();
         }

         @Override
         public void onSessionResuming(CastSession session, String sessionId) {
         }

         @Override
         public void onSessionResumeFailed(CastSession session, int error) {
         }

         @Override
         public void onSessionSuspended(CastSession session, int reason) {
         }
     }
 }