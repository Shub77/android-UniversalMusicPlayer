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
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import com.example.android.uamp.AlbumArtCache;
import com.example.android.uamp.R;
import com.example.android.uamp.model.MusicProvider;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;
import com.example.android.uamp.utils.QueueHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Simple data provider for queues. Keeps track of a current queue and a current index in the
 * queue. Also provides methods to set the current queue based on common queries, relying on a
 * given MusicProvider to provide the actual media metadata.
 * THIS IS UNUSED... IT IS A COPY OF QUEUEMANAGER USING A LIST IMPLEMENTATION WHERE MUSIC CYCLES AROUND THE LIST
 * CAN GO FORWARDS AND BACKWARDS. I WANT TO USE MY IMPLEMENTATION OF QUEUEMANAGER WHICH IS A REAL QUEUE
 */
public class QueueManager {
    private static final String TAG = LogHelper.makeLogTag(QueueManager.class);

    private static final int RANDOM_QUEUE_SIZE = 4;

    private MusicProvider mMusicProvider;
    private MetadataUpdateListener mListener;
    private Resources mResources;

    // "Now playing" queue:
    private List<MediaSessionCompat.QueueItem> mPlayingQueue;

    // get rid of the current index and add a now playing track
    // In our implementation the track being played isn't part of the queue
    // The queue only represents tracks waiting to be played.

    // private int mCurrentIndex;
    private MediaSessionCompat.QueueItem mNowPlaying;

    public QueueManager(@NonNull MusicProvider musicProvider,
                        @NonNull Resources resources,
                        @NonNull MetadataUpdateListener listener) {
        this.mMusicProvider = musicProvider;
        this.mListener = listener;
        this.mResources = resources;

        mPlayingQueue = Collections.synchronizedList(new ArrayList<MediaSessionCompat.QueueItem>());

        // the current index is replaced by now playing in this implementation
        // mCurrentIndex = 0;
        mNowPlaying = null;
    }

    public boolean isSameBrowsingCategory(@NonNull String mediaId) {
        String[] newBrowseHierarchy = MediaIDHelper.getHierarchy(mediaId);
        MediaSessionCompat.QueueItem current = getCurrentMusic();
        if (current == null) {
            return false;
        }
        String[] currentBrowseHierarchy = MediaIDHelper.getHierarchy(
                current.getDescription().getMediaId());

        return Arrays.equals(newBrowseHierarchy, currentBrowseHierarchy);
    }

    // I added this ...
    private List<MediaSessionCompat.QueueItem> getCurrentQueue() {
        return mPlayingQueue;
    }

    // This isn't really used in our implementation
    // In our implementation we take the item out of the queue and we set it now playing
    // example implementation just set the current index
    private void setCurrentQueueIndex(int index) {
        if (index >= 0 && index < mPlayingQueue.size()) {
            mNowPlaying = mPlayingQueue.remove(index);
            // mCurrentIndex = index;

            // I've replaced onCurrentQueueIndexChanged with the following:
            mListener.onNowPlayingChanged(mNowPlaying);
            // the following call doesn't make sense anymore
            // mListener.onCurrentQueueIndexUpdated(mCurrentIndex);
        }
    }

    // In our implementation we take the item out of the queue and we set it now playing
    // example implementation just set the current index
    // No change to the code as we are using the refactored setCurrentQueueIndex()
    public boolean setCurrentQueueItem(long queueId) {
        // set the current index on queue from the queue Id:
        int index = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    // In our implementation we take the item out of the queue and we set it now playing
    // example implementation just set the current index
    // No change to the code as we are using the refactored setCurrentQueueIndex()
    public boolean setCurrentQueueItem(String mediaId) {
        // set the current index on queue from the music Id:
        int index = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, mediaId);
        setCurrentQueueIndex(index);
        return index >= 0;
    }

