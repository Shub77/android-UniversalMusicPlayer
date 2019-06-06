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
public class CursorBasedPagedAlbumsMediaProvider {

    private final Cursor mCursor;

    public CursorBasedPagedAlbumsMediaProvider(Context applicationContext) {
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

    public int getMediaSize() {
        return mCursor.getCount();
    }

    public BrowsableItem getAlbumAtPosition(int position) {
        if (!mCursor.moveToPosition(position))
            return null;

        return createBrowsableItem(mCursor);
    }

    public List<BrowsableItem> getAlbumsAtRange(int startPosition, int endPosition) {
        List<BrowsableItem> browsableItems = new ArrayList<>();
        for (int position = startPosition; position < endPosition; ++position) {
            BrowsableItem browsableItem = getAlbumAtPosition(position);
            if (browsableItem != null)
                browsableItems.add(browsableItem);
        }

        return browsableItems;
    }

    private BrowsableItem createBrowsableItem(Cursor cursor) {
        Long albumID = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Albums._ID));

        BrowsableItem browsableItem = new BrowsableItem(
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)),
                cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)),
                "__ALBUM__" + Long.toString(albumID),
                true,
                true
        );
        return browsableItem;
    }

    private Uri getUri() {
        return MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    }

    private String[] getProjection() {
        return new String[]{
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS,
        MediaStore.Audio.Albums.ARTIST,
        MediaStore.Audio.Albums.ALBUM
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
