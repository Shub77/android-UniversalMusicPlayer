package com.example.android.uamp.database;

import java.util.List;

/**
 * Created by AsbridgeD on 29-Oct-18.
 */

public interface AsyncResult {

    void asyncFinished(List<QueuedSong> queuedSongs);
}
