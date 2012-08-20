package uk.co.senab.photup.listeners;

import uk.co.senab.photup.model.PhotoSelection;

public interface OnPhotoSelectionChangedListener {

	void onPhotoSelectionChanged(PhotoSelection upload, boolean added);

	void onPhotoSelectionsCleared();

	void onUploadsCleared();

}
