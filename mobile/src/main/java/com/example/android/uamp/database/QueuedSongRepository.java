package com.example.android.uamp.database;

import android.arch.lifecycle.MutableLiveData;
import android.os.AsyncTask;

import java.util.List;

/**
 * Created by AsbridgeD on 29-Oct-18.
 */

public class QueuedSongRepository implements AsyncResult {

    private MutableLiveData<List<QueuedSong>> searchResults =
            new MutableLiveData<>();

    @Override
    public void asyncFinished(List<QueuedSong> results){
        searchResults.setValue(results);
    }

    private static class queryAsyncTask extends
            AsyncTask<Integer, Void, List<QueuedSong>> {

        private QueuedSongDao asyncTaskDao;
        private QueuedSongRepository delegate = null;

        queryAsyncTask(QueuedSongDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected List<QueuedSong> doInBackground(final Integer... params) {
            int o = params[0];
            return asyncTaskDao.getAllQueuedSongs();
        }

        @Override
        protected void onPostExecute(List<QueuedSong> result) {
            delegate.asyncFinished(result);
        }
    }

    private static class insertAsyncTask extends AsyncTask<QueuedSong, Void, Void> {

        private QueuedSongDao asyncTaskDao;

        insertAsyncTask(QueuedSongDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final QueuedSong... params) {
            asyncTaskDao.insertQueuedSong(params[0]);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<Integer, Void, Void> {

        private QueuedSongDao asyncTaskDao;

        deleteAsyncTask(QueuedSongDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Integer... params) {
            int o = params[0];
            asyncTaskDao.deleteQueuedSong(params[0]);
            return null;
        }
    }

}

