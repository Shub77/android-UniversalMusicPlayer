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
import android.content.*;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.android.uamp.R;
import com.example.android.uamp.constants.Constants;
import com.example.android.uamp.model.MediaChooserFragmentListener;
import com.example.android.uamp.settings.Settings;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.NetworkHelper;

import java.util.List;

/**
 * A Fragment that lists all the various browsable queues available
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * It uses a {@link MediaBrowserCompat} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get all the children.
 * All {@link MediaBrowserCompat.MediaItem}'s that can be browsed are shown in a ListView.
 */
public class MediaChooserTracksFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(MediaChooserTracksFragment.class);

    private MusicCursorAdapter mCursorAdapter;
    private String mMediaId;
    private MediaChooserFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;
    private EditText etSearchText;

    private final BroadcastReceiver mConnectivityChangeReceiver = new BroadcastReceiver() {
        private boolean oldOnline = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            // We don't care about network changes while this fragment is not associated
            // with a media ID (for example, while it is being initialized)
            if (mMediaId != null) {
                boolean isOnline = NetworkHelper.isOnline(context);
                if (isOnline != oldOnline) {
                    oldOnline = isOnline;
                    checkForUserVisibleErrors(false);
                    if (isOnline) {
                        mCursorAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

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
//            mBrowserAdapter.notifyDataSetChanged();
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            LogHelper.d(TAG, "Received state change: ", state);
            checkForUserVisibleErrors(false);
//            mBrowserAdapter.notifyDataSetChanged();
        }
    };

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
        new MediaBrowserCompat.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(@NonNull String parentId,
                                         @NonNull List<MediaBrowserCompat.MediaItem> children) {
                try {
                    LogHelper.i(TAG, "fragment onChildrenLoaded, parentId=" + parentId + "  count=" + children.size());
                    checkForUserVisibleErrors(children.isEmpty());
 //                   mBrowserAdapter.clear();
                    for (MediaBrowserCompat.MediaItem item : children) {
//                        mBrowserAdapter.add(item);
                    }
//                    mBrowserAdapter.notifyDataSetChanged();
                } catch (Throwable t) {
                    LogHelper.e(TAG, "Error on childrenloaded", t);
                }
            }

            @Override
            public void onError(@NonNull String id) {
                LogHelper.e(TAG, "browse fragment subscription onError, id=" + id);
                Toast.makeText(getActivity(), R.string.error_loading_media, Toast.LENGTH_LONG).show();
                checkForUserVisibleErrors(true);
            }
        };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaChooserFragmentListener) activity;

    }


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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_media_chooser_tracks, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        String selection = DURATION_IN_MS + " > ?";
        String[] selectionArgs = null;
        String searchId = getSearchId();
        String searchType = getSearchType();
        if (searchType == null) {
            selectionArgs = new String [1];
        } else {
            selectionArgs = new String [2];
        }
        selectionArgs[0] = Integer.toString(Settings.getMinDurationInSeconds(getActivity())*1000);
        if (searchType != null)
        {
            selectionArgs[1] = searchId;
            switch (searchType) {
                case Constants.SEARCH_TYPE_ALBUM:
                    selection += " AND " + MediaStore.Audio.Media.ALBUM_ID + "=?";
                    break;
                case Constants.SEARCH_TYPE_ARTIST:
                    selection += " AND " + MediaStore.Audio.Media.ARTIST_ID + "=?";
                    break;
            }
        }

        ContentResolver cr = getActivity().getContentResolver();
        Cursor tracksCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);

        mCursorAdapter = new MusicCursorAdapter(getActivity(), tracksCursor);

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        listView.setAdapter(mCursorAdapter);

        // This onclick listener for the list item.
        // The click could come from the list item background -> means browse
        // OR could come from the 'add' button -> means add to the music queue
        // Requires the add button code to pass the button click up to the list item click, to be handled here
        // Here we use the view parameter to determine if a button was clicked
        // of if the click came directly from the list background
        // requires android:descendantFocusability="blocksDescendants" in the list item layout,
        // otherwise the button takes all the focus and we can't click on the list item background
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                checkForUserVisibleErrors(false);
                LogHelper.i(TAG, "clicked item with position", position, "and id ", id);

                mMediaFragmentListener.onAddTrackToQueue(id);
                /*
                MediaBrowserCompat.MediaItem item = mCursorAdapter.getItem(position);

                long viewId = view.getId();
                if (viewId == R.id.plus_eq) {
                    LogHelper.i(TAG, "Add button clicked for item", item.getMediaId());
                    // This is a callback to the Music Player Activity
                    mMediaFragmentListener.onAddMediaToQueue(item);
                    // click has been handled by the add button. Nothing else to do
                    return;
                }

                // This is a callback to the Music Player Activity to browse (NOT add)
                mMediaFragmentListener.onBrowseMediaItemSelected(item);
                */

            }
        });
        etSearchText = (EditText) rootView.findViewById(R.id.searchText);

        etSearchText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {


            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {


            }

            @Override
            public void afterTextChanged(Editable s) {
                LogHelper.i(TAG, "search for ", s.toString());
                mCursorAdapter.getFilter().filter(s.toString());
            }
        });

        FilterQueryProvider filter;
        if (Constants.SEARCH_TYPE_ALBUM.equals(getSearchType()))
        {
            filter = getAlbumsFilterQueryProvider();
        } else {
            filter = getArtistsFilterQueryProvider();
        }

        mCursorAdapter.setFilterQueryProvider(filter);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        // fetch browsing information to fill the listview:
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();

        //LogHelper.i(TAG, "fragment.onStart, mediaId=", mMediaId,"  onConnected=" + mediaBrowser.isConnected());

        if (mediaBrowser.isConnected()) {
            onConnected();
        }

        // Registers BroadcastReceiver to track network connection changes.
        this.getActivity().registerReceiver(mConnectivityChangeReceiver,
            new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();
        if (mediaBrowser != null && mediaBrowser.isConnected() && mMediaId != null) {
            mediaBrowser.unsubscribe(mMediaId);
        }
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.unregisterCallback(mMediaControllerCallback);
        }
        this.getActivity().unregisterReceiver(mConnectivityChangeReceiver);
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

    public void setSearchParams(String searchType, String id) {
        LogHelper.i(TAG, "setSearchId, ",id);
        Bundle args = new Bundle(2);
        args.putString(Constants.ARG_ID, id);
        args.putString(Constants.ARG_SEARCH_TYPE, searchType);
        setArguments(args);
    }

    public String getSearchId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_ID);
        }
        return null;
    }

    public String getSearchType() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_SEARCH_TYPE);
        }
        return null;
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    public void onConnected() {
        if (isDetached()) {
            return;
        }
        mMediaId = getMediaId();
        if (mMediaId == null) {
            mMediaId = mMediaFragmentListener.getMediaBrowser().getRoot();
        }
        updateTitle();

        // Unsubscribing before subscribing is required if this mediaId already has a subscriber
        // on this MediaBrowser instance. Subscribing to an already subscribed mediaId will replace
        // the callback, but won't trigger the initial callback.onChildrenLoaded.
        //
        // This is temporary: A bug is being fixed that will make subscribe
        // consistently call onChildrenLoaded initially, no matter if it is replacing an existing
        // subscriber or not. Currently this only happens if the mediaID has no previous
        // subscriber or if the media content changes on the service side, so we need to
        // unsubscribe first.
        mMediaFragmentListener.getMediaBrowser().unsubscribe(mMediaId);

        mMediaFragmentListener.getMediaBrowser().subscribe(mMediaId, mSubscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
        MediaControllerCompat controller = ((FragmentActivity) getActivity())
                .getSupportMediaController();
        if (controller != null) {
            controller.registerCallback(mMediaControllerCallback);
        }
    }

    private void checkForUserVisibleErrors(boolean forceError) {
        boolean showError = forceError;
        // If offline, message is about the lack of connectivity:
        if (!NetworkHelper.isOnline(getActivity())) {
            mErrorMessage.setText(R.string.error_no_connection);
            showError = true;
        } else {
            // otherwise, if state is ERROR and metadata!=null, use playback state error message:
            MediaControllerCompat controller = ((FragmentActivity) getActivity())
                    .getSupportMediaController();
            if (controller != null
                && controller.getMetadata() != null
                && controller.getPlaybackState() != null
                && controller.getPlaybackState().getState() == PlaybackStateCompat.STATE_ERROR
                && controller.getPlaybackState().getErrorMessage() != null) {
                mErrorMessage.setText(controller.getPlaybackState().getErrorMessage());
                showError = true;
            } else if (forceError) {
                // Finally, if the caller requested to show error, show a generic message:
                mErrorMessage.setText(R.string.error_loading_media);
                showError = true;
            }
        }
        mErrorView.setVisibility(showError ? View.VISIBLE : View.GONE);
        LogHelper.d(TAG, "checkForUserVisibleErrors. forceError=", forceError,
            " showError=", showError,
            " isOnline=", NetworkHelper.isOnline(getActivity()));
    }

    private void updateTitle() {
        String searchType = getSearchType();
        LogHelper.i(TAG, "update tile search type = ", searchType);
        if (searchType == null)
            mMediaFragmentListener.setToolbarTitle("All music");
        else
        {
            switch (searchType) {
                case Constants.SEARCH_TYPE_ALBUM:
                    mMediaFragmentListener.setToolbarTitle("Music on album");
                    break;
                case Constants.SEARCH_TYPE_ARTIST:
                    mMediaFragmentListener.setToolbarTitle("Music by artist");
                    break;

            }
        }
    }

    // An adapter for showing the list of browsed MediaItem's
    private static class MusicCursorAdapter extends CursorAdapter  {

        public MusicCursorAdapter(Activity context, Cursor tracksCursor) {
            super(context, tracksCursor, 0);
        }

        public static class ViewHolder{
            public TextView title;
            public TextView artist;
            public ImageButton btnAddToPlayqueue;
            public View view;
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            String trackTitle = cursor.getString(1/*cursor.getColumnIndexOrThrow(nameColumn)*/);
            String trackArtist = cursor.getString(2/*cursor.getColumnIndexOrThrow(nameColumn)*/);
            viewHolder.title.setText(trackTitle);
            viewHolder.artist.setText(trackArtist);
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

            //viewHolder.btnAddToPlayqueue.setOnClickListener(new btnAddToPlayqueueClickListener(AlbumTitle));
            view.setTag(viewHolder);
            return view;
        }

        class btnAddToPlayqueueClickListener implements View.OnClickListener {
            int position;
            String title;
            // constructor
            public btnAddToPlayqueueClickListener(String title) {
                this.title = title;
            }
            @Override
            public void onClick(View v) {
                // checkbox clicked
                LogHelper.i(TAG, "add track ",title);
                /*
                if (artistListActionsListener != null)
                    artistListActionsListener.onAddArtistToPlaylistClicked(artistName);
                    */
            }
        }

    }
/*
    public interface MediaChooserFragmentListener extends MediaBrowserProvider {
        void onBrowseMediaItemSelected(MediaBrowserCompat.MediaItem item);
//        void onAddMediaToQueue(MediaBrowserCompat.MediaItem item);
        void onAddTrackToQueue(long trackId);
        void setToolbarTitle(CharSequence AlbumTitle);
    }
*/
}
