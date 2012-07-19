package uk.co.senab.photup.service;

import java.io.File;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.model.MediaStorePhotoUpload;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.UploadQuality;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

public class InstantUploadService extends Service {

	private static class ImageObserver extends ContentObserver {

		final static String[] PROJECTION = { Images.Media._ID, Images.Media.DATA };

		private final Context mContext;

		public ImageObserver(Context context) {
			super(null);
			mContext = context;
		}

		@Override
		public void onChange(boolean selfChange) {
			Log.d("InstantUploadService", "onChange");

			PhotoSelection lastAdded = getLastTakenImage();
			lastAdded.setUploadParams("625766908580", null, UploadQuality.MEDIUM);
			
			PhotupApplication.getApplication(mContext).getPhotoUploadController().addPhotoToUploads(lastAdded);
			mContext.startService(new Intent(Constants.INTENT_SERVICE_UPLOAD_ALL));
		}

		private PhotoSelection getLastTakenImage() {
			Cursor cursor = mContext.getContentResolver().query(Images.Media.EXTERNAL_CONTENT_URI, PROJECTION, null,
					null, Images.Media.DATE_ADDED + " desc");

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

	private ContentObserver mObserver;

	@Override
	public void onCreate() {
		super.onCreate();
		mObserver = new ImageObserver(this);
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
