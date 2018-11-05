package com.example.android.uamp.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by AsbridgeD on 29-Oct-18.
 */

@Dao
public interface QueuedSongDao {

    @Insert
    void insertQueuedSong(QueuedSong song);

    @Query ("Select * from queuedsongs Order BY queueorder")
    List<QueuedSong> getAllQueuedSongs();

    @Query ("Select * from queuedsongs WHERE queueorder = :queueorder")
    QueuedSong getQueuedSongByOrder(int queueorder);


    @Query ("Delete From queuedsongs WHERE queueorder = :queueorder")
    void deleteQueuedSong(int queueorder);

}
