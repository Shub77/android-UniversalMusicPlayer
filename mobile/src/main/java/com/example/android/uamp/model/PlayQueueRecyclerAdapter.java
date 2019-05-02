package com.example.android.uamp.model;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.uamp.R;
import com.example.android.uamp.model.recyclerhelpers.ItemTouchHelperAdapter;
import com.example.android.uamp.model.recyclerhelpers.ItemTouchHelperViewHolder;
import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by asbridged on 06/07/2017.
 * An adapter for showing the list of playque items
 * The activity hosting th list MUST implement interface PlayQueueActionsListener to receive callbacks
 * - For example 'remove button'
 * This is for the recycler view based list and replaces the PlayQueueAdapter
 */
public class PlayQueueRecyclerAdapter  extends RecyclerView.Adapter<PlayQueueRecyclerAdapter.MyViewHolder>
        implements ItemTouchHelperAdapter
        /* extends ArrayAdapter<MediaSessionCompat.QueueItem> */{

    private static String TAG = "PlayQueueAdapter";

    private ArrayList<MediaSessionCompat.QueueItem> mDataset;

    // this is from https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-6a6f0c422efd
    // It communicates with the adapter's activity (or fragment).
    // I added some extra methods
    public interface OnDragStartListener {
        void onDragStarted(RecyclerView.ViewHolder viewHolder);
        // I added onItemMoveComplete and onItemRemoved
        void onItemMoveComplete(int originalFromPosition, int finalToPosition);
        void onItemRemoved(long uid); // should this be a uid???
    }

    ///////////////////////////////////////////////////////////////////////
    // Methods from ItemTouchHelperAdapter
    ///////////////////////////////////////////////////////////////////////
    @Override
    public void onItemMoveComplete(int originalFromPosition, int finalToPosition) {
        LogHelper.i(TAG, "onItemMoveComplete: from=",originalFromPosition,", to=", finalToPosition);
        mDragStartListener.onItemMoveComplete(originalFromPosition, finalToPosition);
    }


    @Override
    public void onItemDismiss(int position) {
        LogHelper.i(TAG, "onItemDismiss: pos=",position);
        mDragStartListener.onItemRemoved(mDataset.get(position).getQueueId()); //getuniqueQueueId());
        mDataset.remove(position);
        notifyItemRemoved(position); // This tells the recycler _view_ that something been removed

    }


    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        LogHelper.i(TAG, "onItemMove: from=",fromPosition,", to=", toPosition);
        /* Note that on the webpage this doesn't swap... it has the code
       String prev = mItems.remove(fromPosition);
        mItems.add(toPosition > fromPosition ? toPosition - 1 : toPosition, prev);
        notifyItemMoved(fromPosition, toPosition);
         */

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mDataset, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mDataset, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition); // This tells the recycler _view_ that something has moved
    }
    ///////////////////////////////////////////////////////////////////////
    // END Methods from ItemTouchHelperAdapter
    ///////////////////////////////////////////////////////////////////////

    /*
    public interface PlayQueueActionsListener  {
        void onRemoveSongClicked(long queueId, MediaDescriptionCompat description);
        void onMoveSongToTopClicked(long queueId);
    }
    */

    private final OnDragStartListener mDragStartListener;
    // private Context activity;// replaced by dragstartlistener  // this activity must implement PlayQueueActionsListener
    private LayoutInflater songInf;
/*
    private static class ViewHolder{
        public ImageButton removeItemButton;
        public ImageButton moveItemToTopButton;
        public TextView title;
        public TextView description;
        public View view;
    }
*/
    public PlayQueueRecyclerAdapter(ArrayList<MediaSessionCompat.QueueItem> myDataset, OnDragStartListener dragStartListener) {
        mDataset = myDataset;
        mDragStartListener = dragStartListener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public PlayQueueRecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_playlist_song, parent, false);

        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final MyViewHolder  holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        LogHelper.i(TAG, "onBindViewHolder ",position);
        MediaSessionCompat.QueueItem queueItem = mDataset.get(position);
        MediaDescriptionCompat description = queueItem.getDescription();
        holder.tvSongTitle.setText(description.getTitle());
        holder.tvSongArtist.setText(description.getSubtitle());

        holder.handleView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked() /*was deprecate MotionEventCompat.getActionMasked(event)*/) {
                    case MotionEvent.ACTION_DOWN:
                        mDragStartListener.onDragStarted(holder);
                        break;
                    default:
                        LogHelper.i(TAG, "MotionEvent:", event.getActionMasked());
                }

                /* Originally this just had action down to detact start of drag
                // see https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-6a6f0c422efd
                if (MotionEventCompat.getActionMasked(event) ==
                        MotionEvent.ACTION_DOWN) {
                    mDragStartListener.onDragStarted(holder);
                }
                */
                return false;
            }

        });

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder implements
        ItemTouchHelperViewHolder {
        // each data item is just a string in this case
        public final TextView tvSongTitle;
        public final TextView tvSongArtist;
        public final ImageView handleView;

        //for drag handle


        public MyViewHolder(View v) {
            super(v);
            tvSongArtist = (TextView)v.findViewById(R.id.tvRSongArtist);
            tvSongTitle = (TextView)v.findViewById(R.id.tvRSongTitle);
            handleView = (ImageView) itemView.findViewById(R.id.handle);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.LTGRAY);
        }

        @Override
        public void onItemClear() {
            itemView.setBackgroundColor(0);
        }
    }


}
