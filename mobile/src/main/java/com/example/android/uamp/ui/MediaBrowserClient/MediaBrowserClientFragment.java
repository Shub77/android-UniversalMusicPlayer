package com.example.android.uamp.ui.MediaBrowserClient;

import android.app.Activity;
import android.app.Fragment;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.arch.paging.PositionalDataSource;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;


import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;

import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.constants.Constants;
import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A Fragment that lists all tracks from browser using pagination
 * from a {@link android.service.media.MediaBrowserService}.
 * <p/>
 * Uses the mediaBrowserService to browse on the service...
 * unlike earlier efforts which browsed on the client
 * It uses a {@link MediaBrowserCompat} to connect to the {@link com.example.android.uamp.MusicService}.
 * Once connected, the fragment subscribes to get all the children.
 * All {@link MediaBrowserCompat.MediaItem}'s that can be browsed are shown in a ListView.
 */
public class MediaBrowserClientFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag("MedBroClFrag");

    private MusicListAdapter mListAdapter;
    private Observer mObserver;
    private LiveData<PagedList<BrowsableItem>> mLiveSongData;

    private MediaBrowserClientFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;

    LinearLayoutManager mLayoutManager;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        LogHelper.i(TAG, "OnAttach");
        // If used on an activity that doesn't implement MediaFragmentListener, it
        // will throw an exception as expected:
        mMediaFragmentListener = (MediaBrowserClientFragmentListener) activity;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogHelper.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_media_chooser_paginated_tracks, container, false);
        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.list_view);
        mLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(mLayoutManager);
        mListAdapter = new MusicListAdapter(mMediaFragmentListener);
        recyclerView.setAdapter(mListAdapter);

        getItems();
        mMediaFragmentListener.setTitleString(getTitle());
        return rootView;
    }

    /**
     * Called from OnCreateView
     */
    private void getItems() {
        LogHelper.i(TAG, "getItems");
        MediaBrowserCompat browser = mMediaFragmentListener.getMediaBrowser();
        LogHelper.i(TAG, "browser is ", browser.isConnected()?"connected":"NOT connected");

        PagedList.Config config = new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPageSize(10)
                .build();

        String mediaId = getMediaId();
        LogHelper.i(TAG, "mediaId:", mediaId);
        // The SongsDataSourceFactory will return a SongsDataSource which extends PositionalDataSource
        // It gets songs using our media browser
        // That is a data source that knows all about pages (Methods loadInitial, loadRange etc)
        SongsDataSourceFactory songsDataSourceFactory = new SongsDataSourceFactory(browser, mediaId);
        // From this data source we create a LivePagedList LiveData<PagedList<BrowsableItem>>
        mLiveSongData = new LivePagedListBuilder<>(songsDataSourceFactory, config).build();
        // Create an observer for a paged list of browsable items
        // When the observed list changes we update the PagedListAdapter view
        mObserver = new Observer<PagedList<BrowsableItem>>() {
            @Override
            public void onChanged(@Nullable PagedList<BrowsableItem> browsableItems) {
                LogHelper.i(TAG, "Observer.onChanged");
                mListAdapter.submitList(browsableItems);
            }
        };
        // Use the observer to observe the paged list
        mLiveSongData.observeForever(mObserver);
    }

    @Override
    public void onStart() {
        super.onStart();
        LogHelper.i(TAG, "OnStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        LogHelper.i(TAG, "OnStop");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        LogHelper.i(TAG, "OnDetach");
        if (mLiveSongData != null && mObserver != null) {
            mLiveSongData.removeObserver(mObserver);
        }
        mMediaFragmentListener = null;

    }

    public String getMediaId() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_MEDIA_ID);
        }
        return null;
    }

    public String getTitle() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Constants.ARG_TITLE);
        }
        return null;
    }

    public void setBrowseParameters(String mediaId, String title) {
        LogHelper.i(TAG, "setBrowseParameters ",mediaId, ",", title);
        Bundle args = new Bundle(2);
        args.putString(Constants.ARG_MEDIA_ID, mediaId);
        args.putString(Constants.ARG_TITLE, title);
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

    // An PAGED adapter for showing the list of browsed MediaItem's
    private static class MusicListAdapter extends PagedListAdapter<BrowsableItem, MusicListAdapter.ViewHolder> { // implements Filterable {

        MediaBrowserClientFragmentListener mListener;

        public static DiffUtil.ItemCallback<BrowsableItem> DIFF_CALLBACK =
                new DiffUtil.ItemCallback<BrowsableItem>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull BrowsableItem oldUser, @NonNull BrowsableItem newUser) {
                        // User properties may have changed if reloaded from the DB, but ID is fixed
                        return oldUser.mediaId.equals(newUser.mediaId);
                    }
                    @Override
                    public boolean areContentsTheSame(
                            @NonNull BrowsableItem oldUser, @NonNull BrowsableItem newUser) {
                        // NOTE: if you use equals, your object must properly override Object#equals()
                        // Incorrectly returning false here will result in too many animations.
                        return oldUser.title.equals(newUser.title) && oldUser.subtitle.equals(newUser.subtitle);
                    }
                };


        public MusicListAdapter(MediaBrowserClientFragmentListener listener) {
            super(DIFF_CALLBACK);
            mListener = listener;
        }

        public static class ViewHolder  extends RecyclerView.ViewHolder {
            public final TextView tvTitle;
            public final TextView tvArtist;
            public final ImageButton btnAddToPlayqueue;
            public final View view;

            public ViewHolder(View v) {
                super(v);
                view = v;
                tvTitle = (TextView)v.findViewById(R.id.title);
                tvArtist = (TextView)v.findViewById(R.id.artist);
                btnAddToPlayqueue = (ImageButton) itemView.findViewById(R.id.plus_eq);
            }

            public void bindTo(BrowsableItem browsableItem){
                tvTitle.setText(browsableItem.title);
                tvArtist.setText(browsableItem.subtitle);


            }

            public void clear() {
                tvTitle.setText("");
                tvArtist.setText("");

            }
        }

        class btnAddToPlayqueueClickListener implements View.OnClickListener {
            private int position;
            private String id;
            private MediaBrowserClientFragmentListener listener;

            // constructor
            public btnAddToPlayqueueClickListener(int position, String id, MediaBrowserClientFragmentListener listener) {
                this.position = position;
                this.id = id;
                this.listener = listener;
            }
            @Override
            public void onClick(View v) {
                // button clicked
                LogHelper.i(TAG, "add pos ",position, "id=", id);
                listener.onAddBrowsableItemToQueueByMediaIdFromRecyclerView(id);
            }
        }

        class btnBrowseToItemClickListener implements View.OnClickListener {
            private int position;
            private String id;
            private MediaBrowserClientFragmentListener listener;
            private String title;

            // constructor
            public btnBrowseToItemClickListener(int position, String id, String title, MediaBrowserClientFragmentListener listener) {
                this.position = position;
                this.id = id;
                this.listener = listener;
                this.title = title;
            }
            @Override
            public void onClick(View v) {
                // button clicked
                LogHelper.i(TAG, "browse pos ",position, "id=", id);
                listener.onBrowseItemFromRecyclerView(id, title);
            }
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MusicListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_track, parent, false);

            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder( ViewHolder  holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            BrowsableItem browsableItem = getItem(position);
            if (browsableItem != null) {
                holder.bindTo(browsableItem);

                if (browsableItem.isBrowsable) {
                    holder.view.setOnClickListener(new btnBrowseToItemClickListener(0, /*Long.parseLong(*/browsableItem.mediaId/*)*/, browsableItem.title, mListener));
                }
                if (browsableItem.isPlayable) {
                    holder.btnAddToPlayqueue.setVisibility(View.VISIBLE);
                    holder.btnAddToPlayqueue.setOnClickListener(new btnAddToPlayqueueClickListener(0, /*Long.parseLong(*/browsableItem.mediaId/*)*/, mListener));
                } else {
                    holder.btnAddToPlayqueue.setVisibility(View.INVISIBLE);
                }
            } else {
                // Null defines a placeholder item - PagedListAdapter will automatically invalidate
                // this row when the actual object is loaded from the database
                holder.clear();
            }
        }


    }


}

