package uk.co.senab.photup.service;

import java.io.File;

import uk.co.senab.photup.model.MediaStorePhotoUpload;
import uk.co.senab.photup.model.PhotoSelection;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

public class InstantUploadService extends Service {

	final static String[] PROJECTION = { Images.Media._ID, Images.Media.DATA };

	private final ContentObserver mObserver = new ContentObserver(null) {

		@Override
		public void onChange(boolean selfChange) {
			Log.d("InstantUploadService", "onChange");
			
			PhotoSelection lastAdded = getLastTakenImage();
		}

		private PhotoSelection getLastTakenImage() {
			Cursor cursor = getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, PROJECTION, null, null,
					Images.Media.DATE_ADDED + " desc");

			PhotoSelection lastAdded = null;
			
			if (cursor.moveToNext()) {
				try {
					File file = new File(cursor.getString(cursor.getColumnIndexOrThrow(ImageColumns.DATA)));
					if (file.exists()) {
						lastAdded = new MediaStorePhotoUpload(Images.Media.EXTERNAL_CONTENT_URI, cursor.getInt(cursor
								.getColumnIndexOrThrow(ImageColumns._ID)));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			cursor.close();

			return lastAdded;
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		getContentResolver().registerContentObserver(Images.Media.EXTERNAL_CONTENT_URI, false, mObserver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getContentResolver().unregisterContentObserver(mObserver);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
