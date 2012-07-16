package uk.co.senab.photup.listeners;

import uk.co.senab.photup.model.PhotoSelection;

public interface OnFaceDetectionListener {

	void onFaceDetectionStarted(PhotoSelection selection);

	void onFaceDetectionFinished(PhotoSelection selection);

}
