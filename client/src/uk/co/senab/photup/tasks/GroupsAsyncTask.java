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
import uk.co.senab.photup.model.Group;

public class GroupsAsyncTask extends AsyncTask<Void, Void, List<Group>> {

    public static interface GroupsResultListener extends FacebookErrorListener {

        void onGroupsLoaded(Account account, List<Group> groups);
    }

    private final Account mAccount;
    private final WeakReference<Context> mContext;
    private final WeakReference<GroupsResultListener> mListener;

    public GroupsAsyncTask(Context context, Account account, GroupsResultListener listener) {
        mContext = new WeakReference<Context>(context);
        mAccount = account;
        mListener = new WeakReference<GroupsResultListener>(listener);
    }

    @Override
    protected List<Group> doInBackground(Void... params) {

        FacebookRequester requester = new FacebookRequester(mAccount);
        try {
            return requester.getGroups();
        } catch (FacebookError e) {
            GroupsResultListener listener = mListener.get();
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
    protected void onPostExecute(List<Group> result) {
        super.onPostExecute(result);

        Context context = mContext.get();
        if (null != context) {
            if (null != result) {
                Group.saveToDatabase(context, result, mAccount);
            } else {
                result = Group.getFromDatabase(context, mAccount);
            }
        }

        GroupsResultListener listener = mListener.get();
        if (null != listener && null != result) {
            listener.onGroupsLoaded(mAccount, result);
        }
    }

}
