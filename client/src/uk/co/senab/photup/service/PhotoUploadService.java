package uk.co.senab.photup.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.ExecutorService;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotoSelectionActivity;
import uk.co.senab.photup.PhotoSelectionController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.UploadQuality;
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

	static final int MAX_NUMBER_RETRIES = 3;

	static final int NOTIFICATION_ID = 1000;
	static final int MSG_UPLOAD_COMPLETE = 0;
	static final int MSG_UPLOAD_PROGRESS = 1;
	static final String TEMPORARY_FILE_NAME = "upload_temp.jpg";

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
		private final UploadQuality mQuality;

		public UploadPhotoRunnable(Context context, Handler handler, PhotoUpload upload, Session session,
				String albumId, UploadQuality quality) {
			mContextRef = new WeakReference<Context>(context);
			mHandler = handler;
			mUpload = upload;
			mSession = session;
			mAlbumId = albumId;
			mQuality = quality;
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

			/**
			 * Photo
			 */
			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "About to get Upload bitmap");
			}
			Bitmap bitmap = mUpload.getUploadImage(context, mQuality);

			final File temporaryFile = new File(context.getFilesDir(), TEMPORARY_FILE_NAME);
			if (temporaryFile.exists()) {
				temporaryFile.delete();
			}

			try {
				temporaryFile.createNewFile();
				OutputStream os = new BufferedOutputStream(new FileOutputStream(temporaryFile));
				bitmap.compress(CompressFormat.JPEG, mQuality.getJpegQuality(), os);
			} catch (IOException e) {
				e.printStackTrace();
			}
			bitmap.recycle();

			/**
			 * Actual Request
			 */
			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "Starting Facebook Request");
			}

			boolean complete = false;
			int retries = 0;
			do {
				try {
					InputStream is = new ProgressInputStream(new FileInputStream(temporaryFile), temporaryFile.length());
					String response = facebook.request(mAlbumId + "/photos", bundle, "POST", is, "source");
					complete = true;
					
					// TODO Parse Response
					Log.d(LOG_TAG, response);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					retries++;
				}
			} while (!complete && retries < MAX_NUMBER_RETRIES);

			// Delete Temporary File
			temporaryFile.delete();

			// Send complete message
			mHandler.sendMessage(mHandler.obtainMessage(MSG_UPLOAD_COMPLETE, mUpload));
		}

		class ProgressInputStream extends FilterInputStream {
			private final long mInputLength;
			private volatile long mTotalBytesRead;
			private volatile int mPercentageRead;

			public ProgressInputStream(InputStream in, long maxNumBytes) {
				super(in);
				mTotalBytesRead = 0;
				mPercentageRead = 0;
				mInputLength = maxNumBytes;
			}

			@Override
			public int read() throws IOException {
				return updateProgress(super.read());
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return updateProgress(super.read(b, off, len));
			}

			@Override
			public boolean markSupported() {
				return false;
			}

			private int updateProgress(final int numBytesRead) {
				if (numBytesRead > 0) {
					mTotalBytesRead += numBytesRead;
				}

				if (Constants.DEBUG) {
					Log.d(LOG_TAG, "Upload. File length: " + mInputLength + ". Read so far:" + mTotalBytesRead);
				}

				mPercentageRead = Math.round(mTotalBytesRead / mInputLength * 100f);
				Message msg = mHandler.obtainMessage(MSG_UPLOAD_PROGRESS);
				msg.arg1 = mPercentageRead;
				mHandler.sendMessage(msg);

				return numBytesRead;
			}
		}
	}

	private ServiceBinder<PhotoUploadService> mBinder;

	private ExecutorService mExecutor;
	private Session mSession;
	private PhotoSelectionController mController;

	private final Handler mHandler = new Handler(this);
	private int mNumberUploaded = 0;
	private String mAlbumId;
	private UploadQuality mUploadQuality;

	private NotificationManager mNotificationMgr;
	private NotificationCompat.Builder mNotificationBuilder;

	@Override
	public void onCreate() {
		super.onCreate();
		mBinder = new ServiceBinder<PhotoUploadService>(this);

		PhotupApplication app = PhotupApplication.getApplication(this);

		mController = app.getPhotoSelectionController();
		mExecutor = app.getSingleThreadExecutorService();
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
			case MSG_UPLOAD_PROGRESS:
				updateNotification(msg.arg1);
				return true;
		}

		return false;
	}

	public void uploadAll(String albumId, UploadQuality quality) {
		mAlbumId = albumId;
		mUploadQuality = quality;

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
		trimCache();
		updateNotification();
		mExecutor.submit(new UploadPhotoRunnable(this, mHandler, upload, mSession, mAlbumId, mUploadQuality));
	}

	private void onFinishedUpload(PhotoUpload completedUpload) {
		mController.removePhotoUpload(completedUpload);
		mNumberUploaded++;

		PhotoUpload nextUpload = getNextUpload();
		if (null != nextUpload) {
			startUpload(nextUpload);
		} else {
			stopForeground(true);
			finishedNotification();
			stopSelf();
		}
	}

	private void startForeground() {
		if (null == mNotificationBuilder) {
			mNotificationBuilder = new NotificationCompat.Builder(this);
			mNotificationBuilder.setSmallIcon(R.drawable.ic_notification);
			mNotificationBuilder.setContentTitle(getString(R.string.app_name));
			mNotificationBuilder.setOngoing(true);
			mNotificationBuilder.setWhen(System.currentTimeMillis());

			PendingIntent intent = PendingIntent
					.getActivity(this, 0, new Intent(this, PhotoSelectionActivity.class), 0);
			mNotificationBuilder.setContentIntent(intent);
		}

		startForeground(NOTIFICATION_ID, mNotificationBuilder.getNotification());
	}

	private void trimCache() {
		PhotupApplication.getApplication(this).getImageCache().trimMemory();
	}

	void updateNotification() {
		String text = getString(R.string.notification_uploading_photo, mNumberUploaded + 1);

		mNotificationBuilder.setWhen(System.currentTimeMillis());
		mNotificationBuilder.setContentText(text);
		mNotificationBuilder.setTicker(text);

		mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.getNotification());
	}

	void updateNotification(final int progress) {
		String text = getString(R.string.notification_uploading_photo_progress, mNumberUploaded + 1, progress);
		mNotificationBuilder.setContentText(text);
		mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.getNotification());
	}

	void finishedNotification() {
		String text = getResources().getQuantityString(R.plurals.notification_uploaded_photo, mNumberUploaded,
				mNumberUploaded);

		mNotificationBuilder.setOngoing(false);
		mNotificationBuilder.setWhen(System.currentTimeMillis());
		mNotificationBuilder.setContentText(text);
		mNotificationBuilder.setTicker(text);

		mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.getNotification());
	}
}