    /**
     * Our implementation will use this instead of skipQueuePosition
     * As our only control is 'go to next track'. No need to skip 'n' tracks
     * @return
     */
    public boolean goToNextSong() {
        LogHelper.i(TAG, "goToNextSong queue size=", mPlayingQueue.size());
        if (mPlayingQueue.size() > 0) {
            // get the next track as the first in the queue and set it to now playing
            mNowPlaying = mPlayingQueue.remove(0);

            // TEMP
            // Add another item into the queue
            // In fact fill the queue up to n places, in case the queue size is < n (queued items were removed by user, maybe)
            // Don't add items if queus alredy has >n items (items were added manually by the user)
            fillRandomQueue();
            return true;
        }
        return false;
    }

    /**
     * Skips [amount] songs through the queue
     * Amount = -1 means skip to previous track
     * @param amount
     * @return true if the new index is a valid playable media item
     */
    // We should only allow skip to next...
    // But for the moment leave it like this.
    public boolean skipQueuePosition(int amount) {
        LogHelper.i(TAG, "skip queue by ", Integer.toString(amount), "queue size=", mPlayingQueue.size());
        int index = /* mCurrentIndex + */ amount; // in principle the current index is always 0 in our implementation.
        if (index < 0) {
            // skip backwards before the first song will keep you on the first song
            index = 0;
        } else if (index >= mPlayingQueue.size()) {
            // in the example skip forwards when in last song will cycle back to start of the queue
            // index %= mPlayingQueue.size();
            // in our example it returns false
            return false;
        }
        if (!QueueHelper.isIndexPlayable(index, mPlayingQueue)) {
            LogHelper.e(TAG, "Cannot increment queue index by ", amount,
                    " queue length=", mPlayingQueue.size());
            return false;
        }
        // strange that in the example we just update the index.
        // there is no call(back) to any listener
        // so we just do the same (remove from queue and update now playing)
        mNowPlaying = mPlayingQueue.remove(index);
        // mCurrentIndex = index;
        return true;
    }

    public boolean setQueueFromSearch(String query, Bundle extras) {
        LogHelper.i(TAG, "SetQueuefromSearch: query = ", query);
        List<MediaSessionCompat.QueueItem> queue =
                QueueHelper.getPlayingQueueFromSearch(query, extras, mMusicProvider);
        String title =  mResources.getString(R.string.search_queue_title);
        setCurrentQueue(title, queue);
        mListener.onQueueUpdated(title, mPlayingQueue);
        updateMetadata();
        return queue != null && !queue.isEmpty();
    }

    /*
    // creates a random queue of n items
    public void setRandomQueue() {
        LogHelper.i(TAG, "setRandomQueue");
        setCurrentQueue(mResources.getString(R.string.random_queue_title), QueueHelper.getRandomQueue(mMusicProvider, RANDOM_QUEUE_SIZE));
        mListener.onQueueUpdated(mResources.getString(R.string.random_queue_title), mPlayingQueue);
        if (mNowPlaying == null) {
            goToNextSong();
            updateMetadata();
        }
    }
    */

    // Fills the queue up to n items by adding Random tracks.
    // If the queue is already >= n then there is no need to do anything
    public void fillRandomQueue() {
        int currentQueueSize = mPlayingQueue.size();
        LogHelper.i(TAG, "fillRandomQueue, current size = ", mPlayingQueue.size());
        if (currentQueueSize < RANDOM_QUEUE_SIZE)
        {
            List<MediaSessionCompat.QueueItem> newTracks =  QueueHelper.getRandomQueue(mMusicProvider, RANDOM_QUEUE_SIZE - currentQueueSize);
            // Add the new songs
            addToCurrentQueue("Random", newTracks, null);
        }
        mListener.onQueueUpdated("title", mPlayingQueue);
    }

    /**
     * My own implementation of the example function.
     * Takes an input media ID from the 'music browser' and adds songs to the queue
     * (The standard implementation replaces the queue)
     * @param mediaId
     */
    public void setQueueFromMusic(String mediaId) {
        LogHelper.i(TAG, "setQueueFromMusic id=", mediaId);
        String queueTitle = mResources.getString(R.string.browse_musics_by_genre_subtitle,
                MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaId));

