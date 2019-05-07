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
package com.example.android.uamp.ui.MediaChooserFragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.constants.Constants;
import com.example.android.uamp.model.MediaChooserFragmentListener;
import com.example.android.uamp.settings.Settings;
import com.example.android.uamp.ui.dialogs.ClearableEditText;
import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that lists all the various browsable queues available
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * It uses a {@link MediaBrowserCompat} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get all the children.
 * All {@link MediaBrowserCompat.MediaItem}'s that can be browsed are shown in a ListView.
 */
public class MediaChooserHistoryFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(MediaChooserHistoryFragment.class);

    private MusicListAdapter mListAdapter;
    ArrayList<MediaBrowserCompat.MediaItem> mMediaItems = new ArrayList<>();

    private MediaChooserFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;
    private ClearableEditText etSearchText;


/*
    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current AlbumTitle and description and the PlaybackState.
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            if (metadata == null) {
                return;
            }
            LogHelper.d(TAG, "Received metadata change to media ",
                    metadata.getDescription().getMediaId());
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            LogHelper.d(TAG, "Received state change: ", state);
        }
    };
*/
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaChooserFragmentListener) activity;

    }

/*
    private final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    private final String _ID = MediaStore.Audio.Media._ID;
    private final String TITLE = MediaStore.Audio.Media.TITLE;
    private final String ARTIST = MediaStore.Audio.Media.ARTIST;
    private final String ARTIST_ID = MediaStore.Audio.Media.ARTIST_ID;
    private final String ALBUM = MediaStore.Audio.Albums.ALBUM;
    private final String DURATION_IN_MS = MediaStore.Audio.Media.DURATION;
    private final String TRACK_NO = MediaStore.Audio.Media.TRACK;
    private final String[] cursorColumns={_ID,TITLE, ARTIST, ARTIST_ID, ALBUM, DURATION_IN_MS, TRACK_NO};
    private final String orderby = TITLE + " COLLATE NOCASE";

    private FilterQueryProvider getAlbumsFilterQueryProvider() {
        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String partialValue = constraint.toString();
                String albumId = getSearchId();
                String selection;
                String[] selectionArgs;
                if (albumId == null) {
                    selectionArgs = new String[2];
                } else {
                    selectionArgs = new String[3];
                }
                selectionArgs[0] = Integer.toString(Settings.getMinDurationInSeconds(getActivity())*1000);
                selectionArgs[1]= "%" + partialValue + "%";

                selection = DURATION_IN_MS + " > ? AND " + TITLE +" LIKE ?";
                if (albumId != null) {
                    selection += " AND " +  MediaStore.Audio.Media.ALBUM_ID  + "=?"; ;
                    selectionArgs[2] = albumId;
                }

                ContentResolver cr = getActivity().getContentResolver();
                Cursor filteredCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
                return filteredCursor;
            }
        };
    }


    private FilterQueryProvider getArtistsFilterQueryProvider() {
        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String partialValue = constraint.toString();
                LogHelper.i(TAG, "filtering on ", partialValue);
                String albumId = getSearchId();
                String selection;
                String[] selectionArgs;
                if (albumId == null) {
                    selectionArgs = new String[2];
                } else {
                    selectionArgs = new String[3];
                }
                selectionArgs[0] = Integer.toString(Settings.getMinDurationInSeconds(getActivity())*1000);
                selectionArgs[1]= "%" + partialValue + "%";

                selection = DURATION_IN_MS + " > ? AND " + TITLE +" LIKE ?";

                if (albumId != null) {
                    selection += " AND " +  MediaStore.Audio.Media.ARTIST_ID  + "=?"; ;
                    selectionArgs[2] = albumId;
                }

                ContentResolver cr = getActivity().getContentResolver();
                Cursor filteredCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
                return filteredCursor;
            }
        };
    }
*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_media_chooser_tracks, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        getHistory();

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        mListAdapter = new MusicListAdapter(getActivity(), mMediaItems, mMediaFragmentListener);

        listView.setAdapter(mListAdapter);
        return rootView;
    }

    private void getHistory() {
        LogHelper.i(TAG, "getHistory");
        MediaBrowserCompat browser = mMediaFragmentListener.getMediaBrowser();
        boolean b = browser.isConnected();

        if (b) {
            LogHelper.i(TAG, "connected:");
        } else
        {
            LogHelper.i(TAG, "NOT connected");
        }
        String rootID =  browser.getRoot();
        LogHelper.i(TAG, "connected:", rootID, ":x");
        browser.subscribe("HISTORY", new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
                super.onChildrenLoaded(parentId, children);
                LogHelper.i(TAG, "onChildrenLoaded. Size=", children.size());
                mMediaItems.clear();
                for (MediaBrowserCompat.MediaItem item : children) {
                    mMediaItems.add(item);
                }
                mListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        updateTitle();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMediaFragmentListener = null;
    }

    public String getMediaId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_MEDIA_ID);
        }
        return null;
    }

    public void setMediaId(String mediaId) {
        LogHelper.i(TAG, "SET MEDIA ID!!!");
        Bundle args = new Bundle(1);
        args.putString(Constants.ARG_MEDIA_ID, mediaId);
        setArguments(args);
    }

    /**
     * Called by the activity in the case when we are  not  showing all tracks
     * e.g. showing tracks for a specific artist
     * The search will have a type (artist or album) and an id (the specific artist id or album id)
     * The parameters are stored in the fragment's bundle of parameters
     * @param searchType
     * @param id
     */
    public void setSearchParams(String searchType, String id) {
        LogHelper.i(TAG, "setSearchId, ",id);
        Bundle args = new Bundle(2);
        args.putString(Constants.ARG_ID, id);
        args.putString(Constants.ARG_SEARCH_TYPE, searchType);
        setArguments(args);
    }

    /**
     * Get the search ID (identifying an album or artist)
     * stored in the fragments's bundle of parameters
     * @return
     */
    private String getSearchId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_ID);
        }
        return null;
    }

    /**
     * Get the search type (identifying an album or artist)
     * stored in the fragments's bundle of parameters
     * @return
     */
    private String getSearchType() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_SEARCH_TYPE);
        }
        return Constants.SEARCH_TYPE_HISTORY;
    }

    private void updateTitle() {
        mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_tracks_by_history);

        /*
        String searchType = getSearchType();

        switch (searchType) {
            case Constants.SEARCH_TYPE_ALBUM:
                mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_tracks_on_album_title);
                break;
            case Constants.SEARCH_TYPE_ARTIST:
                mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_tracks_by_artist_title);
                break;
            case Constants.SEARCH_TYPE_SEARCH:
                mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_tracks_by_search);
                break;
                case Constants.SEARCH_TYPE_HISTORY:
                    mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_tracks_by_history);
                break;
            case Constants.SEARCH_TYPE_NONE:
                mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_alltracks_title);
                break;
        }
        */
    }

    // An adapter for showing the list of browsed MediaItem's
    private static class MusicListAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> {

        MediaChooserFragmentListener mListener;
        ArrayList<MediaBrowserCompat.MediaItem> mMediaItems;
        Activity mActivity;

        public MusicListAdapter(Activity context, ArrayList<MediaBrowserCompat.MediaItem> mediaItems,  MediaChooserFragmentListener listener) {
            super(context, R.layout.list_item_track, mediaItems);
            mListener = listener;
            mMediaItems = mediaItems;
            mActivity = context;
        }

        public static class ViewHolder{
            public TextView title;
            public TextView artist;
            public ImageButton btnAddToPlayqueue;
            public View view;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // reuse views
            if (rowView == null) {
                LayoutInflater inflater = mActivity.getLayoutInflater();
                rowView = inflater.inflate(R.layout.list_item_track, null);
                // configure view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.title = (TextView) rowView.findViewById(R.id.title);
                viewHolder.artist = (TextView) rowView.findViewById(R.id.artist);
                viewHolder.btnAddToPlayqueue = (ImageButton) rowView.findViewById(R.id.plus_eq);
                rowView.setTag(viewHolder);
            }

            // fill data
            ViewHolder holder = (ViewHolder) rowView.getTag();
            MediaBrowserCompat.MediaItem mediaItem = mMediaItems.get(position);
            holder.title.setText(mediaItem.getDescription().getTitle());
            holder.artist.setText(mediaItem.getDescription().getSubtitle());
            holder.artist.setText(mediaItem.getDescription().getSubtitle());
            long id = Long.parseLong(mediaItem.getDescription().getMediaId());
            holder.btnAddToPlayqueue.setOnClickListener(new btnAddToPlayqueueClickListener(0, id, mListener));
            return rowView;
        }
        class btnAddToPlayqueueClickListener implements View.OnClickListener {
            private int position;
            private long id;
            private MediaChooserFragmentListener listener;

            // constructor
            public btnAddToPlayqueueClickListener(int position, long id, MediaChooserFragmentListener listener) {
                this.position = position;
                this.id = id;
                this.listener = listener;
            }
            @Override
            public void onClick(View v) {
                // button clicked
                LogHelper.i(TAG, "add group pos ",position, "id=", id);
                listener.onAddTrackToQueue(id);
                // in this track adapter no need to pass the click up to the listview to handle.
                // we are at the lowest level, so the list item cannot be clicked.
            }
        }
/*
        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            String trackTitle = cursor.getString(1/*cursor.getColumnIndexOrThrow(nameColumn));
            String trackArtist = cursor.getString(2/*cursor.getColumnIndexOrThrow(nameColumn));
            long id = cursor.getLong(0);
            viewHolder.title.setText(trackTitle);
            viewHolder.artist.setText(trackArtist);
            viewHolder.btnAddToPlayqueue.setOnClickListener(new btnAddToPlayqueueClickListener(0, id, mListener));
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view =  LayoutInflater.from(context).inflate(R.layout.list_item_track, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.title = (TextView) view.findViewById(R.id.title);
            viewHolder.artist = (TextView) view.findViewById(R.id.artist);
            viewHolder.btnAddToPlayqueue = (ImageButton) view.findViewById(R.id.plus_eq);
            view.setTag(viewHolder);
            return view;
        }


        */

    }
}
