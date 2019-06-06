package com.example.android.uamp.ui.MediaBrowserClient;

import com.example.android.uamp.ui.MediaBrowserProvider;

/**
 * Methods for recycler view with recursive browsing
 */
public interface MediaBrowserClientFragmentListener extends MediaBrowserProvider {

    void onAddBrowsableItemToQueueByMediaIdFromRecyclerView(String trackId);
    void onBrowseItemFromRecyclerView(String trackId, String title);
    void setTitleString(String title);
}
