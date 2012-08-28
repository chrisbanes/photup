package uk.co.senab.photup;

import java.sql.SQLException;

import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Database helper class used to manage the creation and upgrading of your
 * database. This class also usually provides the DAOs used by the other
 * classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

	private static final String DATABASE_NAME = "photup.db";
	private static final int DATABASE_VERSION = 3;

	// the DAO object we use to access the PhotoUpload table
	private Dao<PhotoUpload, String> mPhotoUploadDao = null;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * This is called when the database is first created. Usually you should
	 * call createTable statements here to create the tables that will store
	 * your data.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			if (Flags.DEBUG) {
				Log.i(DatabaseHelper.class.getName(), "onCreate");
			}
			TableUtils.createTable(connectionSource, PhotoUpload.class);

		} catch (SQLException e) {
			Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * This is called when your application is upgraded and it has a higher
	 * version number. This allows you to adjust the various data to match the
	 * new version number.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			if (Flags.DEBUG) {
				Log.i(DatabaseHelper.class.getName(), "onUpgrade");
			}
			TableUtils.dropTable(connectionSource, PhotoUpload.class, true);

			onCreate(db, connectionSource);
		} catch (SQLException e) {
			Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the Database Access Object (DAO) for our SimpleData class. It
	 * will create it or just give the cached value.
	 */
	public Dao<PhotoUpload, String> getPhotoUploadDao() throws SQLException {
		if (mPhotoUploadDao == null) {
			mPhotoUploadDao = getDao(PhotoUpload.class);
		}
		return mPhotoUploadDao;
	}

	/**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		mPhotoUploadDao = null;
		super.close();
	}
}