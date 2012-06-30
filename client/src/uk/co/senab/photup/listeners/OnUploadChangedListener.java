package uk.co.senab.photup.listeners;

import uk.co.senab.photup.model.PhotoUpload;

public interface OnUploadChangedListener {

	void onUploadChanged(PhotoUpload upload, boolean added);

}
