package com.example.android.uamp.ui.MediaBrowserClient;

import android.arch.paging.DataSource;
import android.support.v4.media.MediaBrowserCompat;

public class SongsDataSourceFactory extends DataSource.Factory<Integer, BrowsableItem> {

    private final MediaBrowserCompat mediaBrowser;
    private final String mediaId;

    SongsDataSourceFactory(MediaBrowserCompat mediaBrowser, String mediaId) {
        this.mediaBrowser = mediaBrowser;
        this.mediaId = mediaId;
    }

    @Override
    public DataSource<Integer, BrowsableItem> create() {
        return new SongsDataSource(mediaBrowser, mediaId);
    }
}
