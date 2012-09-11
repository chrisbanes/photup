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
import java.util.concurrent.ExecutorService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotoSelectionActivity;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.model.PhotoTag;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.receivers.ConnectivityReceiver;
import uk.co.senab.photup.util.PhotoUploadDatabaseHelper;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.jakewharton.notificationcompat2.NotificationCompat2;

public class PhotoUploadService extends Service implements Handler.Callback {

	static final int MAX_NUMBER_RETRIES = 3;
	static final int NOTIFICATION_ID = 1000;

	static final int MSG_UPLOAD_COMPLETE = 0;
	static final int MSG_UPLOAD_PROGRESS = 1;
	static final int MSG_UPLOAD_FAILED = 2;

	static final String TEMPORARY_FILE_NAME = "upload_temp.jpg";

	private class UpdateBigPictureStyleRunnable implements Runnable {

		private final PhotoUpload mSelection;

		public UpdateBigPictureStyleRunnable(PhotoUpload selection) {
			mSelection = selection;
		}

		public void run() {
			mSelection.setBigPictureNotificationBmp(PhotoUploadService.this,
					mSelection.processBitmap(mSelection.getThumbnailImage(PhotoUploadService.this), false, true));
			updateNotification(mSelection);
		}

	}

	private static class UploadPhotoRunnable implements Runnable {

		static final String LOG_TAG = "UploadPhotoRunnable";

		private final WeakReference<Context> mContextRef;
		private final Handler mHandler;
		private final PhotoUpload mUpload;

		public UploadPhotoRunnable(Context context, Handler handler, PhotoUpload upload, Session session) {
			mContextRef = new WeakReference<Context>(context);
			mHandler = handler;
			mUpload = upload;
		}

