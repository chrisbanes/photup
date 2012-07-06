package uk.co.senab.photup.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotoSelectionActivity;
import uk.co.senab.photup.PhotoSelectionController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.model.PhotoUpload;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.android.Facebook;

public class PhotoUploadService extends Service implements Handler.Callback {

	static final int NOTIFICATION_ID = 1000;
	static final int MSG_UPLOAD_COMPLETE = 0;

	public static class ServiceBinder<S> extends Binder {
		private WeakReference<S> mService;

		public ServiceBinder(final S service) {
			mService = new WeakReference<S>(service);
		}

		public void close() {
			mService.clear();
			mService = null;
		}

		public S getService() {
			return mService.get();
		}
	}

	private static class UploadPhotoRunnable implements Runnable {

		static final String LOG_TAG = "UploadPhotoRunnable";

		private final WeakReference<Context> mContextRef;
		private final Handler mHandler;
		private final Session mSession;
		private final PhotoUpload mUpload;
		private final String mAlbumId;

		public UploadPhotoRunnable(Context context, Handler handler, PhotoUpload upload, Session session, String albumId) {
			mContextRef = new WeakReference<Context>(context);
			mHandler = handler;
			mUpload = upload;
			mSession = session;
			mAlbumId = albumId;
		}

		public void run() {
			Context context = mContextRef.get();
			if (null == context) {
				return;
			}

			Facebook facebook = mSession.getFb();
			Bundle bundle = new Bundle();

			String caption = mUpload.getCaption();
			if (!TextUtils.isEmpty(caption)) {
				bundle.putString("message", caption);
			}

			// TODO ADD PLACE param

			// TODO Make this a choice
			final int largestDimension = Constants.FACEBOOK_MAX_PHOTO_SIZE;

			/**
			 * Photo
			 */
			Bitmap bitmap = mUpload.getUploadImage(context, largestDimension);
			if (mUpload.requiresProcessing()) {
				bitmap = mUpload.processBitmap(bitmap, true);
			}

			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "Finished processing bitmap");
			}

			ByteArrayOutputStream bos = new ByteArrayOutputStream(1024 * 128);
			bitmap.compress(CompressFormat.JPEG, 80, bos);
			bitmap.recycle();
			bundle.putByteArray("source", bos.toByteArray());
			bos = null;

			/**
			 * Actual Request
			 */
			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "Starting Facebook Request");
			}
			try {
				String response = facebook.request(mAlbumId + "/photos", bundle, "POST");
				Log.d(LOG_TAG, response);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Send complete message
			mHandler.sendMessage(mHandler.obtainMessage(MSG_UPLOAD_COMPLETE, mUpload));
		}
	}

	private ServiceBinder<PhotoUploadService> mBinder;

	private ExecutorService mExecutor;
	private Session mSession;
	private PhotoSelectionController mController;

	private final Handler mHandler = new Handler(this);
	private int mNumberUploaded = 0;
	private String mAlbumId;

	private NotificationManager mNotificationMgr;
	private NotificationCompat.Builder mNotificationBuilder;

	@Override
	public void onCreate() {
		super.onCreate();
		mBinder = new ServiceBinder<PhotoUploadService>(this);
		mController = PhotupApplication.getApplication(this).getPhotoSelectionController();
		mExecutor = Executors.newSingleThreadExecutor();
		mSession = Session.restore(this);

		mNotificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public IBinder onBind(Intent intent) {
		startService(new Intent(this, PhotoUploadService.class));
		return mBinder;
	}
	
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_UPLOAD_COMPLETE:
				onFinishedUpload((PhotoUpload) msg.obj);
				return true;
		}

		return false;
	}

	public void uploadAll(String albumId) {
		mAlbumId = albumId;

		PhotoUpload nextUpload = getNextUpload();
		if (null != nextUpload) {
			startForeground();
			startUpload(nextUpload);
		}
	}

	private PhotoUpload getNextUpload() {
		List<PhotoUpload> uploads = mController.getSelectedPhotoUploads();
		if (!uploads.isEmpty()) {
			return uploads.get(0);
		}
		return null;
	}

	private void startUpload(PhotoUpload upload) {
		mExecutor.submit(new UploadPhotoRunnable(this, mHandler, upload, mSession, mAlbumId));
	}

	private void onFinishedUpload(PhotoUpload completedUpload) {
		mController.removePhotoUpload(completedUpload);
		mNumberUploaded++;

		PhotoUpload nextUpload = getNextUpload();
		if (null != nextUpload) {
			startUpload(nextUpload);
		} else {
			stopForeground(false);
			finishedNotification();
			stopSelf();
		}
	}

	private void startForeground() {
		if (null == mNotificationBuilder) {
			mNotificationBuilder = new NotificationCompat.Builder(this);
			mNotificationBuilder.setSmallIcon(R.drawable.ic_launcher);
			mNotificationBuilder.setContentTitle(getString(R.string.app_name));
			mNotificationBuilder.setOngoing(true);
			mNotificationBuilder.setWhen(System.currentTimeMillis());

			PendingIntent intent = PendingIntent
					.getActivity(this, 0, new Intent(this, PhotoSelectionActivity.class), 0);
			mNotificationBuilder.setContentIntent(intent);
		}

		startForeground(NOTIFICATION_ID, mNotificationBuilder.getNotification());
	}

	void updateNotification() {
		mNotificationBuilder.setWhen(System.currentTimeMillis());

		mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.getNotification());
	}

	void finishedNotification() {
		mNotificationBuilder.setOngoing(false);
		mNotificationBuilder.setWhen(System.currentTimeMillis());
		mNotificationBuilder.setContentText("Uploaded " + mNumberUploaded + " photos");

		mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.getNotification());
	}

}
