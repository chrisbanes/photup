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

	public static final int STATE_UPLOAD_COMPLETED = 2;
	public static final int STATE_UPLOAD_ERROR = 3;
	public static final int STATE_UPLOAD_IN_PROGRESS = 1;
	public static final int STATE_WAITING = 0;

	private Account mAccount;
	private String mAlbumId;
	private Bitmap mBigPictureNotificationBmp;
	private Place mPlace;
	private int mProgress;
	private UploadQuality mQuality;
	private String mResultPostId;
	private int mState;
	
	private WeakReference<OnUploadStateChanged> mStateListener;

	public PhotoUpload() {
		mState = STATE_WAITING;
	}

	public Account getAccount() {
		return mAccount;
	}

	public String getAlbumId() {
		return mAlbumId;
	}

	public Bitmap getBigPictureNotificationBmp() {
		return mBigPictureNotificationBmp;
	}
	
	public Place getPlace() {
		return mPlace;
	}

	public UploadQuality getQuality() {
		return null != mQuality ? mQuality : UploadQuality.MEDIUM;
	}

	public String getResultPostId() {
		return mResultPostId;
	}

	public int getState() {
		return mState;
	}
	
	public int getUploadProgress() {
		return mProgress;
	}
	
	public void removeUploadStateChangedListener() {
		mStateListener = null;
	}

	public void setBigPictureNotificationBmp(Context context, Bitmap bigPictureNotificationBmp) {
		if (null == bigPictureNotificationBmp) {
			mBigPictureNotificationBmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_logo);
		} else {
			mBigPictureNotificationBmp = bigPictureNotificationBmp;
		}
	}

	public void setPlace(Place place) {
		mPlace = place;
	}

	public void setResultPostId(String resultPostId) {
		mResultPostId = resultPostId;
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

	public void setUploadParams(Account account, String albumId, UploadQuality quality) {
		mAccount = account;
		mAlbumId = albumId;
		mQuality = quality;
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

	private void notifyListeners() {
		if (null == mStateListener) {
			return;
		}

		OnUploadStateChanged listener = mStateListener.get();
		if (null != listener) {
			listener.onUploadStateChanged(this, mState, mProgress);
		}
	}

}
