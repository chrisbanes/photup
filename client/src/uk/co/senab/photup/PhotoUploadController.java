package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.UploadQuality;
import android.content.Context;

public class PhotoUploadController {

	private final ArrayList<PhotoSelection> mSelectedPhotoList;
	private final ArrayList<PhotoSelection> mUploadingList;

	private final HashSet<OnPhotoSelectionChangedListener> mSelectionChangedListeners;

	public static PhotoUploadController getFromContext(Context context) {
		return PhotupApplication.getApplication(context).getPhotoUploadController();
	}

	PhotoUploadController() {
		mSelectionChangedListeners = new HashSet<OnPhotoSelectionChangedListener>();
		mSelectedPhotoList = new ArrayList<PhotoSelection>();
		mUploadingList = new ArrayList<PhotoSelection>();
	}

	public void addPhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.add(listener);
	}

	public void removePhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.remove(listener);
	}

	public void addPhotoSelection(final PhotoSelection upload) {
		if (!isPhotoUploadSelected(upload)) {
			mSelectedPhotoList.add(upload);

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionChanged(upload, true);
			}
		}
	}

	public void removePhotoSelection(final PhotoSelection upload) {
		if (mSelectedPhotoList.remove(upload)) {

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionChanged(upload, false);
			}
		}
	}

	public boolean isPhotoUploadSelected(PhotoSelection upload) {
		return mSelectedPhotoList.contains(upload);
	}

	public List<PhotoSelection> getSelectedPhotoUploads() {
		return new ArrayList<PhotoSelection>(mSelectedPhotoList);
	}

	public int getSelectedPhotoUploadsSize() {
		return mSelectedPhotoList.size();
	}

	/**
	 * Upload Methods
	 */

	public List<PhotoSelection> getUploadingPhotoUploads() {
		return new ArrayList<PhotoSelection>(mUploadingList);
	}

	public int getActiveUploadsSize() {
		int count = 0;
		for (PhotoSelection upload : mUploadingList) {
			if (upload.getState() != PhotoUpload.STATE_UPLOAD_COMPLETED) {
				count++;
			}
		}
		return count;
	}

	public boolean hasWaitingUploads() {
		for (PhotoSelection upload : mUploadingList) {
			if (upload.getState() == PhotoUpload.STATE_WAITING) {
				return true;
			}
		}
		return false;
	}

	public boolean hasUploads() {
		return !mUploadingList.isEmpty();
	}

	public boolean addPhotoToUploads(PhotoSelection upload) {
		if (null != upload && !mUploadingList.contains(upload)) {
			mUploadingList.add(upload);

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onSelectionsAddedToUploads();
			}

			return true;
		}
		return false;
	}

	public void moveSelectedPhotosToUploads(Album album, UploadQuality quality) {
		mUploadingList.addAll(mSelectedPhotoList);
		mSelectedPhotoList.clear();

		for (PhotoUpload upload : mUploadingList) {
			upload.setUploadParams(album.getId(), quality);
		}

		for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
			l.onSelectionsAddedToUploads();
		}
	}

	public void removePhotoFromUploads(PhotoSelection selection) {
		mUploadingList.remove(selection);

		if (mUploadingList.isEmpty()) {
			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onUploadsCleared();
			}
		}
	}

	public PhotoSelection getNextPhotoToUpload() {
		for (PhotoSelection selection : mUploadingList) {
			if (selection.getState() == PhotoUpload.STATE_WAITING) {
				return selection;
			}
		}
		return null;
	}

}
