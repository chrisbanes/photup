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

import com.facebook.android.FacebookError;

import org.json.JSONException;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.List;

import uk.co.senab.photup.facebook.FacebookRequester;
import uk.co.senab.photup.listeners.FacebookErrorListener;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;

public class AlbumsAsyncTask extends AsyncTask<Void, Void, List<Album>> {

    public static interface AlbumsResultListener extends FacebookErrorListener {

        void onAlbumsLoaded(Account account, List<Album> albums);
    }

    private final Account mAccount;
    private final WeakReference<Context> mContext;
    private final WeakReference<AlbumsResultListener> mListener;

    public AlbumsAsyncTask(Context context, Account account, AlbumsResultListener listener) {
        mContext = new WeakReference<Context>(context);
        mAccount = account;
        mListener = new WeakReference<AlbumsResultListener>(listener);
    }

    @Override
    protected List<Album> doInBackground(Void... params) {

        FacebookRequester requester = new FacebookRequester(mAccount);
        try {
            return requester.getUploadableAlbums();
        } catch (FacebookError e) {
            AlbumsResultListener listener = mListener.get();
            if (null != listener) {
                listener.onFacebookError(e);
            } else {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(List<Album> result) {
        super.onPostExecute(result);

        Context context = mContext.get();
        if (null != context) {
            if (null != result) {
                Album.saveToDatabase(context, result, mAccount);
            } else {
                result = Album.getFromDatabase(context, mAccount);
            }
        }

        AlbumsResultListener listener = mListener.get();
        if (null != listener && null != result) {
            listener.onAlbumsLoaded(mAccount, result);
        }
    }

}
