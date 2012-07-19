package uk.co.senab.photup.model;

import java.lang.ref.WeakReference;

import uk.co.senab.photup.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class PhotoUpload {

	public static interface OnUploadStateChanged {
		void onUploadStateChanged(PhotoUpload upload, int state, int progress);
	}

	public static final int STATE_WAITING = 0;
	public static final int STATE_UPLOAD_IN_PROGRESS = 1;
	public static final int STATE_UPLOAD_COMPLETED = 2;
	public static final int STATE_UPLOAD_ERROR = 3;

	private Bitmap mBigPictureNotificationBmp;

	private WeakReference<OnUploadStateChanged> mStateListener;

	private String mAlbumName;
	private String mAlbumId;
	private UploadQuality mQuality;

	private int mState;
	private int mProgress;

	public PhotoUpload() {
		mState = STATE_WAITING;
	}

	public int getState() {
		return mState;
	}

	public String getAlbumName() {
		return mAlbumName;
	}

	public String getAlbumId() {
		return mAlbumId;
	}

	public UploadQuality getQuality() {
		return null != mQuality ? mQuality : UploadQuality.MEDIUM;
	}

	public void setUploadParams(String albumId, String albumName, UploadQuality quality) {
		mAlbumId = albumId;
		mAlbumName = albumName;
		mQuality = quality;
	}

	public void setState(int state) {
		if (mState != state) {
			mState = state;

			switch (state) {
				case STATE_UPLOAD_ERROR:
				case STATE_UPLOAD_COMPLETED:
					if (null != mBigPictureNotificationBmp) {
						mBigPictureNotificationBmp.recycle();
						mBigPictureNotificationBmp = null;
					}
				case STATE_WAITING:
					mProgress = -1;
					break;
			}

			notifyListeners();
		}
	}

	public int getUploadProgress() {
		return mProgress;
	}

	public void setUploadProgress(int progress) {
		if (progress != mProgress) {
			mProgress = progress;
			notifyListeners();
		}
	}

	public void setUploadStateChangedListener(OnUploadStateChanged listener) {
		mStateListener = new WeakReference<PhotoUpload.OnUploadStateChanged>(listener);
	}

	public void removeUploadStateChangedListener() {
		mStateListener = null;
	}

	private void notifyListeners() {
		if (null == mStateListener) {
			return;
		}

		OnUploadStateChanged listener = mStateListener.get();
		if (null != listener) {
			listener.onUploadStateChanged(this, mState, mProgress);
		}
	}

	public Bitmap getBigPictureNotificationBmp() {
		return mBigPictureNotificationBmp;
	}

	public void setBigPictureNotificationBmp(Context context, Bitmap bigPictureNotificationBmp) {
		if (null == bigPictureNotificationBmp) {
			mBigPictureNotificationBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_logo);
		} else {
			mBigPictureNotificationBmp = bigPictureNotificationBmp;
		}
	}

}
