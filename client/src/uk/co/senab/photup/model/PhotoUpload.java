package uk.co.senab.photup.model;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

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

	private Set<WeakReference<OnUploadStateChanged>> mStateListeners;

	private int mState;
	private int mProgress;

	public PhotoUpload() {
		mState = STATE_WAITING;
	}

	public int getState() {
		return mState;
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

	public void addUploadStateChangedListener(OnUploadStateChanged listener) {
		if (null == mStateListeners) {
			mStateListeners = new HashSet<WeakReference<OnUploadStateChanged>>();
		}

		mStateListeners.add(new WeakReference<PhotoUpload.OnUploadStateChanged>(listener));
	}

	public void removeUploadStateChangedListener(OnUploadStateChanged listener) {
		if (null == mStateListeners) {
			return;
		}

		WeakReference<OnUploadStateChanged> refToRemove = null;
		for (WeakReference<OnUploadStateChanged> ref : mStateListeners) {
			if (ref.get() == listener) {
				refToRemove = ref;
				break;
			}
		}

		if (null != refToRemove) {
			mStateListeners.remove(refToRemove);
		}

		if (mStateListeners.isEmpty()) {
			mStateListeners = null;
		}
	}

	private void notifyListeners() {
		if (null == mStateListeners) {
			return;
		}

		for (WeakReference<OnUploadStateChanged> ref : mStateListeners) {
			OnUploadStateChanged listener = ref.get();
			if (null != listener) {
				listener.onUploadStateChanged(this, mState, mProgress);
			}
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
