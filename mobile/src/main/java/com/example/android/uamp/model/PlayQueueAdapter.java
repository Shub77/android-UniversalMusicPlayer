package com.example.android.uamp.model;

import android.app.Activity;
import android.content.Context;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.android.uamp.R;
import com.example.android.uamp.utils.LogHelper;

import java.util.ArrayList;

/**
 * Created by asbridged on 06/07/2017.
 * An adapter for showing the list of playque items
 * The activity hosting th list MUST implement interface PlayQueueActionsListener to receive callbacks
 * - For example 'remove button'
 */
public class PlayQueueAdapter extends ArrayAdapter<MediaSessionCompat.QueueItem> {

    private static String TAG = "PlayQueueAdapter";

    public interface PlayQueueActionsListener  {
        void onRemoveSongClicked(long queueId, MediaDescriptionCompat description);
        //void onMoveSongToTopClicked(int Position);
    }

    private Context activity; // this activity must implement PlayQueueActionsListener
    private LayoutInflater songInf;

    private static class ViewHolder{
        public ImageButton removeItemButton;
        public TextView title;
        public TextView description;
        public View view;
    }

    public PlayQueueAdapter(Activity context) {
        super(context, R.layout.playqueue_list_item, new ArrayList<MediaSessionCompat.QueueItem>());
        activity = context;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        View vi = convertView;
        ViewHolder holder;
        songInf= LayoutInflater.from(activity);
        if(convertView==null) {
            vi = songInf.inflate(R.layout.playqueue_list_item, null);

            holder = new ViewHolder();
            holder.view = vi;
            holder.removeItemButton = (ImageButton) vi.findViewById(R.id.playqueue_remove_item);
            holder.title = (TextView) vi.findViewById(R.id.title);
            holder.description =(TextView)vi.findViewById(R.id.description);
            vi.setTag( holder );
        } else {
            holder=(ViewHolder)vi.getTag();
        }

        MediaSessionCompat.QueueItem item = getItem(position);
        MediaDescriptionCompat description = item.getDescription();

        holder.title.setText(description.getTitle());
        holder.description.setText(description.getDescription());
        holder.removeItemButton.setOnClickListener(new OnRemoveButtonClickListener(position, item.getDescription().getMediaId(), description, item.getQueueId()));
        return vi;
    }


    class OnRemoveButtonClickListener implements View.OnClickListener {
        int position;
        String mediaId;
        long queueId;
        MediaDescriptionCompat description;

        // constructor
        public OnRemoveButtonClickListener(int position, String mediaId, MediaDescriptionCompat description, long queueId) {
            this.position = position;
            this.mediaId = mediaId;
            this.description = description;
            this.queueId = queueId;
        }
        @Override
        public void onClick(View v) {
            //final Song song = (Song) getItem(songPosition);
            LogHelper.i(TAG, "remove song, queueId = ", queueId, " mediaId=", mediaId);

            // Callback to the activity which must implement PlayQueueActionsListener
            ((PlayQueueActionsListener)activity).onRemoveSongClicked(queueId, description);

        }
    }
}
