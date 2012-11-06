package uk.co.senab.photup.events;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.model.PhotoUpload;

public class PhotoSelectionRemovedEvent {

	private final List<PhotoUpload> mUploads;

	public PhotoSelectionRemovedEvent(List<PhotoUpload> uploads) {
		mUploads = uploads;
	}

	public PhotoSelectionRemovedEvent(PhotoUpload upload) {
		mUploads = new ArrayList<PhotoUpload>();
		mUploads.add(upload);
	}

	public List<PhotoUpload> getTargets() {
		return mUploads;
	}

	public PhotoUpload getTarget() {
		if (isSingleChange()) {
			return mUploads.get(0);
		} else {
			throw new IllegalStateException("Can only call this when isSingleChange returns true");
		}
	}

	public boolean isSingleChange() {
		return mUploads.size() == 1;
	}

}
