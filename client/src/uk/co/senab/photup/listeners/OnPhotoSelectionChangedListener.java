package uk.co.senab.photup.listeners;

import uk.co.senab.photup.model.PhotoUpload;

public interface OnPhotoSelectionChangedListener {

	void onPhotoChosen(PhotoUpload id, boolean added);

}
