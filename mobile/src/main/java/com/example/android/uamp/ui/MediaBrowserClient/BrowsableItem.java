package com.example.android.uamp.ui.MediaBrowserClient;

/**
 * Simple class to represent a "Browsable Item" for display in the Media Browser RecyclerList
 * It can be a track (playable, not browsable) mediaId = X (should change to TRACK_X)
 * An Artist (playable, browsable) mediaId = __ARTIST__X
 * An Album (playable, browsable) mediaId = __ALBUM_X__
 */
public class BrowsableItem {
    public String title;
    public String subtitle;
    public String mediaId;
    public boolean isPlayable;
    public boolean isBrowsable;

    public BrowsableItem(String title, String subtitle, String mediaId) {
        this.title = title;
        this.subtitle =subtitle;
        this.mediaId = mediaId;
        isPlayable = false;
        isBrowsable = false;
    }

    public BrowsableItem(String title, String subtitle, String mediaId, boolean isPlayable, boolean isBrowsable) {
        this.title = title;
        this.subtitle =subtitle;
        this.mediaId = mediaId;
        this.isPlayable = isPlayable;
        this.isBrowsable = isBrowsable;
    }
}
