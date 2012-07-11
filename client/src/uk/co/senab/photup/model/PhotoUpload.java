package uk.co.senab.photup.model;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class PhotoUpload {

	public static interface OnUploadStateChanged {
		void onUploadStateChanged(PhotoUpload upload, int state, int progress);
	}

	public static final int STATE_WAITING = 0;
	public static final int STATE_UPLOAD_IN_PROGRESS = 1;
	public static final int STATE_UPLOAD_COMPLETED = 2;

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

			if (STATE_UPLOAD_IN_PROGRESS != state) {
				mProgress = -1;
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

}
