package uk.co.senab.photup.util;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.DatabaseHelper;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.DeleteBuilder;

public class PhotoUploadDatabaseHelper {

	public static List<PhotoUpload> getSelected(Context context) {
		final DatabaseHelper helper = getHelper(context);
		List<PhotoUpload> uploads = null;

		try {
			final Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
			uploads = dao.query(dao.queryBuilder().where().eq(PhotoUpload.FIELD_STATE, PhotoUpload.STATE_SELECTED)
					.prepare());
		} catch (SQLException e) {
			if (Constants.DEBUG) {
				e.printStackTrace();
			}
		}

		OpenHelperManager.releaseHelper();
		return uploads;
	}

	public static void deleteAllSelected(Context context) {
		final DatabaseHelper helper = getHelper(context);

		try {
			final Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
			final DeleteBuilder<PhotoUpload, String> deleteBuilder = dao.deleteBuilder();
			deleteBuilder.where().eq(PhotoUpload.FIELD_STATE, PhotoUpload.STATE_SELECTED);
			dao.delete(deleteBuilder.prepare());
		} catch (SQLException e) {
			if (Constants.DEBUG) {
				e.printStackTrace();
			}
		}

		OpenHelperManager.releaseHelper();
	}

	public static void deleteFromDatabase(Context context, PhotoUpload upload) {
		final DatabaseHelper helper = getHelper(context);
		try {
			Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
			dao.delete(upload);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		OpenHelperManager.releaseHelper();
	}

	public static void saveToDatabase(Context context, PhotoUpload upload) {
		final DatabaseHelper helper = getHelper(context);
		try {
			Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();
			dao.createOrUpdate(upload);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		OpenHelperManager.releaseHelper();
	}

	public static void saveToDatabase(Context context, final List<PhotoUpload> uploads, final boolean forceUpdate) {
		final DatabaseHelper helper = getHelper(context);

		try {
			final Dao<PhotoUpload, String> dao = helper.getPhotoUploadDao();

			dao.callBatchTasks(new Callable<Void>() {
				public Void call() throws Exception {

					for (PhotoUpload upload : uploads) {
						if (forceUpdate || upload.requiresSaving()) {
							dao.createOrUpdate(upload);
							upload.resetSaveFlag();
						}
					}
					return null;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		OpenHelperManager.releaseHelper();
	}

	private static DatabaseHelper getHelper(Context context) {
		return OpenHelperManager.getHelper(context, DatabaseHelper.class);
	}

}
