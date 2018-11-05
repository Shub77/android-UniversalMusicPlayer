package com.example.android.uamp.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import io.reactivex.annotations.NonNull;

/**
 * Created by AsbridgeD on 29-Oct-18.
 */

@Entity(tableName = "queuedsongs")
public class QueuedSong {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "queueorder")
    private int queueorder;

    @NonNull
    @ColumnInfo(name = "trackId")
    private int trackId;


    public QueuedSong(@NonNull int queueorder, @NonNull int trackId) {
        this.trackId = trackId;
        this.queueorder = queueorder;
    }

    public int getQueueorder() {
        return this.queueorder;
    }

    public int getTrackId() {
        return this.trackId;
    }

    public void setOrder(int queueorder) {
        this.queueorder = queueorder;
    }

    public void setTrackID(int trackId) {
        this.trackId = trackId;
    }


}
