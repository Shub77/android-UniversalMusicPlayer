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
public class CursorBasedPagedArtistsMediaProvider {

    private final Cursor mCursor;

    public CursorBasedPagedArtistsMediaProvider(Context applicationContext) {
        mCursor = ContentResolverCompat.query(
                applicationContext.getContentResolver(),
                getUri(),
                getProjection(),
                getSelection(),
                getSelectionArgs(),
                getSortOrder(),
                null
        );
    }

    public List<BrowsableItem> getAllArtistsNonPaged() {
        List<BrowsableItem> browsableItems = new ArrayList<>();

        while (mCursor.moveToNext()) {
            browsableItems.add(createBrowsableItem(mCursor));
        }

        return browsableItems;
    }

    public int getMediaSize() {
        return mCursor.getCount();
    }

    public BrowsableItem getArtistAtPosition(int position) {
        if (!mCursor.moveToPosition(position))
            return null;

        return createBrowsableItem(mCursor);
    }

    public List<BrowsableItem> getArtistsAtRange(int startPosition, int endPosition) {
        List<BrowsableItem> browsableItems = new ArrayList<>();
        for (int position = startPosition; position < endPosition; ++position) {
            BrowsableItem browsableItem = getArtistAtPosition(position);
            if (browsableItem != null)
                browsableItems.add(browsableItem);
        }

        return browsableItems;
    }

    private BrowsableItem createBrowsableItem(Cursor cursor) {
        Long artistID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Artists._ID));

        BrowsableItem browsableItem = new BrowsableItem(
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)),
                "Tracks by " + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)), //cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST)),
                "__ARTIST__" + Long.toString(artistID),
                false, // TODO: true, SHOULD BE TRUE TO ALLOW ADDING A WHOLE ARTIST TO PQ
                true
        );
        return browsableItem;
    }

    private Uri getUri() {
        return MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    }

    private String[] getProjection() {
        return new String[]{
        MediaStore.Audio.Artists._ID,
        MediaStore.Audio.Artists.ARTIST,
        };
    }

    private String getSelection() {
        return null;
    }

    @Nullable
    private String[] getSelectionArgs() {
        return null;
    }

    @Nullable
    private String getSortOrder() {
        return MediaStore.Audio.Albums.ARTIST + " COLLATE LOCALIZED ASC";
    }
}
