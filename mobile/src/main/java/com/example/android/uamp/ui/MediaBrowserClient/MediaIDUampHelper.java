package com.example.android.uamp.ui.MediaBrowserClient;


import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.TextUtils;

import com.example.android.uamp.utils.LogHelper;

import java.util.Arrays;

/**
 * Utility class to help on queue related tasks.
 */
public class MediaIDUampHelper {
    private static final String TAG = LogHelper.makeLogTag(com.example.android.uamp.ui.MediaBrowserClient.MediaIDUampHelper.class);
    // Media IDs used on browseable items of MediaBrowser
    public static final String MEDIA_ID_EMPTY_ROOT = "__EMPTY_ROOT__";
    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_ALL_SONGS = "__ALL_SONGS__";
    public static final String MEDIA_ID_HISTORY = "__HISTORY__";
    public static final String MEDIA_ID_ALBUMS = "__ALBUMS__";
    public static final String MEDIA_ID_ARTISTS = "__ARTISTS__";
    public static final String MEDIA_ID_ARTIST = "__ARTIST__";
    public static final String MEDIA_ID_ALBUM = "__ALBUM__";

    public static String getArtistIdFromMediaId(String ArtistMediaId) {
        return ArtistMediaId.substring(MediaIDUampHelper.MEDIA_ID_ARTIST.length());
    }

    public static String getAlbumIdFromMediaId(String AlbumMediaId) {
        return AlbumMediaId.substring(MediaIDUampHelper.MEDIA_ID_ALBUM.length());
    }



    /**
     * Determine if media item is playing (matches the currently playing media item).
     *
     * @param context for retrieving the {@link MediaControllerCompat}
     * @param mediaItem to compare to currently playing {@link MediaBrowserCompat.MediaItem}
     * @return boolean indicating whether media item matches currently playing media item
     */
    public static boolean isMediaItemPlaying(Context context,
                                             MediaBrowserCompat.MediaItem mediaItem) {
        // Media item is considered to be playing or paused based on the controller's current
        // media id
        /*
        MediaControllerCompat controller = ((FragmentActivity) context).getSupportMediaController();
        Cnahged for 27.1.1
        */
        MediaControllerCompat controller = MediaControllerCompat.getMediaController((Activity) context);
        if (controller != null && controller.getMetadata() != null) {
            String currentPlayingMediaId = controller.getMetadata().getDescription()
                    .getMediaId();
            String itemMusicId = mediaItem.getDescription().getMediaId();
            if (currentPlayingMediaId != null
                    && TextUtils.equals(currentPlayingMediaId, itemMusicId)) {
                return true;
            }
        }
        return false;
    }
}

