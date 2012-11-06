package uk.co.senab.photup.events;

import uk.co.senab.photup.model.PhotoUpload;

public class UploadStateChangedEvent {

	private final PhotoUpload mUpload;

	public UploadStateChangedEvent(PhotoUpload upload) {
		mUpload = upload;
	}

	public PhotoUpload getUpload() {
		return mUpload;
	}

}
