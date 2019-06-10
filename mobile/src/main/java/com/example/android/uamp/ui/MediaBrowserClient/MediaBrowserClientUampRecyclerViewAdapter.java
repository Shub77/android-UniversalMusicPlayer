        package com.example.android.uamp.ui.MediaBrowserClient;

        import android.content.Context;
        import android.content.res.ColorStateList;
        import android.graphics.drawable.AnimationDrawable;
        import android.graphics.drawable.Drawable;
        import android.media.browse.MediaBrowser;
        import android.support.v4.app.FragmentActivity;
        import android.support.v4.content.ContextCompat;
        import android.support.v4.graphics.drawable.DrawableCompat;
        import android.support.v4.media.MediaBrowserCompat;
        import android.support.v4.media.MediaDescriptionCompat;
        import android.support.v4.media.session.MediaControllerCompat;
        import android.support.v4.media.session.PlaybackStateCompat;
        import android.support.v7.widget.RecyclerView;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.Filter;
        import android.widget.Filterable;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.example.android.uamp.R;
        import com.example.android.uamp.utils.LogHelper;

        import java.util.ArrayList;
        import java.util.List;

public class MediaBrowserClientUampRecyclerViewAdapter extends RecyclerView.Adapter<MediaBrowserClientUampRecyclerViewAdapter.MyViewHolder>
        implements Filterable {

    private ArrayList<MediaBrowserCompat.MediaItem> mDataset;
    private ArrayList<MediaBrowserCompat.MediaItem> mFilteredDataset;

    private static final String TAG = LogHelper.makeLogTag(MediaBrowserClientUampRecyclerViewAdapter.class);

    public static final int STATE_INVALID = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_PLAYABLE = 1;
    public static final int STATE_PAUSED = 2;
    public static final int STATE_PLAYING = 3;
    private static ColorStateList sColorStatePlaying;
    private static ColorStateList sColorStateNotPlaying;

    private BrowserRecyclerViewAdapterListener mListener ;
    private Context mContext;
    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder {



        // each data item is just a string in this case
        //public TextView textView;
        View view;
        ImageView imageView;
        TextView titleView;
        TextView descriptionView;
        ImageButton plusView;

        public MyViewHolder(View v) {
            super(v);
            view = v;
            imageView = (ImageView) v.findViewById(R.id.play_eq);
            plusView = (ImageButton) v.findViewById(R.id.plus_eq);
            titleView = (TextView) v.findViewById(R.id.title);
            descriptionView = (TextView) v.findViewById(R.id.description);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MediaBrowserClientUampRecyclerViewAdapter(ArrayList<MediaBrowserCompat.MediaItem> myDataset,
                                                     BrowserRecyclerViewAdapterListener listener, Context context) {
        mDataset = myDataset;
        mFilteredDataset = myDataset;
        mListener = listener;
        mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MediaBrowserClientUampRecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                                     int viewType) {
        // create a new view
        View v = (View) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.media_list_item_with_plus, parent, false);

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    mFilteredDataset = mDataset;
                } else {
                    ArrayList<MediaBrowserCompat.MediaItem> filteredList = new ArrayList<>();
                    for (MediaBrowserCompat.MediaItem item : mDataset) {

                        // name match condition. this might differ depending on your requirement
                        // here we are looking for name or phone number match
                        if (item.getDescription().getTitle().toString().toLowerCase().contains(charString.toLowerCase())
                                || item.getDescription().getSubtitle().toString().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(item);
                        }
                    }

                    mFilteredDataset = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = mFilteredDataset ;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mFilteredDataset = (ArrayList<MediaBrowserCompat.MediaItem>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final MediaBrowserCompat.MediaItem  item = mFilteredDataset.get(position);
        MediaDescriptionCompat description = item.getDescription();
        holder.titleView.setText(description.getTitle());
        holder.descriptionView.setText(description.getSubtitle());

        holder.plusView.setVisibility(item.isPlayable()? View.VISIBLE:View.INVISIBLE);

        if (item.isPlayable()) {
            holder.plusView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogHelper.i(TAG, "Plus button for:", item.getMediaId());
                    mListener.onAddClick(item);
                }
            });
        }
        if (item.isBrowsable()) {
            holder.view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LogHelper.i(TAG, "background for:", item.getMediaId());
                    mListener.onBrowseClick(item);

                }
            });
        }

        Integer cachedState = STATE_INVALID;
        cachedState = (Integer) holder.view.getTag(R.id.tag_mediaitem_state_cache);

        // If the state of convertView is different, we need to adapt the view to the
        // new state.
        int state = getMediaItemState(mContext, item);
        if (cachedState == null || cachedState != state) {
            Drawable drawable = getDrawableByState(mContext, state);
            if (drawable != null) {
                holder.imageView.setImageDrawable(drawable);
                holder.imageView.setVisibility(View.VISIBLE);
            } else {
                holder.imageView.setVisibility(View.INVISIBLE);
            }
            holder.view.setTag(R.id.tag_mediaitem_state_cache, state);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mFilteredDataset.size();
    }

    ///////////////

    private static void initializeColorStateLists(Context ctx) {
        sColorStateNotPlaying = ColorStateList.valueOf(ctx.getResources().getColor(
                R.color.media_item_icon_not_playing));
        sColorStatePlaying = ColorStateList.valueOf(ctx.getResources().getColor(
                R.color.media_item_icon_playing));
    }


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
            if (MediaIDUampHelper.isMediaItemPlaying(context, mediaItem)) {
                state = getStateFromController(context);
            }
        }
        return state;
    }

    public static int getStateFromController(Context context) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController((FragmentActivity) context);
        PlaybackStateCompat pbState = controller.getPlaybackState();
        if (pbState == null ||
                pbState.getState() == PlaybackStateCompat.STATE_ERROR) {
            return STATE_NONE;
        } else if (pbState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            return  STATE_PLAYING;
        } else {
            return STATE_PAUSED;
        }
    }

    ///////////////

    public interface BrowserRecyclerViewAdapterListener {
        public void onBrowseClick(MediaBrowserCompat.MediaItem item);
        public void onAddClick(MediaBrowserCompat.MediaItem item);
    }
}

