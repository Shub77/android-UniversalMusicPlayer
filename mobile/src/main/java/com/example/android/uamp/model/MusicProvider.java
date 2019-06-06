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

package com.example.android.uamp.model;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import com.example.android.uamp.settings.Settings;
import com.example.android.uamp.utils.LogHelper;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;



/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);


    private Context context;

    public MusicProvider(Context context) {
        this.context = context;

    }

    /**
     * Get a list of artists
     *
     * @return artists
     */
    public ArrayList<MediaBrowserCompat.MediaItem> getArtistMediaItems() {
        final Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Artists._ID;
        final String NAME_COLUMN = MediaStore.Audio.Artists.ARTIST;
        final String[] cursorColumns={_ID, NAME_COLUMN };
        final String orderby = NAME_COLUMN + " COLLATE NOCASE";
        final String where = null;

        ContentResolver cr = context.getContentResolver();
        Cursor artistsCursor =  cr.query(uri, cursorColumns, where, null, orderby);
        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        MediaBrowserCompat.MediaItem mediaItem;
        try {
            while (artistsCursor.moveToNext()) {
                String id = artistsCursor.getString(0);
                String name = artistsCursor.getString(1);
                MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                        .setTitle(name)
                        .setSubtitle("Songs by "+name)
                        .setMediaId("__ARTIST__"+id)
                        .build();

                int flags = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE | MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;
                mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, flags);
                mediaItems.add(mediaItem);
            }
        } finally {
            artistsCursor.close();
        }

        return mediaItems;

    }

    /**
     * Get an iterator over the list of albums
     *
     * @return albums
     */
    public ArrayList<MediaBrowserCompat.MediaItem> getAlbumMediaItems() {
        final Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Albums._ID;
        final String ARTIST_COLUMN = MediaStore.Audio.Albums.ARTIST;
        final String NAME_COLUMN = MediaStore.Audio.Albums.ALBUM;

        final String[] cursorColumns={_ID, NAME_COLUMN, ARTIST_COLUMN};
        final String orderby = NAME_COLUMN + " COLLATE NOCASE";

        final String where = null;
        ContentResolver cr = context.getContentResolver();
        Cursor albumsCursor =  cr.query(uri, cursorColumns, where, null, orderby);

        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        MediaBrowserCompat.MediaItem mediaItem;
        try {
            while (albumsCursor.moveToNext()) {
                String id = albumsCursor.getString(0);
                String name = albumsCursor.getString(1);
                String artist = albumsCursor.getString(2);
                MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                        .setTitle(name)
                        .setSubtitle(artist)
                        .setMediaId("__ALBUM__"+id)
                        .build();

                int flags = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE | MediaBrowserCompat.MediaItem.FLAG_BROWSABLE;

                mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, flags);
                mediaItems.add(mediaItem);
            }
        } finally {
            albumsCursor.close();
        }

        return mediaItems;
    }


    /**
     * Get a random song from ALL songs on the phone
     * More efficient version of the code (does not load all songs into memory)
     * @return
     */
    public MediaMetadataCompat getRandomSongFromAllSongsOnDevice() {
        long randomSongID = 0;

        // First select ONE song ID (randomly)
        // Uses random ordering in DB (not efficient, but does not use memory)
        ContentResolver musicResolver = context.getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,     // context id/ uri id of the file
        };

        Cursor musicCursor = musicResolver.query(musicUri, projection, null /*selection*/, null /*selectionArgs*/, "RANDOM() LIMIT 1");
        if(musicCursor!=null && musicCursor.moveToFirst()){
            int idColumn = musicCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            randomSongID = musicCursor.getLong(idColumn);
            musicCursor.close();
        }
        // Now get the song by the randomly chosen ID
        MediaMetadataCompat song = getMusic(Long.toString(randomSongID));
        return song;
    }

    /**
     * Get music tracks of the given genre
     *
     */
    // TODO: this is actually albums
    public Iterable<MediaMetadataCompat> getMusicsByGenreById(String id) {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Albums.ARTIST;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String ALBUM_ID = MediaStore.Audio.Albums.ALBUM_ID;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID, TITLE, ARTIST, ALBUM, ALBUM_ID, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;
        if (id != null && !id.isEmpty()) {
            selection = MediaStore.Audio.Media.ALBUM_ID + "=?";
            selectionArgs = new String [1];
            selectionArgs[0] = id;
        }
        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();

        try {
            while (tracksCursor.moveToNext()) {
                String trackid = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String album = tracksCursor.getString(3);
                String albumId = tracksCursor.getString(4);
                Long durationInMs = tracksCursor.getLong(5);
                Long trackNo = tracksCursor.getLong(6);

                tracks.add(buildMetadataFromProperties(trackid,title,artist, album, durationInMs, trackNo));
            }
        } finally {
            tracksCursor.close();
        }

        return tracks;
    }
    /**
     * Get music tracks of the given genre
     *
     */
    // TODO: this is actually albums
    public Iterable<MediaMetadataCompat> getMusicsByGenre(String genreName) {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Albums.ARTIST;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;
        if (genreName != null && !genreName.isEmpty()) {
            selection = MediaStore.Audio.Media.ALBUM + "=?";
            selectionArgs = new String [1];
            selectionArgs[0] = genreName;
        }
        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();

        try {
            while (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String album = tracksCursor.getString(3);
                Long durationInMs = tracksCursor.getLong(4);
                Long trackNo = tracksCursor.getLong(5);

                tracks.add(buildMetadataFromProperties(id,title,artist, album, durationInMs, trackNo));
            }
        } finally {
            tracksCursor.close();
        }

        return tracks;
    }

    public ArrayList<MediaBrowserCompat.MediaItem> getMediaItemsByArtist(String id) {
        ArrayList<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        Iterable<MediaMetadataCompat> metadatas;
        metadatas = getMusicsByArtist(id);
        MediaBrowserCompat.MediaItem mediaItem;
        Iterator itr = metadatas.iterator();
        while (itr.hasNext()) {
            MediaMetadataCompat metadata = (MediaMetadataCompat)itr.next();
            mediaItem = new MediaBrowserCompat.MediaItem(metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE );
            result.add(mediaItem);
        }
        return result;
    }

    public ArrayList<MediaBrowserCompat.MediaItem> getMediaItemsByAlbum(String id) {
        ArrayList<MediaBrowserCompat.MediaItem> result = new ArrayList<>();
        Iterable<MediaMetadataCompat> metadatas;
        metadatas = getMusicsByAlbum(id);
        MediaBrowserCompat.MediaItem mediaItem;
        Iterator itr = metadatas.iterator();
        while (itr.hasNext()) {
            MediaMetadataCompat metadata = (MediaMetadataCompat)itr.next();
            mediaItem = new MediaBrowserCompat.MediaItem(metadata.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE );
            result.add(mediaItem);
        }
        return result;
    }

    /**
     * Get music tracks of the given artist
     * By the supplied artist ID, not the artist name
     *
     */
    public Iterable<MediaMetadataCompat> getMusicsByArtist(String artistId) {
        LogHelper.i(TAG, "getMusicsByArtist" , artistId);
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Media.ARTIST;
        final String ARTIST_ID = MediaStore.Audio.Media.ARTIST_ID;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ARTIST_ID, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = ARTIST_ID + "=? AND " + DURATION_IN_MS + " > ?";;
        String[] selectionArgs = new String [2];
        selectionArgs[0] = artistId;
        selectionArgs[1] = Integer.toString(Settings.getMinDurationInSeconds(context));

        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();

        try {
            while (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String artist_id = tracksCursor.getString(3);
                String album = tracksCursor.getString(4);
                Long durationInMs = tracksCursor.getLong(5);
                Long trackNo = tracksCursor.getLong(6);
                LogHelper.i(TAG, "Track ", id, " artist id=",artist_id );
                tracks.add(buildMetadataFromProperties(id,title,artist, album, durationInMs, trackNo));
            }
        } finally {
            tracksCursor.close();
        }

        return tracks;
    }

    /**
     * Get all songs
     */
    public ArrayList<MediaBrowserCompat.MediaItem> getAllSongs() {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Media.ARTIST;
        final String ARTIST_ID = MediaStore.Audio.Media.ARTIST_ID;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ARTIST_ID, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;

        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        ArrayList<MediaBrowserCompat.MediaItem> tracks = new ArrayList<>();
        MediaBrowserCompat.MediaItem mediaItem;

        try {
            while (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String artist_id = tracksCursor.getString(3);
                String album = tracksCursor.getString(4);
                Long durationInMs = tracksCursor.getLong(5);
                Long trackNo = tracksCursor.getLong(6);
                LogHelper.i(TAG, "Track ", id, " artist id=",artist_id );
                MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat.Builder()
                        .setTitle(title)
                        .setSubtitle(artist)
                        .setMediaId(id)
                        .build();

                mediaItem = new MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                tracks.add(mediaItem);
            }
        } finally {
            tracksCursor.close();
        }

        return tracks;
    }

    private MediaMetadataCompat buildMetadataFromProperties(String id, String title, String artist, String album, Long durationInMs, Long trackNumber) {
        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        //noinspection ResourceType

        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
        LogHelper.i(TAG, "build Metadata from id=", id, " trackUri", trackUri.toString(), " artist=", artist);
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                //.putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, "source")
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationInMs) // in ms
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "countryjazzfusion")
                //.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, trackUri.toString())
                .build();
    }

    /**
     * Get music tracks of the given album
     *
     */
    public Iterable<MediaMetadataCompat> getMusicsByAlbum(String albumId) {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Albums.ARTIST;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String ALBUM_ID = MediaStore.Audio.Albums.ALBUM_ID;

        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = ALBUM_ID + "=? AND " + DURATION_IN_MS + " > ?";;
        String[] selectionArgs = new String [2];
        selectionArgs[0] = albumId;
        selectionArgs[1] = Integer.toString(Settings.getMinDurationInSeconds(context));

        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();

        try {
            while (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String album = tracksCursor.getString(3);
                Long durationInMs = tracksCursor.getLong(4);
                Long trackNo = tracksCursor.getLong(5);

                tracks.add(buildMetadataFromProperties(id,title,artist, album, durationInMs, trackNo));
            }
        } finally {
            tracksCursor.close();
        }

        return tracks;

    }

    /**
     * Very basic implementation of a search that filter music tracks with AlbumTitle containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicBySongTitle(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_TITLE, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with album containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByAlbum(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ALBUM, query);
    }

    /**
     * Very basic implementation of a search that filter music tracks with artist containing
     * the given query.
     *
     */
    public Iterable<MediaMetadataCompat> searchMusicByArtist(String query) {
        return searchMusic(MediaMetadataCompat.METADATA_KEY_ARTIST, query);
    }

    Iterable<MediaMetadataCompat> searchMusic(String metadataField, String query) {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Media.ARTIST;
        final String ARTIST_ID = MediaStore.Audio.Media.ARTIST_ID;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ARTIST_ID, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;
        if (query != null && !query.isEmpty()) {
            selection = MediaStore.Audio.Media.TITLE + " LIKE ?";
            selectionArgs = new String [1];
            selectionArgs[0] = "%" + query + "%";
        }

        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();

        try {
            while (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String artist_id = tracksCursor.getString(3);
                String album = tracksCursor.getString(4);
                Long durationInMs = tracksCursor.getLong(5);
                Long trackNo = tracksCursor.getLong(6);
                LogHelper.i(TAG, "Track ", id, " artist id=",artist_id );
                tracks.add(buildMetadataFromProperties(id,title,artist, album, durationInMs, trackNo));
            }
        } finally {
            tracksCursor.close();
        }

        return tracks;
    }


    /**
     * Return the MediaMetadataCompat for the given musicID.
     *
     * @param musicId The unique, non-hierarchical music ID.
     */
    //TODO: rename to 'getTrackById', 'getMusic' is a bit vague
    public MediaMetadataCompat getMusic(String musicId) {
        LogHelper.i(TAG, "getTrackById=", musicId);
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Media._ID;
        final String TITLE = MediaStore.Audio.Media.TITLE;
        final String ARTIST = MediaStore.Audio.Albums.ARTIST;
        final String ALBUM = MediaStore.Audio.Albums.ALBUM;
        final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
        final String TRACK_NO = MediaStore.Audio.Media.TRACK;

        final String[] cursorColumns={_ID,TITLE, ARTIST, ALBUM, DURATION_IN_MS, TRACK_NO};
        final String orderby = TITLE + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;
        if (musicId != null && !musicId.isEmpty()) {
            selection = MediaStore.Audio.Media._ID + "=?";
            selectionArgs = new String [1];
            selectionArgs[0] = musicId;
        }
        ContentResolver cr = context.getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
        MediaMetadataCompat track = null;

        try {
            if (tracksCursor.moveToNext()) {
                String id = tracksCursor.getString(0);
                String title= tracksCursor.getString(1);
                String artist = tracksCursor.getString(2);
                String album = tracksCursor.getString(3);
                Long durationInMs = tracksCursor.getLong(4);
                Long trackNo = tracksCursor.getLong(5);

                track = buildMetadataFromProperties(id,title,artist, album, durationInMs, trackNo);
            }
        } finally {
            tracksCursor.close();
        }
        return track;
    }

    public synchronized void updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
        //TODO: implement updateMusicArt
        return;
        /*
        MediaMetadataCompat metadata = getMusic(musicId);
        metadata = new MediaMetadataCompat.Builder(metadata)

                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
                // example, on the lockscreen background when the media session is active.
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)

                // set small version of the album art in the DISPLAY_ICON. This is used on
                // the MediaDescription and thus it should be small to be serialized if
                // necessary
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)

                .build();

        MutableMediaMetadata mutableMetadata = mMusicListById.get(musicId);
        if (mutableMetadata == null) {
            throw new IllegalStateException("Unexpected error: Inconsistent data structures in " +
                    "MusicProvider");
        }

        mutableMetadata.metadata = metadata;
        */
    }

}
