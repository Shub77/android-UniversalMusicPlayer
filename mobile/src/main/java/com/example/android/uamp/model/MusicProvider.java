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
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import android.widget.Toast;
import com.example.android.uamp.R;
import com.example.android.uamp.settings.Settings;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.example.android.uamp.utils.MediaIDHelper.*;

/**
 * Simple data provider for music tracks. The actual metadata source is delegated to a
 * MusicProviderSource defined by a constructor argument of this class.
 */
public class MusicProvider {

    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);

    // private MusicProviderSource mSource;

    // Categorized caches for music track data:
    /* Don't  cache all songs
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByGenre;
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByArtist;
    private ConcurrentMap<String, List<MediaMetadataCompat>> mMusicListByAlbum;
    private final ConcurrentMap<String, MutableMediaMetadata> mMusicListById;
    */

    // Although we could cache lists of artists/albums ???

    private final Set<String> mFavoriteTracks;
    private Context context;
    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider(Context context) {
        //this(new StorageMusicSource(context) /* RemoteJSONSource()*/);
        mFavoriteTracks = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        this.context = context;

    }

    //TODO: this is actually ALBUMS
    public Iterable<Genre> getGenreObjects() {

        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }

        // original imlementation : return mMusicListByGenre.keySet();
        final Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Albums._ID;
        final String NUM_ITEMS_COLUMN = MediaStore.Audio.Albums.NUMBER_OF_SONGS;

        final String NAME_COLUMN = MediaStore.Audio.Albums.ALBUM;

        final String[] cursorColumns={_ID, NUM_ITEMS_COLUMN, NAME_COLUMN};
        final String orderby = NAME_COLUMN + " COLLATE NOCASE";

        final String where = null;
        ContentResolver cr = context.getContentResolver();
        Cursor genresCursor =  cr.query(uri, cursorColumns, where, null, orderby);
        ArrayList<Genre> genres = new ArrayList<>();
        if (genresCursor != null) {
            Toast.makeText(context, "genres size=" + genresCursor.getCount(), Toast.LENGTH_LONG).show();
        }
        Genre genre;
        try {
            while (genresCursor.moveToNext()) {
                String id = genresCursor.getString(0);
                String numItems= genresCursor.getString(1);
                String name = genresCursor.getString(2);
                genre = new Genre();
                genre.id = id;
                genre.name = name;
                genres.add(genre);
            }
        } finally {
            genresCursor.close();
        }

        return genres;

    }

    /**
     * Get an iterator over the list of genres
     *
     * @return genres
     */
    //TODO: this is actually ALBUMS
    public Iterable<String> getGenres() {

        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }

        // original imlementation : return mMusicListByGenre.keySet();
        final Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
         final String _ID = MediaStore.Audio.Albums._ID;
        final String NUM_ITEMS_COLUMN = MediaStore.Audio.Albums.NUMBER_OF_SONGS;

        final String NAME_COLUMN = MediaStore.Audio.Albums.ALBUM;

        final String[] cursorColumns={_ID, NUM_ITEMS_COLUMN, NAME_COLUMN};
        final String orderby = NAME_COLUMN + " COLLATE NOCASE";

        final String where = null;
        ContentResolver cr = context.getContentResolver();
        Cursor genresCursor =  cr.query(uri, cursorColumns, where, null, orderby);
        ArrayList<String> genres = new ArrayList<>();
        if (genresCursor == null) {
            genres.add("Cursorisnull");
        } else {
            Toast.makeText(context, "genres size=" + genresCursor.getCount(), Toast.LENGTH_LONG).show();
        }

        try {
            while (genresCursor.moveToNext()) {
                String id = genresCursor.getString(0);
                String numItems= genresCursor.getString(1);
                String name = genresCursor.getString(2);
                genres.add(name);
            }
        } finally {
            genresCursor.close();
        }

        return genres;

    }

    /**
     * Get an iterator over the list of artists
     *
     * @return artists
     */
    public Iterable<Artist> getArtistObjects() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        //return mMusicListByArtist.keySet();
        final Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Artists._ID;

        final String NAME_COLUMN = MediaStore.Audio.Artists.ARTIST;

        final String[] cursorColumns={_ID, NAME_COLUMN };
        final String orderby = NAME_COLUMN + " COLLATE NOCASE";

        final String where = null;
        ContentResolver cr = context.getContentResolver();
        Cursor artistsCursor =  cr.query(uri, cursorColumns, where, null, orderby);

        ArrayList<Artist> artists = new ArrayList<>();
        Artist artist;
        try {
            while (artistsCursor.moveToNext()) {
                String id = artistsCursor.getString(0);
                String name = artistsCursor.getString(1);
                artist = new Artist(id, name);
                artists.add(artist);
            }
        } finally {
            artistsCursor.close();
        }

        return artists;

    }

    /**
     * Get an iterator over the list of albums
     *
     * @return albums
     */
    public Iterable<Album> getAlbumObjects() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        //return mMusicListByAlbum.keySet();

        final Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Albums._ID;
        final String NUM_ITEMS_COLUMN = MediaStore.Audio.Albums.NUMBER_OF_SONGS;
        final String ARTIST_COLUMN = MediaStore.Audio.Albums.ARTIST;
        final String NAME_COLUMN = MediaStore.Audio.Albums.ALBUM;

        final String[] cursorColumns={_ID, NUM_ITEMS_COLUMN, NAME_COLUMN, ARTIST_COLUMN};
        final String orderby = NAME_COLUMN + " COLLATE NOCASE";

        final String where = null;
        ContentResolver cr = context.getContentResolver();
        Cursor albumsCursor =  cr.query(uri, cursorColumns, where, null, orderby);

        ArrayList<Album> albums = new ArrayList<>();
        Album album;
        try {
            while (albumsCursor.moveToNext()) {
                album = new Album();
                album.id = albumsCursor.getString(0);
                String numItems= albumsCursor.getString(1);
                album.name = albumsCursor.getString(2);
                album.artist = albumsCursor.getString(3);
                albums.add(album);
            }
        } finally {
            albumsCursor.close();
        }

        return albums;
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
     * Get an iterator over a shuffled collection of all songs
     */
    public Iterable<MediaMetadataCompat> getShuffledMusic() {
        LogHelper.i(TAG, "DO NOT USE THIS getShuffledMusic");
        if (mCurrentState != State.INITIALIZED) {
            //LogHelper.i(TAG, "not initialized");
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> shuffled = new ArrayList<>(4);
/*
        for (MutableMediaMetadata mutableMetadata: mMusicListById.values()) {
            shuffled.add(mutableMetadata.metadata);
        }
*/
        Collections.shuffle(shuffled);
        //LogHelper.i(TAG, "shuffled size = ", shuffled.size());
        return shuffled;
    }
    /**
     * Get music tracks of the given genre
     *
     */
    // TODO: this is actually albums
    public Iterable<MediaMetadataCompat> getMusicsByGenreById(String id) {
        if (mCurrentState != State.INITIALIZED /*|| !mMusicListByGenre.containsKey(genre)*/) {
            return Collections.emptyList();
        }
        //return mMusicListByGenre.get(genre);

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
        if (mCurrentState != State.INITIALIZED /*|| !mMusicListByGenre.containsKey(genre)*/) {
            return Collections.emptyList();
        }
        //return mMusicListByGenre.get(genre);

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

    /**
     * Get music tracks of the given artist
     * By the supplied artist ID, not the artist name
     *
     */
    public Iterable<MediaMetadataCompat> getMusicsByArtist(String artistId) {
        LogHelper.i(TAG, "getMusicsByArtist" , artistId);
        if (mCurrentState != State.INITIALIZED /*|| !mMusicListByArtist.containsKey(artist)*/) {
            return Collections.emptyList();
        }
        // return mMusicListByArtist.get(artist);

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
     * Get music tracks of the given artist
     * By the supplied artist ID, not the artist name
     *
     */
    public Iterable<MediaMetadataCompat> getAllSongs() {
        if (mCurrentState != State.INITIALIZED /*|| !mMusicListByArtist.containsKey(artist)*/) {
            return Collections.emptyList();
        }
        // return mMusicListByArtist.get(artist);

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

    private MediaMetadataCompat buildMetadataFromProperties(String id, String title, String artist, String album, Long durationInMs, Long trackNumber) {
        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        //noinspection ResourceType

        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
        LogHelper.i(TAG, "id=", id, "trackUri", trackUri.toString(), " artist=", artist);
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
        if (mCurrentState != State.INITIALIZED /* || !mMusicListByAlbum.containsKey(album)*/) {
            return Collections.emptyList();
        }
        //return mMusicListByAlbum.get(album);

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
        if (mCurrentState != State.INITIALIZED /*|| !mMusicListByArtist.containsKey(artist)*/) {
            return Collections.emptyList();
        }
        // return mMusicListByArtist.get(artist);

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
        //return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId).metadata : null;

        if (mCurrentState != State.INITIALIZED /* || !mMusicListByAlbum.containsKey(album)*/) {
            return null;
        }
        //return mMusicListByAlbum.get(album);

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

    public void setFavorite(String musicId, boolean favorite) {
        if (favorite) {
            mFavoriteTracks.add(musicId);
        } else {
            mFavoriteTracks.remove(musicId);
        }
    }

    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    public boolean isFavorite(String musicId) {
        return mFavoriteTracks.contains(musicId);
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by genre.
     */
    //TODO: remove all calls to this. We don't need to initialize anything
    public void retrieveMediaAsync(final Callback callback) {
        LogHelper.d(TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            if (callback != null) {
                // Nothing to do, execute callback immediately
                callback.onMusicCatalogReady(true);
            }
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                //retrieveMedia();
                mCurrentState = State.INITIALIZED;
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onMusicCatalogReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }

    /*
    private synchronized void buildListsByGenre() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByGenre = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String genre = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE);
            List<MediaMetadataCompat> list = newMusicListByGenre.get(genre);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByGenre.put(genre, list);
            }
            list.add(m.metadata);
        }
        mMusicListByGenre = newMusicListByGenre;
    }

    private synchronized void buildListsByArtist() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByArtist = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String artist = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            List<MediaMetadataCompat> list = newMusicListByArtist.get(artist);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByArtist.put(artist, list);
            }
            list.add(m.metadata);
        }
        mMusicListByArtist = newMusicListByArtist;
    }

    private synchronized void buildListsByAlbum() {
        ConcurrentMap<String, List<MediaMetadataCompat>> newMusicListByAlbum = new ConcurrentHashMap<>();

        for (MutableMediaMetadata m : mMusicListById.values()) {
            String album = m.metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
            List<MediaMetadataCompat> list = newMusicListByAlbum.get(album);
            if (list == null) {
                list = new ArrayList<>();
                newMusicListByAlbum.put(album, list);
            }
            list.add(m.metadata);
        }
        mMusicListByAlbum = newMusicListByAlbum;
    }

    private synchronized void retrieveMedia() {
        try {
            if (mCurrentState == State.NON_INITIALIZED) {
                mCurrentState = State.INITIALIZING;

                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
                while (tracks.hasNext()) {
                    MediaMetadataCompat item = tracks.next();
                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mMusicListById.put(musicId, new MutableMediaMetadata(musicId, item));
                }
                buildListsByGenre();
                buildListsByArtist();
                buildListsByAlbum();
                mCurrentState = State.INITIALIZED;
            }
        } finally {
            if (mCurrentState != State.INITIALIZED) {
                // Something bad happened, so we reset state to NON_INITIALIZED to allow
                // retries (eg if the network connection is temporary unavailable)
                mCurrentState = State.NON_INITIALIZED;
            }
        }
    }
*/

    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        //LogHelper.i(TAG, "getChildren, mediaId=", mediaId );
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (!MediaIDHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MEDIA_ID_ROOT.equals(mediaId)) {
            //LogHelper.i(TAG, "At the root...");
            mediaItems.add(createBrowsableMediaItemGenreForRoot(resources));
            mediaItems.add(createBrowsableMediaItemArtistForRoot(resources));
            mediaItems.add(createBrowsableMediaItemAlbumForRoot(resources));
            mediaItems.add(createBrowsableMediaItemSongForRoot(resources));
        } else if (MEDIA_ID_MUSICS_BY_GENRE.equals(mediaId)) {
            //LogHelper.i(TAG, "The Genre item, add all Genres ...");
            for (Genre genre : getGenreObjects()) {
                mediaItems.add(createBrowsableMediaItemForGenreWithId(genre.name, genre.id, resources));
            }
        } else if (MEDIA_ID_MUSICS_BY_ARTIST.equals(mediaId)) {
            //LogHelper.i(TAG, "The Genre item, add all Artists ...");
            for (Artist artist : getArtistObjects()) {
                mediaItems.add(createBrowsableMediaItemForArtist(artist, resources));
            }
        } else if (MEDIA_ID_MUSICS_BY_ALBUM.equals(mediaId)) {
            //LogHelper.i(TAG, "The Genre item, add all albums...");
            for (Album album : getAlbumObjects()) {
                mediaItems.add(createBrowsableMediaItemForAlbum(album, resources));
            }
        } else if (MEDIA_ID_MUSICS_BY_SONG.equals(mediaId)) {
            LogHelper.i(TAG, "The Song item ...");
            for (MediaMetadataCompat metadata : getAllSongs()) {
                mediaItems.add(createMediaItem(metadata, MediaMetadataCompat.METADATA_KEY_ALBUM, MEDIA_ID_MUSICS_BY_ALBUM));
            }
        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_GENRE)) {
            String genre = MediaIDHelper.getHierarchy(mediaId)[1];
            Toast.makeText(context, "genre = "+genre, Toast.LENGTH_LONG).show();
            //LogHelper.i(TAG, "Genre ", genre, " add all the songs");
            for (MediaMetadataCompat metadata : getMusicsByGenreById(genre)) {
                mediaItems.add(createMediaItem(metadata, MediaMetadataCompat.METADATA_KEY_GENRE, MEDIA_ID_MUSICS_BY_GENRE));
            }
        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_ARTIST)) {
            String artist = MediaIDHelper.getHierarchy(mediaId)[1];
            //LogHelper.i(TAG, "Artist ", artist, " add all the songs");
            for (MediaMetadataCompat metadata : getMusicsByArtist(artist)) {
                mediaItems.add(createMediaItem(metadata, MediaMetadataCompat.METADATA_KEY_ARTIST, MEDIA_ID_MUSICS_BY_ARTIST));
            }
        } else if (mediaId.startsWith(MEDIA_ID_MUSICS_BY_ALBUM)) {
            String album = MediaIDHelper.getHierarchy(mediaId)[1];
            //LogHelper.i(TAG, "Album ", album, " add all the songs");
            for (MediaMetadataCompat metadata : getMusicsByAlbum(album)) {
                mediaItems.add(createMediaItem(metadata, MediaMetadataCompat.METADATA_KEY_ALBUM, MEDIA_ID_MUSICS_BY_ALBUM));
            }
        } else {
            LogHelper.w(TAG, "Skipping unmatched mediaId: ", mediaId);
        }
        return mediaItems;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemGenreForRoot(Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_BY_GENRE)
                .setTitle(resources.getString(R.string.browse_genres))
                .setSubtitle(resources.getString(R.string.browse_genre_subtitle))
                .setIconUri(Uri.parse("android.resource://" +
                        "com.example.android.uamp/drawable/ic_by_genre"))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemArtistForRoot(Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_BY_ARTIST)
                .setTitle(resources.getString(R.string.browse_artists))
                .setSubtitle(resources.getString(R.string.browse_artist_subtitle))
                .setIconUri(Uri.parse("android.resource://" +
                        "com.example.android.uamp/drawable/ic_by_genre"))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemSongForRoot(Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_BY_SONG)
                .setTitle(resources.getString(R.string.browse_songs))
                .setSubtitle(resources.getString(R.string.browse_song_subtitle))
                .setIconUri(Uri.parse("android.resource://" +
                        "com.example.android.uamp/drawable/ic_by_genre"))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemAlbumForRoot(Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MEDIA_ID_MUSICS_BY_ALBUM)
                .setTitle(resources.getString(R.string.browse_albums))
                .setSubtitle(resources.getString(R.string.browse_album_subtitle))
                .setIconUri(Uri.parse("android.resource://" +
                        "com.example.android.uamp/drawable/ic_by_genre"))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForGenre(String genre,
                                                                          Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, "NOTREALTEMPGENRE"/*genre*/))
                .setTitle(genre)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, genre))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForGenreWithId(String genre, String id,
                                                                          Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_GENRE, id/*genre*/))
                .setTitle(genre)
                .setSubtitle(resources.getString(
                        R.string.browse_musics_by_genre_subtitle, genre))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }


    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForArtist(Artist artist,
                                                                           Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_ARTIST, artist.id))
                .setTitle(artist.name)
                .setSubtitle(resources.getString(R.string.browse_musics_by_artist_subtitle, artist.name))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }


    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForAlbum(Album album,
                                                                           Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(createMediaID(null, MEDIA_ID_MUSICS_BY_ALBUM, album.id))
                .setTitle(album.name)
                .setSubtitle(resources.getString(R.string.browse_musics_by_album_subtitle, album.artist))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata, String metadataKey, String category) {
        // Since mediaMetadata fields are immutable, we need to create a copy, so we
        // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
        // when we get a onPlayFromMusicID call, so we can create the proper queue based
        // on where the music was selected from (by artist, by genre, random, etc)
        String type = metadata.getString(metadataKey /*MediaMetadataCompat.METADATA_KEY_GENRE*/);
        String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                metadata.getDescription().getMediaId(),
                category,
                type);
        MediaMetadataCompat copy = new MediaMetadataCompat.Builder(metadata)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                .build();
        return new MediaBrowserCompat.MediaItem(copy.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);

    }

}
