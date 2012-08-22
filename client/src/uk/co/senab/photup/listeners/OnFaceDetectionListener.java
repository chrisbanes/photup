package uk.co.senab.photup.listeners;

import uk.co.senab.photup.model.PhotoUpload;

public interface OnFaceDetectionListener {

	void onFaceDetectionStarted(PhotoUpload selection);

	void onFaceDetectionFinished(PhotoUpload selection);

}
