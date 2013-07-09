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
package uk.co.senab.photup;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.sql.SQLException;

import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.Event;
import uk.co.senab.photup.model.Group;
import uk.co.senab.photup.model.PhotoUpload;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also
 * usually provides the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private static final Class<?>[] DATA_CLASSES = {PhotoUpload.class, Album.class, Event.class,
            Group.class,
            Account.class};

    public static final String DATABASE_NAME = "photup.db";
    private static final int DATABASE_VERSION = 10;

    // the DAO object we use to access the PhotoUpload table
    private Dao<PhotoUpload, String> mPhotoUploadDao = null;
    private Dao<Album, String> mAlbumDao = null;
    private Dao<Event, String> mEventDao = null;
    private Dao<Group, String> mGroupDao = null;
    private Dao<Account, String> mAccountDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable
     * statements here to create the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            if (Flags.DEBUG) {
                Log.i(DatabaseHelper.class.getName(), "onCreate");
            }
            for (Class<?> dataClass : DATA_CLASSES) {
                TableUtils.createTable(connectionSource, dataClass);
            }

        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows
     * you to adjust the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion,
            int newVersion) {
        try {
            if (Flags.DEBUG) {
                Log.i(DatabaseHelper.class.getName(), "onUpgrade");
            }
            for (Class<?> dataClass : DATA_CLASSES) {
                TableUtils.dropTable(connectionSource, dataClass, true);
            }

            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    public Dao<PhotoUpload, String> getPhotoUploadDao() throws SQLException {
        if (mPhotoUploadDao == null) {
            mPhotoUploadDao = getDao(PhotoUpload.class);
        }
        return mPhotoUploadDao;
    }

    public Dao<Album, String> getAlbumDao() throws SQLException {
        if (mAlbumDao == null) {
            mAlbumDao = getDao(Album.class);
        }
        return mAlbumDao;
    }

    public Dao<Event, String> getEventDao() throws SQLException {
        if (mEventDao == null) {
            mEventDao = getDao(Event.class);
        }
        return mEventDao;
    }

    public Dao<Group, String> getGroupDao() throws SQLException {
        if (mGroupDao == null) {
            mGroupDao = getDao(Group.class);
        }
        return mGroupDao;
    }

    public Dao<Account, String> getAccountDao() throws SQLException {
        if (mAccountDao == null) {
            mAccountDao = getDao(Account.class);
        }
        return mAccountDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        mPhotoUploadDao = null;
        mAlbumDao = null;
        mGroupDao = null;
        mEventDao = null;
        super.close();
    }
}