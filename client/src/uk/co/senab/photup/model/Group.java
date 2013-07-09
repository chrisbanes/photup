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

@DatabaseTable(tableName = "group")
public class Group extends AbstractFacebookObject {

    static final String LOG_TAG = "Group";

    public static final String GRAPH_FIELDS = "id,name";

    Group() {
        // NO-Arg for Ormlites
    }

    public Group(JSONObject object, Account account) throws JSONException {
        super(object, account);
    }

    public static List<Group> getFromDatabase(Context context, Account account) {
        final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        final String accountId = account.getId();
        List<Group> items = null;

        try {
            final Dao<Group, String> dao = helper.getGroupDao();
            items = dao.query(dao.queryBuilder().orderBy(FIELD_NAME, true).where()
                    .eq(FIELD_ACCOUNT_ID, accountId)
                    .prepare());
        } catch (SQLException e) {
            if (Flags.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }

        return items;
    }

    public static void saveToDatabase(Context context, final List<Group> items, Account account) {
        final DatabaseHelper helper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
        final String accountId = account.getId();

        try {
            final Dao<Group, String> dao = helper.getGroupDao();
            dao.callBatchTasks(new Callable<Void>() {

                public Void call() throws Exception {
                    // Delete all
                    DeleteBuilder<Group, String> deleteBuilder = dao.deleteBuilder();
                    deleteBuilder.where().eq(FIELD_ACCOUNT_ID, accountId);
                    int removed = dao.delete(deleteBuilder.prepare());

                    if (Flags.DEBUG) {
                        Log.d(LOG_TAG, "Deleted " + removed + " from database");
                    }

                    for (Group item : items) {
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
