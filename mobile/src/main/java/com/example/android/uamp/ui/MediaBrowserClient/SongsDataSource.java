package com.example.android.uamp.ui.MediaBrowserClient;

import android.arch.paging.PositionalDataSource;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Position-based data loader for a fixed-size, countable data set,
 * supporting fixed-size loads at arbitrary page positions
 * The client using the PositionalDataSource can ask for M items starting at position N,
 * for arbitrary values of M and N
 */
public class SongsDataSource extends PositionalDataSource<BrowsableItem> {
    private static final String TAG = LogHelper.makeLogTag(SongsDataSource.class);

    // Loaded pages is a cache of previously loaded pages
    private Set<Integer> loadedPages = new HashSet<>();

    // mMediaBrowser and media ID parameterise this data source
    // where to get data from
    private final MediaBrowserCompat mMediaBrowser;
    // what data to get
    private String mMediaId;

    SongsDataSource(MediaBrowserCompat mediaBrowser, String mediaId) {
        this.mMediaBrowser = mediaBrowser;
        this.mMediaId = mediaId;
    }

    /**
     * This method is called to load the initial data from the DataSource.
     * Result list must be a multiple of pageSize to enable efficient tiling.
     * @param params Parameters for initial load, including page size.
     * @param callback LoadInitialCallback: Callback that receives initial load data,
     *                 including position and total data set size.
     */
    @Override
    public void loadInitial(@NonNull final LoadInitialParams params, @NonNull final LoadInitialCallback<BrowsableItem> callback) {
        // extra is a bundle of "extra" parameters to pass to  MediBrowser.subscribe
        Bundle extra = new Bundle();
        extra.putInt(MediaBrowserCompat.EXTRA_PAGE, 0);
        extra.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, params.pageSize);

        LogHelper.i(TAG, "loadInitial. mediaId = ", mMediaId);
        mMediaBrowser.subscribe(mMediaId, extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                LogHelper.i(TAG, "loadInitial SubscriptionCallback.onChildrenLoaded");
                loadedPages.add(0);
                List<BrowsableItem> browsableItems = MapToSongs(children);
                callback.onResult(browsableItems, params.requestedStartPosition);
                // Normally we might want to stay subscribed to get changes in the music library
                // But here we just want to get a page and return
                mMediaBrowser.unsubscribe(mMediaId);
            }
        });
    }

    /**
     * This method is called to load a specified number of items starting at a specified position
     * Result list must be a multiple of pageSize to enable efficient tiling.
     * @param params Parameters for initial load, including requested start position,
     *               load size, and page size.
     * @param callback LoadInitialCallback: Callback that receives initial load data,
     *                 including position and total data set size.
     */
    @Override
    public void loadRange(@NonNull final LoadRangeParams params, @NonNull final LoadRangeCallback<BrowsableItem> callback) {
        final int pageIndex = params.startPosition / params.loadSize;
        if (loadedPages.contains(pageIndex)) {
            callback.onResult(new ArrayList<BrowsableItem>());
            return;
        }

        String parentId = mMediaId;
        LogHelper.i(TAG, "loadRange. ParentId = ", parentId);
        Bundle extra = new Bundle();
        // The page number (position/load size)
        extra.putInt(MediaBrowserCompat.EXTRA_PAGE, pageIndex);
        // Page size
        extra.putInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, params.loadSize);
        mMediaBrowser.subscribe(parentId, extra, new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children, @NonNull Bundle options) {
                LogHelper.i(TAG, "loadRange SubscriptionCallback.onChildrenLoaded");
                loadedPages.add(pageIndex);
                List<BrowsableItem> browsableItems = MapToSongs(children);
                callback.onResult(browsableItems);
                // Normally we might want to stay subscribed to get changes in the music library
                // But here we just want to get a page and return
                mMediaBrowser.unsubscribe(mMediaId);
            }
        });
    }

    /**
     * Our MediaBrowser service will return MediaBrowserCompt.MediaItems
     * This helper method just turns these into our own BrowsableItem class
     * @param children the list of MediaItems
     * @return Equivalent list of BrowsableItems
     */
    private List<BrowsableItem> MapToSongs(List<MediaBrowserCompat.MediaItem> children) {
        // The code could possibly be simplified if we always display a list of media items
        // i.e. we remove the browsable item class
        List<BrowsableItem> browsableItems = new ArrayList<>();
        for (MediaBrowserCompat.MediaItem mediaItem : children) {
            int flags= mediaItem.getFlags();
            boolean isPlayable = (flags & MediaBrowserCompat.MediaItem.FLAG_PLAYABLE) == MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
            boolean isBrowsable = (flags & MediaBrowserCompat.MediaItem.FLAG_BROWSABLE) == MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
            BrowsableItem browsableItem = new BrowsableItem(mediaItem.getDescription().getTitle().toString(), mediaItem.getDescription().getSubtitle().toString(), mediaItem.getMediaId(), isPlayable,isBrowsable );
            browsableItems.add(browsableItem);
        }

        return browsableItems;
    }
}
