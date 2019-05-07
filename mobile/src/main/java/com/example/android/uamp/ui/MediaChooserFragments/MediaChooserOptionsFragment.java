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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.android.uamp.R;
import com.example.android.uamp.model.MediaChooserFragmentListener;
import com.example.android.uamp.ui.MediaItemViewHolder;
import com.example.android.uamp.utils.LogHelper;
import com.example.android.uamp.utils.MediaIDHelper;
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
public class MediaChooserOptionsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = LogHelper.makeLogTag(MediaChooserOptionsFragment.class);

    private static final String ARG_MEDIA_ID = "media_id";

    private MediaChooserFragmentListener mMediaFragmentListener;
    private View mErrorView;
    private TextView mErrorMessage;

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
        View rootView = inflater.inflate(R.layout.fragment_media_chooser_options, container, false);

        mErrorView = rootView.findViewById(R.id.playback_error);
        mErrorMessage = (TextView) mErrorView.findViewById(R.id.error_message);

        Button b;
        b = (Button) rootView.findViewById(R.id.btnChooseSong);
        b.setOnClickListener(this);
        b = (Button) rootView.findViewById(R.id.btnChooseAlbum);
        b.setOnClickListener(this);
        b = (Button) rootView.findViewById(R.id.btnChooseArtist);
        b.setOnClickListener(this);
        b = (Button) rootView.findViewById(R.id.btnHistory);
        b.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnChooseSong:
                LogHelper.i(TAG, "btnChooseSongClicked");
                mMediaFragmentListener.onChooseTrack();
                break;

            case R.id.btnChooseAlbum:
                LogHelper.i(TAG, "btnChooseAlbumClicked");
                mMediaFragmentListener.onChooseAlbum();
                break;

            case R.id.btnChooseArtist:
                LogHelper.i(TAG, "btnChooseArtist");
                mMediaFragmentListener.onChooseArtist();
                break;
            case R.id.btnHistory:
                LogHelper.i(TAG, "btnHistory");
                mMediaFragmentListener.onHistory();
                break;
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        mMediaFragmentListener.setToolbarTitle(R.string.media_chooser_mainmenu_title);
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
}