        // get all the new tracks to add. Will add all tracks in the same category as the chosen track
        List<MediaSessionCompat.QueueItem> newTracks = QueueHelper.getPlayingQueue(mediaId, mMusicProvider);
        LogHelper.i(TAG, newTracks.size(), " new tracks");

        addToCurrentQueue(queueTitle, newTracks, mediaId);

        // here we are setting the currently playing track as the one that was chosen.
        // we just don't really need to do this. Adding things to the queue doesn't really change what is playing
        // mCurrentIndex = Math.max(index, 0);

        // In the queue implementation we can browse and just add to the queue. This doesn't change what is playing
        // Here we say that if we add to the queue and nothing is currently playing then we start playing
        // This might not really be the completely desired functionality. Would it start playing on startup when a randomised queue was intialised
        // We need a 'now playing' but we don't need to start playing it (should be paused)

        /* DISABLED Just set the queue. Don't take anything off it to now playing
        if (mNowPlaying == null)
        {
            mNowPlaying = mPlayingQueue.remove(0);
            LogHelper.i(TAG, "mNowPlaying was null, new now playing = ", mNowPlaying.getDescription().getTitle());
            // If we don't call updatemetadata then the queue in queuemanager is updated OK, but the
            // change is never communicated to the service/player activity

            // Commented update metadata and copied the code inline here to test
            //updateMetadata();

            // copied code from update metadata
            final String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                    mNowPlaying.getDescription().getMediaId());
            MediaMetadataCompat metadata = mMusicProvider.getMusic(musicId);
            if (metadata == null) {
                throw new IllegalArgumentException("Invalid musicId " + musicId);
            }

            // Call the listener to set the new metadata and playingQueue
            // If we don't call this then the player activity has no play controls
            // but if we do call it then everything starts playing
            // mListener is a MetadataUpdateListener used to call back to the service
            mListener.onMetadataChanged(metadata);
        }
        DISABLED */

