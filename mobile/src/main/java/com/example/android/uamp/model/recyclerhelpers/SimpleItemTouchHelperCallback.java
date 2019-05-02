package com.example.android.uamp.model.recyclerhelpers;

import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.RecyclerView;
//import androidx.recyclerview.widget.ItemTouchHelper;
//import androidx.recyclerview.widget.RecyclerView;
import com.example.android.uamp.utils.LogHelper;

/**
 * An implementation of {@link ItemTouchHelper.Callback} that enables basic drag & drop and
 * swipe-to-dismiss. Drag events are automatically started by an item long-press.<br/>
 * </br/>
 * Expects the <code>RecyclerView.Adapter</code> to react to {@link
 * ItemTouchHelperAdapter} callbacks and the <code>RecyclerView.ViewHolder</code> to implement
 * {@link ItemTouchHelperViewHolder}.
 *
 * @author Paul Burke (ipaulpro)
 */
public class SimpleItemTouchHelperCallback extends ItemTouchHelper.Callback {
    private static final String TAG = LogHelper.makeLogTag(SimpleItemTouchHelperCallback.class);

    private final ItemTouchHelperAdapter mAdapter;

    public SimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
        mAdapter = adapter;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    // These methods define characteristics of our user interaction (eg " user can swipe")
    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        final int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    private int originalSourcePosition = -1;

    ///////////////////////////////////////////////////////////////////////////////////////
    // These methods will be called when things happen in the GUI
    // We can call methods on the ItemTouchHelperAdapter object which was passed to the constructor
    // in order to do stuff outside in our app
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        LogHelper.i(TAG, "onMove");
        if (originalSourcePosition == -1)
            originalSourcePosition = source.getAdapterPosition();
        mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
        return true;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        LogHelper.i(TAG, "onSwiped");
        mAdapter.onItemDismiss(viewHolder.getAdapterPosition());
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
            ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
            itemViewHolder.onItemSelected();
        }

        super.onSelectedChanged(viewHolder, actionState);
    }

    /**
     * Clearview is Called by the ItemTouchHelper when the user interaction with an element
     * is over and it also completed its animation.
     * @param recyclerView
     * @param viewHolder
     */
    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        int targetPosition = viewHolder.getAdapterPosition();
        LogHelper.i(TAG,"clearView. OriginalSource=", originalSourcePosition," dest=", targetPosition);
        if (originalSourcePosition != -1 && originalSourcePosition != targetPosition) // we were dragging && have dropped somewhere different
        {
            mAdapter.onItemMoveComplete(originalSourcePosition, targetPosition);
        }
        originalSourcePosition = -1;

        ItemTouchHelperViewHolder itemViewHolder = (ItemTouchHelperViewHolder) viewHolder;
        itemViewHolder.onItemClear();

    }
}

