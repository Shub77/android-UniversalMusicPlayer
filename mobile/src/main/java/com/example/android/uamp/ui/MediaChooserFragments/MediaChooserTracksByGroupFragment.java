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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.constants.Constants;
import com.example.android.uamp.model.MediaChooserFragmentListener;
import com.example.android.uamp.ui.dialogs.ClearableEditText;
import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that lists tracks in a group (an album or an artist)
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * This actually uses the mediaBrowserService to browse on the service...
 * unlike earlier efforts which browsed on the client
 * It uses a {@link MediaBrowserCompat} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get all the children.
 * All {@link MediaBrowserCompat.MediaItem}'s that can be browsed are shown in a ListView.
 */
public class MediaChooserTracksByGroupFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(MediaChooserTracksByGroupFragment.class);

    private MusicListAdapter mListAdapter;
    ArrayList<MediaBrowserCompat.MediaItem> mMediaItems = new ArrayList<>();

    private MediaChooserFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;
    private ClearableEditText etSearchText;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaChooserFragmentListener) activity;

    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_media_chooser_tracks, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        if (Constants.SEARCH_TYPE_ARTIST.equals(getSearchType())) {
            getSongsByGroupandId("ARTIST",getSearchId());
        }
         else if (Constants.SEARCH_TYPE_ALBUM.equals(getSearchType())) {
            getSongsByGroupandId("ALBUM",getSearchId());
        }

        ListView listView = (ListView) rootView.findViewById(R.id.list_view);
        mListAdapter = new MusicListAdapter(getActivity(), mMediaItems, mMediaFragmentListener);
        listView.setAdapter(mListAdapter);

        etSearchText = (ClearableEditText) rootView.findViewById(R.id.searchText);



        if (Constants.SEARCH_TYPE_ALBUM.equals(getSearchType())) {
            etSearchText.setHint(R.string.search_albums);
        } else if (Constants.SEARCH_TYPE_ARTIST.equals(getSearchType())) {
            etSearchText.setHint(R.string.search_artists);
        }

        etSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mListAdapter.getFilter().filter(s.toString());
            }
        });

        return rootView;
    }

    private void getSongsByGroupandId(String groupString, String idString) {
        LogHelper.i(TAG, "getSongsByArtist");
        MediaBrowserCompat browser = mMediaFragmentListener.getMediaBrowser();
        boolean b = browser.isConnected();

        if (b) {
            LogHelper.i(TAG, "connected:");
        } else
        {
            LogHelper.i(TAG, "NOT connected");
        }
        String rootID =  browser.getRoot();
        getSearchId();
        String mediaSearchId = groupString+"__"+idString;
        LogHelper.i(TAG, "connected:", rootID, ", mediaSearchId=", mediaSearchId);
        browser.subscribe(mediaSearchId, new MediaBrowserCompat.SubscriptionCallback() {
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

        String searchType = getSearchType();

        switch (searchType) {
            case Constants.SEARCH_TYPE_ALBUM:
                mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_tracks_on_album_title);
                break;
            case Constants.SEARCH_TYPE_ARTIST:
                mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_tracks_by_artist_title);
                break;
        }
    }

    // An adapter for showing the list of browsed MediaItem's
    private static class MusicListAdapter extends ArrayAdapter<MediaBrowserCompat.MediaItem> implements Filterable {

        MediaChooserFragmentListener mListener;
        ArrayList<MediaBrowserCompat.MediaItem> mUnFilteredMediaItems;
        ArrayList<MediaBrowserCompat.MediaItem> mMediaItems;
        Activity mActivity;

        // The filtering code comes from: http://codetheory.in/android-filters/
        @Override
        public Filter getFilter() {
            // return a filter that filters data based on a constraint

            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    // Create a FilterResults object
                    FilterResults results = new FilterResults();

                    // If the constraint (search string/pattern) is null
                    // or its length is 0, i.e., its empty then
                    // we just set the `values` property to the
                    // original contacts list which contains all of them
                    if (constraint == null || constraint.length() == 0) {
                        results.values = mUnFilteredMediaItems;
                        results.count = mUnFilteredMediaItems.size();
                    }
                    else {
                        // Some search copnstraint has been passed
                        // so let's filter accordingly
                        ArrayList<MediaBrowserCompat.MediaItem> filteredMedaItems = new ArrayList<>();

                        // We'll go through all the contacts and see
                        // if they contain the supplied string
                        for (MediaBrowserCompat.MediaItem item : mUnFilteredMediaItems) {
                            String title = item.getDescription().getTitle().toString().toUpperCase();
                            String subtitle = item.getDescription().getSubtitle().toString().toUpperCase();
                            String ucConstraint = constraint.toString().toUpperCase();

                            LogHelper.i(TAG, "filter: title-", title, "subtitle-", subtitle, "constraint-", ucConstraint);

                            if ( subtitle.contains( ucConstraint)
                            || title.contains( ucConstraint) )
                            {
                                // if `contains` == true then add it
                                // to our filtered list
                                filteredMedaItems.add(item);
                            }
                        }

                        // Finally set the filtered values and size/count
                        results.values = filteredMedaItems;
                        results.count = filteredMedaItems.size();
                    }

                    // Return our FilterResults object
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    mMediaItems = (ArrayList<MediaBrowserCompat.MediaItem>) results.values;
                    LogHelper.i(TAG, "Filter: publishResults f-size=", mMediaItems.size(), ",uf-size=", mUnFilteredMediaItems.size());
                    notifyDataSetChanged();
                }
            };
        }

        public MusicListAdapter(Activity context, ArrayList<MediaBrowserCompat.MediaItem> mediaItems,  MediaChooserFragmentListener listener) {
            super(context, R.layout.list_item_track, mediaItems);
            mListener = listener;
            mMediaItems = mediaItems;
            mUnFilteredMediaItems = mediaItems;
            mActivity = context;
        }

        public static class ViewHolder{
            public TextView title;
            public TextView artist;
            public ImageButton btnAddToPlayqueue;
            public View view;
        }

        @Override
        public int getCount() {
            return mMediaItems.size();
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

            LogHelper.i(TAG, "getView size=", mMediaItems.size(), " pos=", position );
            // fill data
            ViewHolder holder = (ViewHolder) rowView.getTag();
            MediaBrowserCompat.MediaItem mediaItem = mMediaItems.get(position);
            holder.title.setText(mediaItem.getDescription().getTitle());
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
    }
}
