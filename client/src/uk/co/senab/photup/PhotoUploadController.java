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
import uk.co.senab.photup.util.PhotoUploadDatabaseHelper;
import android.content.Context;

public class PhotoUploadController {

	public static PhotoUploadController getFromContext(Context context) {
		return PhotupApplication.getApplication(context).getPhotoUploadController();
	}

	private final Context mContext;
	private final ArrayList<PhotoUpload> mSelectedPhotoList;
	private final ArrayList<PhotoUpload> mUploadingList;

	private final HashSet<OnPhotoSelectionChangedListener> mSelectionChangedListeners;

	PhotoUploadController(Context context) {
		mContext = context;
		mSelectionChangedListeners = new HashSet<OnPhotoSelectionChangedListener>();
		mSelectedPhotoList = new ArrayList<PhotoUpload>();
		mUploadingList = new ArrayList<PhotoUpload>();

		populateFromDatabase();
	}

	public void addPhotoSelection(final PhotoUpload selection) {
		if (!isPhotoUploadSelected(selection)) {
			selection.setUploadState(PhotoUpload.STATE_SELECTED);
			mSelectedPhotoList.add(selection);

			// Save to Database
			if (Flags.ENABLE_DB_PERSISTENCE) {
				PhotoUploadDatabaseHelper.saveToDatabase(mContext, selection);
			}

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionChanged(selection, true);
			}
		}
	}

	public void addPhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.add(listener);
	}

	public void addPhotoSelections(List<PhotoUpload> selections) {
		final HashSet<PhotoUpload> currentSelectionsSet = new HashSet<PhotoUpload>(mSelectedPhotoList);
		boolean listModified = false;

		for (final PhotoUpload selection : selections) {
			if (!currentSelectionsSet.contains(selection)) {
				selection.setUploadState(PhotoUpload.STATE_SELECTED);
				mSelectedPhotoList.add(selection);
				listModified = true;
			}
		}

		if (listModified) {
			// Save to Database
			if (Flags.ENABLE_DB_PERSISTENCE) {
				PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, true);
			}

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionsAdded();
			}
		}
	}

	public boolean addPhotoToUploads(PhotoUpload upload) {
		if (null != upload && !mUploadingList.contains(upload)) {
			mUploadingList.add(upload);

			// Save to Database
			if (Flags.ENABLE_DB_PERSISTENCE) {
				PhotoUploadDatabaseHelper.saveToDatabase(mContext, upload);
			}

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionsCleared();
			}

			return true;
		}
		return false;
	}

	public void clearPhotoSelections() {
		if (!mSelectedPhotoList.isEmpty()) {

			// Delete from Database
			if (Flags.ENABLE_DB_PERSISTENCE) {
				PhotoUploadDatabaseHelper.deleteAllSelected(mContext);
			}

			// Reset States (as may still be in cache)
			for (PhotoUpload upload : mSelectedPhotoList) {
				upload.setUploadState(PhotoUpload.STATE_NONE);
			}

			// Clear from memory
			mSelectedPhotoList.clear();

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionsCleared();
			}
		}
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

	public PhotoUpload getNextPhotoToUpload() {
		for (PhotoUpload selection : mUploadingList) {
			if (selection.getUploadState() == PhotoUpload.STATE_UPLOAD_WAITING) {
				return selection;
			}
		}
		return null;
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

	public boolean hasSelectionsWithPlace() {
		for (PhotoUpload selection : mSelectedPhotoList) {
			if (selection.hasPlace()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasUploads() {
		return !mUploadingList.isEmpty();
	}

	public boolean hasWaitingUploads() {
		for (PhotoUpload upload : mUploadingList) {
			if (upload.getUploadState() == PhotoUpload.STATE_UPLOAD_WAITING) {
				return true;
			}
		}
		return false;
	}

	public boolean isPhotoUploadSelected(PhotoUpload upload) {
		return mSelectedPhotoList.contains(upload);
	}

	public boolean moveFailedToSelected() {
		boolean result = false;

		final Iterator<PhotoUpload> iterator = mUploadingList.iterator();
		PhotoUpload upload;

		while (iterator.hasNext()) {
			upload = iterator.next();

			if (upload.getUploadState() == PhotoUpload.STATE_UPLOAD_ERROR) {
				// Reset State and add to selection list
				upload.setUploadState(PhotoUpload.STATE_SELECTED);
				addPhotoSelection(upload);

				// Remove from Uploading list
				iterator.remove();
				result = true;
			}
		}

		// Update Database, but don't force update
		if (Flags.ENABLE_DB_PERSISTENCE) {
			PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, false);
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

	public void moveSelectedPhotosToUploads(final Account account, final String targetId, final UploadQuality quality,
			final Place place) {

		for (PhotoUpload upload : mSelectedPhotoList) {
			upload.setUploadParams(account, targetId, quality);
			upload.setUploadState(PhotoUpload.STATE_UPLOAD_WAITING);

			if (null != place) {
				upload.setPlace(place);
			}
		}

		// Update Database
		if (Flags.ENABLE_DB_PERSISTENCE) {
			PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, true);
		}

		mUploadingList.addAll(mSelectedPhotoList);
		mSelectedPhotoList.clear();

		for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
			l.onPhotoSelectionsCleared();
		}
	}

	public void populateDatabaseItemsFromAccounts(HashMap<String, Account> accounts) {
		if (!mSelectedPhotoList.isEmpty()) {
			for (PhotoUpload upload : mSelectedPhotoList) {
				upload.populateFromAccounts(accounts);
			}
		}
		if (!mUploadingList.isEmpty()) {
			for (PhotoUpload upload : mUploadingList) {
				upload.populateFromAccounts(accounts);
			}
		}
	}

	public void populateDatabaseItemsFromFriends(HashMap<String, FbUser> friends) {
		if (!mSelectedPhotoList.isEmpty()) {
			for (PhotoUpload upload : mSelectedPhotoList) {
				upload.populateFromFriends(friends);
			}
		}
		if (!mUploadingList.isEmpty()) {
			for (PhotoUpload upload : mUploadingList) {
				upload.populateFromFriends(friends);
			}
		}
	}

	public void populateFromDatabase() {
		if (Flags.ENABLE_DB_PERSISTENCE) {
			final List<PhotoUpload> selectedFromDb = PhotoUploadDatabaseHelper.getSelected(mContext);
			if (null != selectedFromDb) {
				// Should do contains() on each item really...
				mSelectedPhotoList.addAll(selectedFromDb);
				PhotoUpload.populateCache(selectedFromDb);
			}

			final List<PhotoUpload> uploadsFromDb = PhotoUploadDatabaseHelper.getUploads(mContext);
			if (null != uploadsFromDb) {
				// Should do contains() on each item really...
				mUploadingList.addAll(uploadsFromDb);
				PhotoUpload.populateCache(uploadsFromDb);
			}
		}
	}

	public void updateDatabase() {
		if (Flags.ENABLE_DB_PERSISTENCE) {
			PhotoUploadDatabaseHelper.saveToDatabase(mContext, mSelectedPhotoList, false);
			PhotoUploadDatabaseHelper.saveToDatabase(mContext, mUploadingList, false);
		}
	}

	public void removePhotoFromUploads(PhotoUpload selection) {
		mUploadingList.remove(selection);

		if (Flags.ENABLE_DB_PERSISTENCE) {
			PhotoUploadDatabaseHelper.deleteFromDatabase(mContext, selection);
		}

		if (mUploadingList.isEmpty()) {
			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onUploadsCleared();
			}
		}
	}

	public void removePhotoSelection(final PhotoUpload upload) {
		if (mSelectedPhotoList.remove(upload)) {

			// Delete from Database
			if (Flags.ENABLE_DB_PERSISTENCE) {
				PhotoUploadDatabaseHelper.deleteFromDatabase(mContext, upload);
			}

			// Reset State (as may still be in cache)
			upload.setUploadState(PhotoUpload.STATE_NONE);

			for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
				l.onPhotoSelectionChanged(upload, false);
			}
		}
	}

	public void removePhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.remove(listener);
	}

}
