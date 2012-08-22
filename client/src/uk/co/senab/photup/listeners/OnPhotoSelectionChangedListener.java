package uk.co.senab.photup.listeners;

import uk.co.senab.photup.model.PhotoUpload;

public interface OnPhotoSelectionChangedListener {

	void onPhotoSelectionChanged(PhotoUpload upload, boolean added);
	
	void onPhotoSelectionsAdded();

	void onPhotoSelectionsCleared();

	void onUploadsCleared();

}
