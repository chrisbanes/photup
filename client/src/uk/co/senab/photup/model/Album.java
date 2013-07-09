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

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import uk.co.senab.photup.DatabaseHelper;
import uk.co.senab.photup.Flags;

@DatabaseTable(tableName = "album")
public class Album extends AbstractFacebookObject {

    static final String LOG_TAG = "Album";

    public static final String GRAPH_FIELDS = "id,name,can_upload,updated_time";

    public static final String FIELD_CAN_UPLOAD = "can_upload";
    public static final String FIELD_UPDATED_TIME = "updated_time";

    @DatabaseField(columnName = FIELD_CAN_UPLOAD)
    private boolean mCanUpload;
    @DatabaseField(columnName = FIELD_UPDATED_TIME)
    private long mUpdatedTime;

    Album() {
        // NO-Arg for Ormlite
    }

    public Album(JSONObject object, Account account) throws JSONException {
        super(object, account);
        mCanUpload = object.getBoolean("can_upload");
        mUpdatedTime = object.getLong("updated_time");
    }

    public boolean canUpload() {
        return mCanUpload;
    }

    public long getUpdatedTime() {
        return mUpdatedTime;
    }

    public static List<Album> getFromDatabase(Context context, Account account) {
        final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        final String accountId = account.getId();
        List<Album> albums = null;

        try {
            final Dao<Album, String> dao = helper.getAlbumDao();
            albums = dao.query(dao.queryBuilder().orderBy(FIELD_UPDATED_TIME, false).where()
                    .eq(FIELD_ACCOUNT_ID, accountId).prepare());
        } catch (SQLException e) {
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }

        return albums;
    }

    public static void saveToDatabase(Context context, final List<Album> items, Account account) {
        final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        final String accountId = account.getId();

        try {
            final Dao<Album, String> dao = helper.getAlbumDao();
            dao.callBatchTasks(new Callable<Void>() {

                public Void call() throws Exception {
                    // Delete all
                    DeleteBuilder<Album, String> deleteBuilder = dao.deleteBuilder();
                    deleteBuilder.where().eq(FIELD_ACCOUNT_ID, accountId);
                    int removed = dao.delete(deleteBuilder.prepare());

                    if (Flags.DEBUG) {
                        Log.d(LOG_TAG, "Deleted " + removed + " from database");
                    }

                    for (Album item : items) {
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
