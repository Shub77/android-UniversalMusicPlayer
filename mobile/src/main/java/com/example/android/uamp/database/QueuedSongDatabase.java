package com.example.android.uamp.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Created by AsbridgeD on 29-Oct-18.
 */


@Database(entities = {QueuedSong.class}, version =2)
public abstract class QueuedSongDatabase extends RoomDatabase {

    public abstract QueuedSongDao QueuedSongDao();
    private static QueuedSongDatabase INSTANCE;

    static QueuedSongDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (QueuedSongDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE =
                            Room.databaseBuilder(context.getApplicationContext(),
                                    QueuedSongDatabase.class, "queuedsong_database")
                                    .fallbackToDestructiveMigration()
                                    .build();

                }
            }
        }
        return INSTANCE;
    }
}
