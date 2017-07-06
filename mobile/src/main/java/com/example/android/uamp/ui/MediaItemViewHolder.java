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
package com.example.android.uamp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.utils.MediaIDHelper;

public class MediaItemViewHolder {

    public static final int STATE_INVALID = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYABLE = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;

    private static ColorStateList sColorStatePlaying;
    private static ColorStateList sColorStateNotPlaying;

    ImageView mPlayImageView;
    ImageView mAddImageView;
    TextView mTitleView;
    TextView mDescriptionView;

    // Returns a view for use in media item list.
    static View setupListView(Activity activity, View convertView, ViewGroup parent,
                              MediaBrowserCompat.MediaItem item) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(activity);
        }

        MediaItemViewHolder holder;

        Integer cachedState = STATE_INVALID;

        if (convertView == null) {
            convertView = LayoutInflater.from(activity)
                    .inflate(R.layout.media_list_item_with_plus, parent, false);
            holder = new MediaItemViewHolder();
            holder.mPlayImageView = (ImageView) convertView.findViewById(R.id.play_eq);
            holder.mAddImageView = (ImageView) convertView.findViewById(R.id.plus_eq);
            holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
            holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
            convertView.setTag(holder);
        } else {
            holder = (MediaItemViewHolder) convertView.getTag();
            cachedState = (Integer) convertView.getTag(R.id.tag_mediaitem_state_cache);
        }

        MediaDescriptionCompat description = item.getDescription();
        holder.mTitleView.setText(description.getTitle());
        holder.mDescriptionView.setText(description.getSubtitle());

        String mediaID = item.getMediaId();
        if (mediaID.indexOf('/') < 0 && mediaID.indexOf('|') < 0) {
            holder.mAddImageView.setVisibility(View.GONE);
        } else {
            holder.mAddImageView.setVisibility(View.VISIBLE);
            //holder.mAddImageView.setOnClickListener(new OnAddImageViewClickListener(mediaID));
        }

        // If the state of convertView is different, we need to adapt the view to the
        // new state.
        int state = getMediaItemState(activity, item);
        if (cachedState == null || cachedState != state) {
            Drawable drawable = getDrawableByState(activity, state);
            if (drawable != null) {
                holder.mPlayImageView.setImageDrawable(drawable);
                holder.mPlayImageView.setVisibility(View.VISIBLE);
            } else {
                holder.mPlayImageView.setVisibility(View.GONE);
            }
            convertView.setTag(R.id.tag_mediaitem_state_cache, state);
        }

        return convertView;
    }

    private static void initializeColorStateLists(Context ctx) {
        sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_not_playing));
        sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
            R.color.media_item_icon_playing));
    }

    /**
     * Gets the Drawable to display in the media browser for the equalizer
     * Depends on the item category and type
     * If it isn't playable... No image
     * If it is Playing ... animated equalizer
     * If it is Paused ... stopped equalizer
     * @param context
     * @param state
     * @return
     */
    public static Drawable getDrawableByState(Context context, int state) {
        if (sColorStateNotPlaying == null || sColorStatePlaying == null) {
            initializeColorStateLists(context);
        }

        switch (state) {
            case STATE_PLAYABLE:
                Drawable pauseDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_play_arrow_black_36dp);
                DrawableCompat.setTintList(pauseDrawable, sColorStateNotPlaying);
                return pauseDrawable;
            case STATE_PLAYING:
                AnimationDrawable animation = (AnimationDrawable)
                        ContextCompat.getDrawable(context, R.drawable.ic_equalizer_white_36dp);
                DrawableCompat.setTintList(animation, sColorStatePlaying);
                animation.start();
                return animation;
            case STATE_PAUSED:
                Drawable playDrawable = ContextCompat.getDrawable(context,
                        R.drawable.ic_equalizer1_white_36dp);
                DrawableCompat.setTintList(playDrawable, sColorStatePlaying);
                return playDrawable;
            default:
                return null;
        }
    }

    public static int getMediaItemState(Context context, MediaBrowserCompat.MediaItem mediaItem) {
        int state = STATE_NONE;
        // Set state to playable first, then override to playing or paused state if needed
        if (mediaItem.isPlayable()) {
            state = STATE_PLAYABLE;
            if (MediaIDHelper.isMediaItemPlaying(context, mediaItem)) {
                state = getStateFromController(context);
            }
        }

        return state;
    }

    public static int getStateFromController(Context context) {
        MediaControllerCompat controller = ((FragmentActivity) context)
                .getSupportMediaController();
        PlaybackStateCompat pbState = controller.getPlaybackState();
        if (pbState == null ||
                pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
            return MediaItemViewHolder.STATE_NONE;
        } else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            return  MediaItemViewHolder.STATE_PLAYING;
        } else {
            return MediaItemViewHolder.STATE_PAUSED;
        }
    }

    public static  View getListItemViewView(Activity activity, View convertView, final ViewGroup parent, MediaBrowserCompat.MediaItem item, final int position) {
        MediaItemViewHolder holder;

        Integer cachedState = STATE_INVALID;

        if (convertView == null) {
            convertView = LayoutInflater.from(activity)
                    .inflate(R.layout.media_list_item_with_plus, parent, false);
            holder = new MediaItemViewHolder();
            holder.mPlayImageView = (ImageView) convertView.findViewById(R.id.play_eq);
            holder.mAddImageView = (ImageView) convertView.findViewById(R.id.plus_eq);
            holder.mTitleView = (TextView) convertView.findViewById(R.id.title);
            holder.mDescriptionView = (TextView) convertView.findViewById(R.id.description);
            convertView.setTag(holder);
        } else {
            holder = (MediaItemViewHolder) convertView.getTag();
            cachedState = (Integer) convertView.getTag(R.id.tag_mediaitem_state_cache);
        }

        // Set the text
        MediaDescriptionCompat description = item.getDescription();
        holder.mTitleView.setText(description.getTitle());
        holder.mDescriptionView.setText(description.getSubtitle());

        // Handle the visibility of the add button (not valid for top level categories)
        String mediaID = item.getMediaId();
        if (MediaIDHelper.isValidCategory(mediaID)) {
            holder.mAddImageView.setVisibility(View.GONE);
        } else {
            holder.mAddImageView.setVisibility(View.VISIBLE);
            // This onclick listener for the button just passes the click to be handled by the standard list
            // list item on click code (which will use the 'view' to determine if a button was clicked
            // of if the click came directly from the list background
            // requires android:descendantFocusability="blocksDescendants" in the list item layout,
            // otherwise the button takes all the focus and we can't click on the list item background
            // see http://www.migapro.com/click-events-listview-gridview/
            holder.mAddImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ListView) parent).performItemClick(v, position, 0); // Let the event be handled in onItemClick()
                }
            });
        }

        // If the state of convertView is different, we need to adapt the view to the new state
        int state = MediaItemViewHolder.getMediaItemState(activity, item);
        if (cachedState == null || cachedState != state) {
            Drawable drawable = MediaItemViewHolder.getDrawableByState(activity, state);
            if (drawable != null) {
                holder.mPlayImageView.setImageDrawable(drawable);
                holder.mPlayImageView.setVisibility(View.VISIBLE);
            } else {
                holder.mPlayImageView.setVisibility(View.GONE);
            }
            convertView.setTag(R.id.tag_mediaitem_state_cache, state);
        }
        return convertView;
    }

}
