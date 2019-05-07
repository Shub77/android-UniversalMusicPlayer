package com.example.android.uamp.model;

import android.support.v4.media.MediaBrowserCompat;
import com.example.android.uamp.ui.MediaBrowserProvider;

/**
 * Created by asbridged on 21/07/2017.
 * Callbacks from MediaChooserFragments in response to user actions in the fragments
 * Can be a response to a 'plus' button (add something to the queue)
 * Or
 * A response to browse a specific "group" from clicking [the background of] an album or artist
 * Or
 * A response to clicking a main menu button to browse ALL tracks/Artist/Albums
 */
public interface MediaChooserFragmentListener extends MediaBrowserProvider {
//    void onBrowseMediaItemSelected(MediaBrowserCompat.MediaItem item);

    // Response to "plus" button click
    void onAddAlbumToQueue(long id);
    void onAddArtistToQueue(long id);
    void onAddTrackToQueue(long trackId);

    void setToolbarTitle(int resourceStringId);

    // the response to a user clicking [the background of] an Artist or Album
    // This is a request to browse the tracks in that album or artist
    // Type is "Album" or "Artist"
    // Id identifies the media id of the album or artist
    void onBrowseGroup(String type, long ID);

    // Response to buttons in "main" menu
    // Will list ALL of the tracks/albums/artists
    void onChooseTrack();
    void onChooseAlbum();
    void onChooseArtist();
    void onHistory();

}
