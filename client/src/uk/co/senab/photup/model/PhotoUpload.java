package uk.co.senab.photup.model;

public class PhotoUpload {

	public static final int STATE_WAITING = 0;
	public static final int STATE_UPLOAD_IN_PROGRESS = 1;
	public static final int STATE_UPLOAD_COMPLETED = 2;

	private int mState;
	private int mProgress;

	public PhotoUpload() {
		mState = STATE_WAITING;
	}

	public int getState() {
		return mState;
	}

	public void setState(int state) {
		mState = state;

		if (STATE_UPLOAD_IN_PROGRESS != state) {
			mProgress = -1;
		}
	}

	public int getUploadProgress() {
		return mProgress;
	}
	
	public void setUploadProgress(int progress) {
		mProgress = progress;
	}

}