        // This communicates the changed queue. It allows the queue to be shown in
        // in the player activity
        mListener.onQueueUpdated("title", mPlayingQueue);
    }

    public void updateMetadata() {
        LogHelper.i(TAG, "upda  teMetadata");
        MediaSessionCompat.QueueItem currentMusic = getCurrentMusic();
        if (currentMusic == null) {
            LogHelper.i(TAG, "current Music = null");
            mListener.onMetadataRetrieveError();
            return;
        }
        final String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                currentMusic.getDescription().getMediaId());
        MediaMetadataCompat metadata = mMusicProvider.getMusic(musicId);
        if (metadata == null) {
            throw new IllegalArgumentException("Invalid musicId " + musicId);
        }

        mListener.onMetadataChanged(metadata);
        // call onqueue updated, as well as on metadata changed
        mListener.onQueueUpdated("title", mPlayingQueue);

        // The rest of this is all about artwork, so we aren't so bothered here
        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
        if (metadata.getDescription().getIconBitmap() == null &&
                metadata.getDescription().getIconUri() != null) {
            String albumUri = metadata.getDescription().getIconUri().toString();
            AlbumArtCache.getInstance().fetch(albumUri, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    mMusicProvider.updateMusicArt(musicId, bitmap, icon);

                    // If we are still playing the same music, notify the listeners:
                    MediaSessionCompat.QueueItem currentMusic = getCurrentMusic();
                    if (currentMusic == null) {
                        return;
                    }
                    String currentPlayingId = MediaIDHelper.extractMusicIDFromMediaID(
                            currentMusic.getDescription().getMediaId());
                    if (musicId.equals(currentPlayingId)) {
                        mListener.onMetadataChanged(mMusicProvider.getMusic(currentPlayingId));
                    }
                }
            });
        }
    }

    /**
     * Takes an input from the 'music browser'
     * @param title
     * @param newQueue
     * @param initialMediaId
     */
    protected void addToCurrentQueue(String title, List<MediaSessionCompat.QueueItem> newQueue,
                                   String initialMediaId) {
        int oldqueuesize = mPlayingQueue.size();
        mPlayingQueue.addAll(newQueue);
        LogHelper.i(TAG, "addToCurrentQueue: adding ",newQueue.size(), " new items to existing queue (",oldqueuesize,")items. New queue has ", mPlayingQueue.size() , "items. Initial media id = ", initialMediaId);
        int index = 0;
        if (initialMediaId != null) {
            index = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, initialMediaId);
        }
        // TEST looks like we don't need this here. It is called from elsewhere ...!!!! uncomnet this updateMetadata(); // TEST instead of next 2 lines TEST
        // mListener.onQueueUpdated(title, mPlayingQueue);
        //mListener.onNowPlayingChanged(mNowPlaying); // we need to call this so the player gets created.
    }


    /* Commented out and replaced by my implementation which adds new music... doesn't replace the queue
    public void setQueueFromMusic(String mediaId) {
        LogHelper.i(TAG, "setQueueFromMusic id=", mediaId);

        // The mediaId used here is not the unique musicId. This one comes from the
        // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
        // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
        // so we can build the correct playing queue, based on where the track was
        // selected from.
        boolean canReuseQueue = false;
        if (isSameBrowsingCategory(mediaId)) {
            LogHelper.i(TAG, "Same Browsing Category, so we aren't recreating the queue");
            canReuseQueue = setCurrentQueueItem(mediaId);
        }
        if (!canReuseQueue) {
            LogHelper.i(TAG, "Recreating the queue based on new mediaId");
            String queueTitle = mResources.getString(R.string.browse_musics_by_genre_subtitle,
                    MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaId));
            setCurrentQueue(queueTitle,
                    QueueHelper.getPlayingQueue(mediaId, mMusicProvider), mediaId);
        }
        updateMetadata();
    }
    */

    // Easy change. No current index .. we just return 'Now playing'
    public MediaSessionCompat.QueueItem getCurrentMusic() {
        return mNowPlaying;
        /* example implementation
        if (!QueueHelper.isIndexPlayable(mCurrentIndex, mPlayingQueue)) {
            return null;
        }
        return mPlayingQueue.get(mCurrentIndex);
        */
    }

    public int getCurrentQueueSize() {
        if (mPlayingQueue == null) {
            return 0;
        }
        return mPlayingQueue.size();
    }

    protected void setCurrentQueue(String title, List<MediaSessionCompat.QueueItem> newQueue) {
        LogHelper.i(TAG, "setCurrentQueue: title=", title);
        mPlayingQueue = newQueue;
        // setCurrentQueue(title, newQueue, null);
    }


    protected void setCurrentQueue(String title, List<MediaSessionCompat.QueueItem> newQueue,
                                   String initialMediaId) {
        LogHelper.i(TAG, "setCurrentQueue: setting new queue with initial media id = ", initialMediaId);
        mPlayingQueue = newQueue;
        if (initialMediaId != null) {
            int index = 0;
            if (initialMediaId != null) {
                index = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, initialMediaId);
            }
            // don't set the mCurrent index, just set the nowPlaying instead
            int currentIndex = Math.max(index, 0);
            mNowPlaying = mPlayingQueue.remove(currentIndex);
        }
        mListener.onQueueUpdated(title, newQueue);
    }

    // this is my interface
    public interface MetadataUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onMetadataRetrieveError();
        void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue);

        // void onCurrentQueueIndexUpdated(int queueIndex);
        void onNowPlayingChanged(MediaSessionCompat.QueueItem nowPlaying);
        void onPauseRequest();

    }

    // This is from the google example
    public interface ExampleMetadataUpdateListener {
        void onMetadataChanged(MediaMetadataCompat metadata);
        void onMetadataRetrieveError();
        void onCurrentQueueIndexUpdated(int queueIndex);
        void onQueueUpdated(String title, List<MediaSessionCompat.QueueItem> newQueue);
    }
}
