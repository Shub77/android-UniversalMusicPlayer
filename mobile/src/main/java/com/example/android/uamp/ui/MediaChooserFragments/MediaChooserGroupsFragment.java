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
public class MediaChooserGroupsFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(MediaChooserGroupsFragment.class);

    private AlbumCursorAdapter mCursorAdapter;
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
        }

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            LogHelper.d(TAG, "Received state change: ", state);
            checkForUserVisibleErrors(false);
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
    private Uri uri;
    private String[] cursorColumns;
    private String orderby;

    private Cursor getAlbumsCursor() {

        uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        final String _ID = MediaStore.Audio.Albums._ID;
        final String NUM_ITEMS_COLUMN = MediaStore.Audio.Albums.NUMBER_OF_SONGS;
        final String ARTIST_COLUMN = MediaStore.Audio.Albums.ARTIST;
        final String NAME_COLUMN = MediaStore.Audio.Albums.ALBUM;

        cursorColumns= new String[]{_ID, NAME_COLUMN, ARTIST_COLUMN, NUM_ITEMS_COLUMN};
        orderby = NAME_COLUMN + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;

        ContentResolver cr = getActivity().getContentResolver();
        return cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
    }

    private Cursor getArtistsCursor() {

        uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;

        final String _ID = MediaStore.Audio.Artists._ID;
        final String NAME_COLUMN = MediaStore.Audio.Artists.ARTIST;

        cursorColumns= new String[]{_ID, NAME_COLUMN };
        orderby = NAME_COLUMN + " COLLATE NOCASE";

        String selection = null;
        String[] selectionArgs = null;

        ContentResolver cr = getActivity().getContentResolver();
        return cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
    }

    private FilterQueryProvider getArtistsFilterQueryProvider() {

        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String partialValue = constraint.toString();
                LogHelper.i (TAG, "filtering on ", partialValue);

                final String selection = MediaStore.Audio.Artists.ARTIST +" LIKE ?";
                final String [] selectionArgs = {"%" + partialValue + "%"};

                ContentResolver cr = getActivity().getContentResolver();
                Cursor filteredCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
                return filteredCursor;
            }
        };
    }

    private FilterQueryProvider getAlbumsFilterQueryProvider() {

        return new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                String partialValue = constraint.toString();
                LogHelper.i (TAG, "filtering on ", partialValue);

                final String selection = MediaStore.Audio.Albums.ALBUM +" LIKE ?";
                final String [] selectionArgs = {"%" + partialValue + "%"};

                ContentResolver cr = getActivity().getContentResolver();
                Cursor filteredCursor =  cr.query(uri, cursorColumns, selection, selectionArgs, orderby);
                return filteredCursor;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //LogHelper.i(TAG, "fragment.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_media_chooser_groups, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        Cursor cursor;
        int subtitleColumnIndex;
        if (Constants.SEARCH_TYPE_ALBUM.equals(getSearchType())) {
            cursor = getAlbumsCursor();
            subtitleColumnIndex = 2;
        } else {
            cursor= getArtistsCursor();
            subtitleColumnIndex = -1;
        }

        mCursorAdapter = new AlbumCursorAdapter(getActivity(), cursor, 1,  subtitleColumnIndex);

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
                LogHelper.i(TAG, "clicked group with position", position, "and id ", id);
                long viewId = view.getId();
                if (viewId == R.id.plus_eq) {
                    LogHelper.i(TAG, "Add button clicked for item");//, item.getMediaId());
                    // This is a callback to the Music Player Activity
                    switch (getSearchType()) {
                        case Constants.SEARCH_TYPE_ALBUM:
                            mMediaFragmentListener.onAddAlbumToQueue(id);
                            break;
                        case Constants.SEARCH_TYPE_ARTIST:
                            mMediaFragmentListener.onAddArtistToQueue(id);
                            break;
                    }
                    return;
                }

                mMediaFragmentListener.onBrowseGroup(getSearchType(), id);
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
        Bundle args = new Bundle(1);
        args.putString(Constants.ARG_MEDIA_ID, mediaId);
        setArguments(args);
    }

    public String getSearchType() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_SEARCH_TYPE);
        }
        return null;
    }

    public void setSearchType(String searchType) {
        Bundle args = new Bundle(1);
        args.putString(Constants.ARG_SEARCH_TYPE, searchType);
        setArguments(args);
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
        switch (searchType) {
            case Constants.SEARCH_TYPE_ALBUM:
                mMediaFragmentListener.setToolbarTitle("Albums");
                break;
            case Constants.SEARCH_TYPE_ARTIST:
                mMediaFragmentListener.setToolbarTitle("Artists");
                break;

        }
    }

    // An adapter for showing the list of browsed MediaItem's
    private static class AlbumCursorAdapter extends CursorAdapter  {

        private static int titleColumnIndex;
        private static int subtitleColumnIndex;

        public AlbumCursorAdapter(Activity context, Cursor cursor, int titleColumnIndex, int subtitleColumnIndex) {
            super(context, cursor, 0);
            this.titleColumnIndex = titleColumnIndex;
            this.subtitleColumnIndex = subtitleColumnIndex;
        }

        public static class GroupViewHolder {
            public TextView title;
            public TextView subtitle;
            public ImageButton btnAddToPlayqueue;
            public View view;
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            GroupViewHolder viewHolder = (GroupViewHolder) view.getTag();
            String title = cursor.getString(titleColumnIndex /*cursor.getColumnIndexOrThrow(NAME_COLUMN)*/);
            viewHolder.title.setText(title);
            if (subtitleColumnIndex >= 0) {
                String subtitle = cursor.getString(subtitleColumnIndex /*cursor.getColumnIndexOrThrow(NAME_COLUMN)*/);
                viewHolder.subtitle.setText(subtitle);
            }
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, final ViewGroup parent) {
            View view =  LayoutInflater.from(context).inflate(R.layout.list_item_group, parent, false);
            GroupViewHolder viewHolder = new GroupViewHolder();
            viewHolder.title = (TextView) view.findViewById(R.id.title);
            viewHolder.subtitle = (TextView) view.findViewById(R.id.description);
            viewHolder.btnAddToPlayqueue = (ImageButton) view.findViewById(R.id.plus_eq);

            String title = cursor.getString(1/*cursor.getColumnIndexOrThrow(nameColumn)*/);
            long id = cursor.getLong(0);

            viewHolder.btnAddToPlayqueue.setOnClickListener(new btnAddToPlayqueueClickListener(0, parent, id));
            view.setTag(viewHolder);
            return view;
        }

        class btnAddToPlayqueueClickListener implements View.OnClickListener {
            int position;
            long id;
            ViewGroup parent;
            // constructor
            public btnAddToPlayqueueClickListener(int position, ViewGroup parent, long id) {
                this.position = position;
                this.parent = parent;
                this.id = id;
            }
            @Override
            public void onClick(View v) {
                // checkbox clicked
                LogHelper.i(TAG, "add group pos ",position);
                ((ListView) parent).performItemClick(v, position, id); // Let the event be handled in onItemClick()
                /*
                if (artistListActionsListener != null)
                    artistListActionsListener.onAddArtistToPlaylistClicked(artistName);
                    */
            }
        }

    }
}