		public void run() {
			Context context = mContextRef.get();
			if (null == context) {
				return;
			}

			mUpload.setUploadState(PhotoUpload.STATE_UPLOAD_IN_PROGRESS);

			Facebook facebook = mUpload.getAccount().getFacebook();
			Bundle bundle = new Bundle();

			String caption = mUpload.getCaption();
			if (!TextUtils.isEmpty(caption)) {
				bundle.putString("message", caption);
			}

			/**
			 * Photo Tags
			 */
			if (mUpload.getPhotoTagsCount() > 0) {
				JSONArray tags = new JSONArray();
				for (PhotoTag tag : mUpload.getPhotoTags()) {
					if (tag.hasFriend()) {
						try {
							tags.put(tag.toJsonObject());
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}

				if (tags.length() > 0) {
					bundle.putString("tags", tags.toString());
				}
			}

			if (mUpload.hasPlace()) {
				bundle.putString("place", mUpload.getPlaceId());
			}

			/**
			 * Photo
			 */
			if (Flags.DEBUG) {
				Log.d(LOG_TAG, "About to get Upload bitmap");
			}
			UploadQuality quality = mUpload.getUploadQuality();
			Bitmap bitmap = mUpload.getUploadImage(context, quality);

			final File temporaryFile = new File(context.getFilesDir(), TEMPORARY_FILE_NAME);
			if (temporaryFile.exists()) {
				temporaryFile.delete();
			}

			OutputStream os = null;
			try {
				temporaryFile.createNewFile();
				os = new BufferedOutputStream(new FileOutputStream(temporaryFile));
				bitmap.compress(CompressFormat.JPEG, quality.getJpegQuality(), os);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (null != os) {
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			bitmap.recycle();

			/**
			 * Actual Request
			 */
			if (Flags.DEBUG) {
				Log.d(LOG_TAG, "Starting Facebook Request");
			}

			String response = null;
			int retries = 0;
			do {
				try {
					InputStream is = new ProgressInputStream(new FileInputStream(temporaryFile), temporaryFile.length());

					String targetId = mUpload.getUploadTargetId();
					String graphPath = null != targetId ? targetId : "me";

					response = facebook.request(graphPath + "/photos", bundle, "POST", is, "source");
					if (Flags.DEBUG) {
						Log.d(LOG_TAG, response);
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					retries++;
				}
			} while (response == null && retries < MAX_NUMBER_RETRIES);

			// Delete Temporary File
			temporaryFile.delete();

			if (null != response) {
				try {
					JSONObject object = Util.parseJson(response);
					mUpload.setResultPostId(object.optString("post_id", null));

					// If we get here, we've successfully uploaded the photos
					mHandler.sendMessage(mHandler.obtainMessage(MSG_UPLOAD_COMPLETE, mUpload));
					return;
				} catch (FacebookError e) {
					e.printStackTrace();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			// If we get here, we've errored somewhere
			mHandler.sendMessage(mHandler.obtainMessage(MSG_UPLOAD_FAILED, mUpload));
		}

		class ProgressInputStream extends FilterInputStream {
			private final long mInputLength;
			private volatile long mTotalBytesRead;

			public ProgressInputStream(InputStream in, long maxNumBytes) {
				super(in);
				mTotalBytesRead = 0;
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

				if (Flags.DEBUG) {
					Log.d(LOG_TAG, "Upload. File length: " + mInputLength + ". Read so far:" + mTotalBytesRead);
				}

				Message msg = mHandler.obtainMessage(MSG_UPLOAD_PROGRESS, mUpload);
				msg.arg1 = Math.round((mTotalBytesRead * 100f) / mInputLength);
				mHandler.sendMessage(msg);

				return numBytesRead;
			}
		}
	}

	private boolean mCurrentlyUploading = false;
	private ExecutorService mExecutor;
	private Session mSession;
	private PhotoUploadController mController;

	private final Handler mHandler = new Handler(this);
	private int mNumberUploaded = 0;

	private NotificationManager mNotificationMgr;
	private NotificationCompat2.Builder mNotificationBuilder;
	private NotificationCompat2.BigPictureStyle mBigPicStyle;

	private String mNotificationSubtitle;

	@Override
	public void onCreate() {
		super.onCreate();

		PhotupApplication app = PhotupApplication.getApplication(this);

		mController = app.getPhotoUploadController();
		mExecutor = app.getSingleThreadExecutorService();
		mSession = Session.restore(this);

		mNotificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (null != intent && Constants.INTENT_SERVICE_UPLOAD_ALL.equals(intent.getAction())) {
			uploadAll();
		}

		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_UPLOAD_COMPLETE:
				onFinishedUpload((PhotoUpload) msg.obj);
				return true;
			case MSG_UPLOAD_FAILED:
				onFailedUpload((PhotoUpload) msg.obj);
				return true;
			case MSG_UPLOAD_PROGRESS:
				// Update the upload progress on the main thread
				PhotoUpload upload = (PhotoUpload) msg.obj;
				upload.setUploadProgress(msg.arg1);

				updateNotification(upload);
				return true;
		}

		return false;
	}

	private void uploadAll() {
		// If we're currently uploading, ignore call
		if (mCurrentlyUploading) {
			return;
		}

		if (ConnectivityReceiver.isConnected(this)) {
			PhotoUpload nextUpload = mController.getNextUpload();
			if (null != nextUpload) {
				startForeground();
				startUpload(nextUpload);
			}
		} else {
			// No need to keep us running
			stopSelf();
		}
	}

	private void startUpload(PhotoUpload upload) {
		trimCache();
		updateNotification(upload);
		mExecutor.submit(new UploadPhotoRunnable(this, mHandler, upload, mSession));
		mCurrentlyUploading = true;
	}

	private void onFinishedUpload(PhotoUpload completedUpload) {
		completedUpload.setUploadState(PhotoUpload.STATE_UPLOAD_COMPLETED);
		if (Flags.ENABLE_DB_PERSISTENCE) {
			PhotoUploadDatabaseHelper.saveToDatabase(getApplicationContext(), completedUpload);
		}

		mNumberUploaded++;
		startNextUploadOrFinish();
	}

	private void onFailedUpload(PhotoUpload failedUpload) {
		// If we're connected and failed, set our state to error, else just
		// return us to the queue
		if (ConnectivityReceiver.isConnected(this)) {
			failedUpload.setUploadState(PhotoUpload.STATE_UPLOAD_ERROR);
		} else {
			failedUpload.setUploadState(PhotoUpload.STATE_UPLOAD_WAITING);
		}
		if (Flags.ENABLE_DB_PERSISTENCE) {
			PhotoUploadDatabaseHelper.saveToDatabase(getApplicationContext(), failedUpload);
		}

		startNextUploadOrFinish();
	}

	void startNextUploadOrFinish() {
		PhotoUpload nextUpload = mController.getNextUpload();
		if (ConnectivityReceiver.isConnected(this) && null != nextUpload) {
			startUpload(nextUpload);
		} else {
			mCurrentlyUploading = false;
			stopForeground(true);
			finishedNotification();
			stopSelf();
		}
	}

	private void startForeground() {
		if (null == mNotificationBuilder) {
			mNotificationBuilder = new NotificationCompat2.Builder(this);
			mNotificationBuilder.setSmallIcon(R.drawable.ic_stat_upload);
			mNotificationBuilder.setContentTitle(getString(R.string.app_name));
			mNotificationBuilder.setOngoing(true);
			mNotificationBuilder.setWhen(System.currentTimeMillis());

			PendingIntent intent = PendingIntent
					.getActivity(this, 0, new Intent(this, PhotoSelectionActivity.class), 0);
			mNotificationBuilder.setContentIntent(intent);
		}

		if (null == mBigPicStyle) {
			mBigPicStyle = new NotificationCompat2.BigPictureStyle(mNotificationBuilder);
		}

		startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
	}

	private void trimCache() {
		PhotupApplication.getApplication(this).getImageCache().trimMemory();
	}

	void updateNotification(final PhotoUpload upload) {
		String text;

		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
			final Bitmap uploadBigPic = upload.getBigPictureNotificationBmp();

			if (null == uploadBigPic) {
				mExecutor.submit(new UpdateBigPictureStyleRunnable(upload));
			}
			mBigPicStyle.bigPicture(uploadBigPic);
		}

		switch (upload.getUploadState()) {
			case PhotoUpload.STATE_UPLOAD_WAITING:
				text = getString(R.string.notification_uploading_photo, mNumberUploaded + 1);
				mNotificationBuilder.setContentTitle(text);
				mNotificationBuilder.setTicker(text);
				mNotificationBuilder.setProgress(0, 0, true);
				mNotificationBuilder.setWhen(System.currentTimeMillis());
				break;

			case PhotoUpload.STATE_UPLOAD_IN_PROGRESS:
				text = getString(R.string.notification_uploading_photo_progress, mNumberUploaded + 1,
						upload.getUploadProgress());
				mNotificationBuilder.setContentTitle(text);
				mNotificationBuilder.setProgress(100, upload.getUploadProgress(), false);
				break;
		}

		mBigPicStyle.setSummaryText(mNotificationSubtitle);

		mNotificationMgr.notify(NOTIFICATION_ID, mBigPicStyle.build());
	}

	void finishedNotification() {
		String text = getResources().getQuantityString(R.plurals.notification_uploaded_photo, mNumberUploaded,
				mNumberUploaded);

		mNotificationBuilder.setOngoing(false);
		mNotificationBuilder.setProgress(100, 100, false);
		mNotificationBuilder.setWhen(System.currentTimeMillis());
		mNotificationBuilder.setContentTitle(text);
		mNotificationBuilder.setTicker(text);

		mNotificationMgr.notify(NOTIFICATION_ID, mNotificationBuilder.build());
	}
}
