package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.model.UploadQuality;
import android.content.Context;

public class PhotoUploadController {

	private final ArrayList<PhotoUpload> mSelectedPhotoList;
	private final ArrayList<PhotoUpload> mUploadingList;

	private final HashSet<OnPhotoSelectionChangedListener> mSelectionChangedListeners;

	public static PhotoUploadController getFromContext(Context context) {
		return PhotupApplication.getApplication(context).getPhotoUploadController();
	}

	PhotoUploadController() {
		mSelectionChangedListeners = new HashSet<OnPhotoSelectionChangedListener>();
		mSelectedPhotoList = new ArrayList<PhotoUpload>();
		mUploadingList = new ArrayList<PhotoUpload>();
	}

	public void addPhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.add(listener);
	}

	public void removePhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.remove(listener);
	}

	public void populateFromDatabase(Context context) {
		// TODO Populate Lists from Database
	}

	public void populateDatabaseItemsFromFriends(HashMap<String, FbUser> friends) {
		// TODO Populate items
	}
	
	public void populateDatabaseItemsFromAccounts(HashMap<String, Account> accounts) {
		// TODO Populate items
	}

	public void addPhotoSelection(final PhotoUpload upload) {
		if (!isPhotoUploadSelected(upload)) {
			mSelectedPhotoList.add(upload);

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionChanged(upload, true);
			}
		}
	}

	public void addPhotoSelections(List<PhotoUpload> selections) {
		final HashSet<PhotoUpload> currentSelectionsSet = new HashSet<PhotoUpload>(mSelectedPhotoList);
		boolean callListeners = false;

		for (final PhotoUpload selection : selections) {
			if (!currentSelectionsSet.contains(selection)) {
				mSelectedPhotoList.add(selection);
				callListeners = true;
			}
		}

		if (callListeners) {
			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionsAdded();
			}
		}
	}

	public void clearPhotoSelections() {
		if (!mSelectedPhotoList.isEmpty()) {
			mSelectedPhotoList.clear();

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionsCleared();
			}
		}
	}

	public void removePhotoSelection(final PhotoUpload upload) {
		if (mSelectedPhotoList.remove(upload)) {

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionChanged(upload, false);
			}
		}
	}

	public boolean isPhotoUploadSelected(PhotoUpload upload) {
		return mSelectedPhotoList.contains(upload);
	}

	public List<PhotoUpload> getSelectedPhotoUploads() {
		return new ArrayList<PhotoUpload>(mSelectedPhotoList);
	}

	public int getSelectedPhotoUploadsSize() {
		return mSelectedPhotoList.size();
	}

	/**
	 * Upload Methods
	 */

	public List<PhotoUpload> getUploadingPhotoUploads() {
		return new ArrayList<PhotoUpload>(mUploadingList);
	}

	public int getActiveUploadsSize() {
		int count = 0;
		for (PhotoUpload upload : mUploadingList) {
			if (upload.getUploadState() != PhotoUpload.STATE_UPLOAD_COMPLETED) {
				count++;
			}
		}
		return count;
	}

	public boolean hasWaitingUploads() {
		for (PhotoUpload upload : mUploadingList) {
			if (upload.getUploadState() == PhotoUpload.STATE_WAITING) {
				return true;
			}
		}
		return false;
	}

	public boolean hasSelectionsWithPlace() {
		for (PhotoUpload selection : mSelectedPhotoList) {
			if (selection.hasPlace()) {
				return true;
			}
		}
		return false;
	}

	public boolean moveFailedToSelected() {
		boolean result = false;

		final Iterator<PhotoUpload> iterator = mUploadingList.iterator();
		PhotoUpload upload;

		while (iterator.hasNext()) {
			upload = iterator.next();

			if (upload.getUploadState() == PhotoUpload.STATE_UPLOAD_ERROR) {
				// Reset State and add to selection list
				upload.setUploadState(PhotoUpload.STATE_WAITING);
				addPhotoSelection(upload);

				// Remove from Uploading list
				iterator.remove();
				result = true;
			}
		}

		/**
		 * Make sure we call listener if we've emptied the list
		 */
		if (mUploadingList.isEmpty()) {
			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onUploadsCleared();
			}
		}

		return result;
	}

	public boolean hasUploads() {
		return !mUploadingList.isEmpty();
	}

	public boolean addPhotoToUploads(PhotoUpload upload) {
		if (null != upload && !mUploadingList.contains(upload)) {
			mUploadingList.add(upload);

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionsCleared();
			}

			return true;
		}
		return false;
	}

	public void moveSelectedPhotosToUploads(final Account account, final String targetId, final UploadQuality quality,
			final Place place) {
		mUploadingList.addAll(mSelectedPhotoList);
		mSelectedPhotoList.clear();

		for (PhotoUpload upload : mUploadingList) {
			upload.setUploadParams(account, targetId, quality);
			if (null != place) {
				upload.setPlace(place);
			}
		}

		for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
			l.onPhotoSelectionsCleared();
		}
	}

	public void removePhotoFromUploads(PhotoUpload selection) {
		mUploadingList.remove(selection);

		if (mUploadingList.isEmpty()) {
			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onUploadsCleared();
			}
		}
	}

	public PhotoUpload getNextPhotoToUpload() {
		for (PhotoUpload selection : mUploadingList) {
			if (selection.getUploadState() == PhotoUpload.STATE_WAITING) {
				return selection;
			}
		}
		return null;
	}

}
