package com.example.android.uamp.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContentResolverCompat;

import com.example.android.uamp.ui.MediaBrowserClient.BrowsableItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by onLoadChildren to get paginated content from the database
 * Taken from https://proandroiddev.com/how-to-paginate-mediabrowserservice-e4fdc902ff2
 */
public class CursorBasedPagedArtistByIdMediaProvider {

    private final Cursor mCursor;

    public CursorBasedPagedArtistByIdMediaProvider(Context applicationContext, String mediaId) {
        mCursor = ContentResolverCompat.query(
                applicationContext.getContentResolver(),
                getUri(),
                getProjection(),
                getSelection(),
                getSelectionArgs(mediaId),
                getSortOrder(),
                null
        );
    }

    public int getMediaSize() {
        return mCursor.getCount();
    }

    public BrowsableItem getSongAtPosition(int position) {
        if (!mCursor.moveToPosition(position))
            return null;

        return createSong(mCursor);
    }

    public List<BrowsableItem> getTracksByArtistByIdPage(int startPosition, int endPosition) {
        List<BrowsableItem> browsableItems = new ArrayList<>();
        for (int position = startPosition; position < endPosition; ++position) {
            BrowsableItem browsableItem = getSongAtPosition(position);
            if (browsableItem != null)
                browsableItems.add(browsableItem);
        }

        return browsableItems;
    }

    private BrowsableItem createSong(Cursor cursor) {
        BrowsableItem browsableItem = new BrowsableItem(
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                Long.toString(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID))),
                true,
                false
        );
        return browsableItem;
    }

    private Uri getUri() {
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }

    private String[] getProjection() {
        return new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
        };
    }

    private String getSelection() {
        return "("
                + "(" + MediaStore.Audio.Media.IS_MUSIC + " !=0 )"
                + "AND (" + MediaStore.Audio.Media.IS_ALARM + " ==0 )"
                + "AND (" + MediaStore.Audio.Media.IS_NOTIFICATION + " ==0 )"
                + "AND (" + MediaStore.Audio.Media.IS_PODCAST + " ==0 )"
                + "AND (" + MediaStore.Audio.Media.IS_RINGTONE + " ==0 )"
                + "AND (" + MediaStore.Audio.Media.ARTIST_ID + "==?)"
                + ")";
    }

    @Nullable
    private String[] getSelectionArgs(String artistId) {
        return new String[] {artistId};
    }

    @Nullable
    private String getSortOrder() {
        return MediaStore.Audio.Media.ARTIST + " COLLATE LOCALIZED ASC";
    }
}
