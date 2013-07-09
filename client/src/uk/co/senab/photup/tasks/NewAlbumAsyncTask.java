/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.senab.photup.tasks;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.model.Account;

public class NewAlbumAsyncTask extends AsyncTask<String, Void, String> {

    public static interface NewAlbumResultListener {

        public void onNewAlbumCreated(String albumId);
    }

    private final Account mAccount;
    private final WeakReference<NewAlbumResultListener> mListener;

    public NewAlbumAsyncTask(Account account, NewAlbumResultListener listener) {
        mAccount = account;
        mListener = new WeakReference<NewAlbumResultListener>(listener);
    }

    @Override
    protected String doInBackground(String... params) {
        FacebookRequester requester = new FacebookRequester(mAccount);
        return requester.createNewAlbum(params[0], params[1], params[2]);
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        NewAlbumResultListener listener = mListener.get();
        if (null != listener) {
            listener.onNewAlbumCreated(result);
        }
    }

}
