package com.example.android.uamp.model;

import android.support.v4.media.MediaBrowserCompat;
import com.example.android.uamp.ui.MediaBrowserProvider;

/**
 * Created by asbridged on 21/07/2017.
 */
public interface MediaChooserFragmentListener extends MediaBrowserProvider {
    void onBrowseMediaItemSelected(MediaBrowserCompat.MediaItem item);
    //        void onAddMediaToQueue(MediaBrowserCompat.MediaItem item);
    void onAddTrackToQueue(long trackId);
    void setToolbarTitle(CharSequence title);
    void onBrowseGroup(String type, long ID);

    void onChooseTrack();
    void onChooseAlbum();
    void onChooseArtist();

}
