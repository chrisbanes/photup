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

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotoSelectionActivity;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.PhotoTag;
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
import com.jakewharton.notificationcompat2.NotificationCompat2.BigPictureStyle;

public class PhotoUploadService extends Service implements Handler.Callback {
	
	public static final String EXTRA_QUALITY = "extra_quality";
	public static final String EXTRA_ALBUM_ID = "extra_album_id";

	static final int MAX_NUMBER_RETRIES = 3;
	static final int NOTIFICATION_ID = 1000;

	static final int MSG_UPLOAD_COMPLETE = 0;
	static final int MSG_UPLOAD_PROGRESS = 1;
	static final int MSG_UPLOAD_FAILED = 2;

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

	private class UpdateBigPictureStyleRunnable implements Runnable {

		private final PhotoSelection mSelection;

		public UpdateBigPictureStyleRunnable(PhotoSelection selection) {
			mSelection = selection;
		}

		public void run() {
			mSelection.setBigPictureNotificationBmp(PhotoUploadService.this,
					mSelection.processBitmap(mSelection.getThumbnailImage(PhotoUploadService.this), true));
			updateNotification(mSelection);
		}

	}

	private static class UploadPhotoRunnable implements Runnable {

		static final String LOG_TAG = "UploadPhotoRunnable";

		private final WeakReference<Context> mContextRef;
		private final Handler mHandler;
		private final Session mSession;
		private final PhotoSelection mUpload;
		private final String mAlbumId;
		private final UploadQuality mQuality;

		public UploadPhotoRunnable(Context context, Handler handler, PhotoSelection upload, Session session,
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

			mUpload.setState(PhotoUpload.STATE_UPLOAD_IN_PROGRESS);

			Facebook facebook = mSession.getFb();
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

			String response = null;
			int retries = 0;
			do {
				try {
					InputStream is = new ProgressInputStream(new FileInputStream(temporaryFile), temporaryFile.length());
					response = facebook.request(mAlbumId + "/photos", bundle, "POST", is, "source");
					if (Constants.DEBUG) {
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
					Util.parseJson(response);
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

				if (Constants.DEBUG) {
					Log.d(LOG_TAG, "Upload. File length: " + mInputLength + ". Read so far:" + mTotalBytesRead);
				}

				Message msg = mHandler.obtainMessage(MSG_UPLOAD_PROGRESS, mUpload);
				msg.arg1 = Math.round((mTotalBytesRead * 100f) / mInputLength);
				mHandler.sendMessage(msg);

				return numBytesRead;
			}
		}
	}

	private ServiceBinder<PhotoUploadService> mBinder;

	private ExecutorService mExecutor;
	private Session mSession;
	private PhotoUploadController mController;

	private final Handler mHandler = new Handler(this);
	private int mNumberUploaded = 0;
	
	private String mAlbumId;
	private String mAlbumName;
	private UploadQuality mUploadQuality;

	private NotificationManager mNotificationMgr;
	private NotificationCompat2.Builder mNotificationBuilder;
	private BigPictureStyle mBigPicStyle;

	private String mNotificationSubtitle;

	@Override
	public void onCreate() {
		super.onCreate();
		mBinder = new ServiceBinder<PhotoUploadService>(this);

		PhotupApplication app = PhotupApplication.getApplication(this);

		mController = app.getPhotoUploadController();
		mExecutor = app.getSingleThreadExecutorService();
		mSession = Session.restore(this);

		mNotificationMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		
		return START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		startService(new Intent(this, PhotoUploadService.class));
		return mBinder;
	}

	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_UPLOAD_COMPLETE:
				onFinishedUpload((PhotoSelection) msg.obj);
				return true;
			case MSG_UPLOAD_FAILED:
				onFailedUpload((PhotoSelection) msg.obj);
				return true;
			case MSG_UPLOAD_PROGRESS:
				// Update the upload progress on the main thread
				PhotoSelection upload = (PhotoSelection) msg.obj;
				upload.setUploadProgress(msg.arg1);

				updateNotification(upload);
				return true;
		}

		return false;
	}

	public void uploadAll(Album album, UploadQuality quality) {
		mAlbumId = album.getId();
		mAlbumName = album.getName();
		mUploadQuality = quality;

		PhotoSelection nextUpload = mController.getNextPhotoToUpload();
		if (null != nextUpload) {
			startForeground();
			startUpload(nextUpload);
		}
	}

	private void startUpload(PhotoSelection upload) {
		trimCache();
		updateNotification(upload);
		mExecutor.submit(new UploadPhotoRunnable(this, mHandler, upload, mSession, mAlbumId, mUploadQuality));
	}

	private void onFinishedUpload(PhotoSelection completedUpload) {
		completedUpload.setState(PhotoUpload.STATE_UPLOAD_COMPLETED);
		mNumberUploaded++;
		startNextUploadOrFinish();
	}

	private void onFailedUpload(PhotoSelection failedUpload) {
		failedUpload.setState(PhotoUpload.STATE_UPLOAD_ERROR);
		mNumberUploaded++;
		startNextUploadOrFinish();
	}

	void startNextUploadOrFinish() {
		PhotoSelection nextUpload = mController.getNextPhotoToUpload();
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
			mNotificationBuilder = new NotificationCompat2.Builder(this);
			mNotificationBuilder.setSmallIcon(R.drawable.ic_stat_upload);
			mNotificationBuilder.setContentTitle(getString(R.string.app_name));
			mNotificationBuilder.setOngoing(true);
			mNotificationBuilder.setWhen(System.currentTimeMillis());

			mNotificationSubtitle = getString(R.string.notification_uploading_album_subtitle, mAlbumName);
			mNotificationBuilder.setContentText(mNotificationSubtitle);

			PendingIntent intent = PendingIntent
					.getActivity(this, 0, new Intent(this, PhotoSelectionActivity.class), 0);
			mNotificationBuilder.setContentIntent(intent);
		}

		if (null == mBigPicStyle) {
			mBigPicStyle = new BigPictureStyle(mNotificationBuilder);
		}

		startForeground(NOTIFICATION_ID, mNotificationBuilder.build());
	}

	private void trimCache() {
		PhotupApplication.getApplication(this).getImageCache().trimMemory();
	}

	void updateNotification(final PhotoSelection upload) {
		String text;

		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN && null == upload.getBigPictureNotificationBmp()) {
			mExecutor.submit(new UpdateBigPictureStyleRunnable(upload));
		}

		switch (upload.getState()) {
			case PhotoUpload.STATE_WAITING:
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

				// TODO Fix ordering when Jake updates lib
				mNotificationBuilder.setProgress(upload.getUploadProgress(), 100, false);
				break;
		}

		mBigPicStyle.setSummaryText(mNotificationSubtitle).bigPicture(upload.getBigPictureNotificationBmp());

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
