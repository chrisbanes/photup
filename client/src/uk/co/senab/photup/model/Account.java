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
package uk.co.senab.photup.model;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.DatabaseHelper;
import uk.co.senab.photup.Flags;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.tasks.AlbumsAsyncTask;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;
import uk.co.senab.photup.tasks.EventsAsyncTask;
import uk.co.senab.photup.tasks.EventsAsyncTask.EventsResultListener;
import uk.co.senab.photup.tasks.GroupsAsyncTask;
import uk.co.senab.photup.tasks.GroupsAsyncTask.GroupsResultListener;

@DatabaseTable(tableName = "account")
public class Account extends AbstractFacebookObject
        implements AlbumsResultListener, EventsResultListener,
        GroupsResultListener {

    static final String LOG_TAG = "Account";

    public static final String FIELD_ACCESS_TOKEN = "access_token";
    public static final String FIELD_ACCESS_EXPIRES = "access_expires";
    public static final String FIELD_MAIN_ACCOUNT = "is_main_account";

    @DatabaseField(columnName = FIELD_ACCESS_TOKEN)
    private String mAccessToken;
    @DatabaseField(columnName = FIELD_ACCESS_EXPIRES)
    private long mAccessExpires;
    @DatabaseField(columnName = FIELD_MAIN_ACCOUNT)
    private boolean mIsMainAccount;

    private AlbumsResultListener mAlbumsListener;
    private GroupsResultListener mGroupsListener;
    private EventsResultListener mEventsListener;

    private ArrayList<Album> mAlbums;
    private ArrayList<Event> mEvents;
    private ArrayList<Group> mGroups;

    Account() {
        // No-ARG for Ormlite
    }

    private Account(String id, String name, String accessToken, long accessExpires) {
        super(id, name, null);
        mAccessToken = accessToken;
        mAccessExpires = accessExpires;
        mIsMainAccount = true;
    }

    public Account(JSONObject object) throws JSONException {
        super(object, null);
        mAccessToken = object.optString("access_token", null);
        mAccessExpires = 0;
        mIsMainAccount = false;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public boolean isMainAccount() {
        return mIsMainAccount;
    }

    public void getAlbums(Context context, AlbumsResultListener listener, boolean forceRefresh) {
        if (null == mAlbums) {
            mAlbums = new ArrayList<Album>();
        }

        if (forceRefresh || mAlbums.isEmpty()) {
            mAlbumsListener = listener;
            new AlbumsAsyncTask(context, this, this).execute();
        } else {
            listener.onAlbumsLoaded(this, mAlbums);
        }
    }

    public void getGroups(Context context, GroupsResultListener listener, boolean forceRefresh) {
        if (null == mGroups) {
            mGroups = new ArrayList<Group>();
        }

        if (forceRefresh || mGroups.isEmpty()) {
            mGroupsListener = listener;
            new GroupsAsyncTask(context, this, this).execute();
        } else {
            listener.onGroupsLoaded(this, mGroups);
        }
    }

    public void getEvents(Context context, EventsResultListener listener, boolean forceRefresh) {
        if (null == mEvents) {
            mEvents = new ArrayList<Event>();
        }

        if (forceRefresh || mEvents.isEmpty()) {
            mEventsListener = listener;
            new EventsAsyncTask(context, this, this).execute();
        } else {
            listener.onEventsLoaded(this, mEvents);
        }
    }

    public boolean hasAccessToken() {
        return !TextUtils.isEmpty(mAccessToken);
    }

    public static Account getMeFromSession(Session session) {
        if (null != session) {
            final Facebook fb = session.getFb();
            return new Account(session.getUid(), session.getName(), fb.getAccessToken(),
                    fb.getAccessExpires());
        }
        return null;
    }

    public static Account getAccountFromSession(Context context) {
        return getMeFromSession(Session.restore(context));
    }

    public Facebook getFacebook() {
        Facebook facebook = new Facebook(Constants.FACEBOOK_APP_ID);
        facebook.setAccessToken(mAccessToken);
        facebook.setAccessExpires(mAccessExpires);
        return facebook;
    }

    public void onFacebookError(FacebookError e) {
        // NO-OP
    }

    public void onAlbumsLoaded(Account account, List<Album> albums) {
        mAlbums.clear();

        if (null != albums) {
            mAlbums.addAll(albums);

            if (null != mAlbumsListener && mAlbumsListener != this) {
                mAlbumsListener.onAlbumsLoaded(account, mAlbums);
                mAlbumsListener = null;
            }
        }
    }

    public void onGroupsLoaded(Account account, List<Group> groups) {
        mGroups.clear();

        if (null != groups) {
            mGroups.addAll(groups);
            if (null != mGroupsListener && mGroupsListener != this) {
                mGroupsListener.onGroupsLoaded(account, mGroups);
                mGroupsListener = null;
            }
        }
    }

    public void onEventsLoaded(Account account, List<Event> events) {
        mEvents.clear();

        if (null != events) {
            mEvents.addAll(events);
            if (null != mEventsListener && mEventsListener != this) {
                mEventsListener.onEventsLoaded(account, mEvents);
                mEventsListener = null;
            }
        }
    }

    public void preload(Context context) {
        getAlbums(context, null, false);
        getGroups(context, null, false);
        getEvents(context, null, false);
    }

    public static List<Account> getFromDatabase(Context context) {
        final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        List<Account> items = null;

        try {
            final Dao<Account, String> dao = helper.getAccountDao();
            items = dao.query(dao.queryBuilder().prepare());
        } catch (SQLException e) {
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }

        return items;
    }

    public static void saveToDatabase(Context context, final List<Account> items) {
        final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);

        try {
            final Dao<Account, String> dao = helper.getAccountDao();
            dao.callBatchTasks(new Callable<Void>() {

                public Void call() throws Exception {
                    // Delete all
                    int removed = dao.delete(dao.deleteBuilder().prepare());
                    if (Flags.DEBUG) {
                        Log.d(LOG_TAG, "Deleted " + removed + " from database");
                    }

                    for (Account item : items) {
                        dao.create(item);
                    }
                    if (Flags.DEBUG) {
                        Log.d(LOG_TAG, "Inserted " + items.size() + " into database");
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }
    }

}
