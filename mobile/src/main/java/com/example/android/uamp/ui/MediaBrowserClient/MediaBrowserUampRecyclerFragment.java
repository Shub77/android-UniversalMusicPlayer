package com.example.android.uamp.ui.MediaBrowserClient;

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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.media.MediaItemStatus;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.uamp.R;
import com.example.android.uamp.ui.dialogs.ClearableEditText;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.NetworkHelper;

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
public class MediaBrowserUampRecyclerFragment extends Fragment implements MediaBrowserClientUampRecyclerViewAdapter.BrowserRecyclerViewAdapterListener {

    private static final String TAG = LogHelper.makeLogTag(MediaBrowserUampRecyclerFragment.class);

    private static final String ARG_MEDIA_ID = "arg_media_id";
    private static final String ARG_TITLE = "arg_title";

    private String mMediaId;
    private String mTitle; // I added this. Same as mMediaId
    private MediaFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;
    private ClearableEditText etSearchText;

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    //private BrowseAdapter mBrowserAdapter;
    private MediaBrowserClientUampRecyclerViewAdapter mAdapter;

    ArrayList<MediaBrowserCompat.MediaItem> myDataset;

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
                        //mBrowserAdapter.notifyDataSetChanged();
                        mAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    // Receive callbacks from the MediaController. Here we update our state such as which queue
    // is being shown, the current title and description and the PlaybackState.
    private final MediaControllerCompat.Callback mMediaControllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    super.onMetadataChanged(metadata);
                    if (metadata == null) {
                        return;
                    }
                    LogHelper.i(TAG, "Received metadata change to media ",
                            metadata.getDescription().getMediaId());
                    //mBrowserAdapter.notifyDataSetChanged();
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onPlaybackStateChanged(@NonNull PlaybackStateCompat state) {
                    super.onPlaybackStateChanged(state);
                    LogHelper.i(TAG, "Received state change: ", state);
                    checkForUserVisibleErrors(false);
                    //mBrowserAdapter.notifyDataSetChanged();
                    mAdapter.notifyDataSetChanged();
                }
            };

    private final MediaBrowserCompat.SubscriptionCallback mSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    try {
                        LogHelper.i(TAG, "fragment onChildrenLoaded, parentId=" + parentId +
                                "  count=" + children.size());
                        checkForUserVisibleErrors(children.isEmpty());
                        //mBrowserAdapter.clear();
                        myDataset.clear();
                        for (MediaBrowserCompat.MediaItem item : children) {
                            //mBrowserAdapter.add(item);
                            myDataset.add(item);
                        }
                        //mBrowserAdapter.notifyDataSetChanged();
                        mAdapter.notifyDataSetChanged();
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
        mMediaFragmentListener = (MediaFragmentListener) activity;

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LogHelper.i(TAG, "fragment.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_uamp_recycler_list, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        // mBrowserAdapter = new BrowseAdapter(getActivity());

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        myDataset = new ArrayList<>();
        // specify an adapter
        mAdapter = new MediaBrowserClientUampRecyclerViewAdapter(myDataset, this, getActivity());
        recyclerView.setAdapter(mAdapter);

        etSearchText = (ClearableEditText) rootView.findViewById(R.id.searchText);
        etSearchText.setHint(R.string.search_tracks);
        etSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mAdapter.getFilter().filter(s.toString());
            }
        });

        return rootView;
    }

    @Override
    public void onAddClick(MediaBrowserCompat.MediaItem item) {
        mMediaFragmentListener.onMediaItemSelectedForPlay(item);
    }

    @Override
    public void onBrowseClick(MediaBrowserCompat.MediaItem item) {
        mMediaFragmentListener.onMediaItemSelectedForBrowse(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        LogHelper.i(TAG, "onStart");
        // fetch browsing information to fill the listview:
        MediaBrowserCompat mediaBrowser = mMediaFragmentListener.getMediaBrowser();

        LogHelper.i(TAG, "fragment.onStart, mediaId=", mMediaId, ", title=", mTitle,
                "  onConnected=" + mediaBrowser.isConnected());

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
        MediaControllerCompat controller = MediaControllerCompat.getMediaController((FragmentActivity) getActivity());
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
            return args.getString(ARG_MEDIA_ID);
        }
        return null;
    }

    public String getTitle() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(ARG_TITLE);
        }
        return null;
    }

    // This was just setMediaId, but now the fragment also remembers it's title
    public void setMediaIdAndTitle(String mediaId, String title) {
        Bundle args = new Bundle(1);
        args.putString(MediaBrowserUampRecyclerFragment.ARG_MEDIA_ID, mediaId);
        args.putString(ARG_TITLE, title);
        setArguments(args);
    }

    // Called when the MediaBrowser is connected. This method is either called by the
    // fragment.onStart() or explicitly by the activity in the case where the connection
    // completes after the onStart()
    public void onConnected() {
        LogHelper.i(TAG, "onConnected");
        if (isDetached()) {
            LogHelper.i(TAG, "isDetached");
            return;
        }
        mMediaId = getMediaId();
        if (mMediaId == null) {
            LogHelper.i(TAG, "mMediaId is null");
            mMediaId = mMediaFragmentListener.getMediaBrowser().getRoot();
        }

        mTitle = getTitle();
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
        LogHelper.i(TAG, "subscribe to ", mMediaId);
        mMediaFragmentListener.getMediaBrowser().subscribe(mMediaId, mSubscriptionCallback);

        // Add MediaController callback so we can redraw the list when metadata changes:
        MediaControllerCompat controller =  MediaControllerCompat.getMediaController((FragmentActivity) getActivity());
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
            MediaControllerCompat controller =  MediaControllerCompat.getMediaController((FragmentActivity) getActivity());
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
        LogHelper.i(TAG, "checkForUserVisibleErrors. forceError=", forceError,
                " showError=", showError,
                " isOnline=", NetworkHelper.isOnline(getActivity()));
    }

    private void updateTitle() {
        mMediaFragmentListener.setToolbarTitle(mTitle);
    }

    public interface MediaFragmentListener  {// extends MediaBrowserProvider {
        void onMediaItemSelectedForBrowse(MediaBrowserCompat.MediaItem item);
        // I added the play. Originally one method was used for play and browse
        // So an item couldn't be browsable _and_ playable
        void onMediaItemSelectedForPlay(MediaBrowserCompat.MediaItem item);
        void setToolbarTitle(CharSequence title);
        // This was in (the only method of) MediaBrowserProvider but I just put it here.
        MediaBrowserCompat getMediaBrowser();
    }

}
