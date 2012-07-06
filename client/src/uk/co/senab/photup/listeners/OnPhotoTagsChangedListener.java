package uk.co.senab.photup.listeners;

import uk.co.senab.photup.model.PhotoTag;

public interface OnPhotoTagsChangedListener {

	void onPhotoTagsChanged(PhotoTag tag, boolean added);

}
