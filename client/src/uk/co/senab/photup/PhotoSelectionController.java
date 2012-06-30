package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import uk.co.senab.photup.listeners.OnUploadChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;

public class PhotoSelectionController {

	private final ArrayList<PhotoUpload> mPhotoUploadsList;
	private final HashSet<PhotoUpload> mPhotoUploadsHashSet;

	private final HashSet<OnUploadChangedListener> mSelectionChangedListeners;

	public static PhotoSelectionController getFromContext(Context context) {
		return PhotupApplication.getApplication(context).getPhotoSelectionController();
	}

	PhotoSelectionController() {
		mSelectionChangedListeners = new HashSet<OnUploadChangedListener>();
		mPhotoUploadsList = new ArrayList<PhotoUpload>();
		mPhotoUploadsHashSet = new HashSet<PhotoUpload>();
	}

	public void addPhotoSelectionListener(OnUploadChangedListener listener) {
		mSelectionChangedListeners.add(listener);
	}

	public void removePhotoSelectionListener(OnUploadChangedListener listener) {
		mSelectionChangedListeners.remove(listener);
	}

	public void addPhotoUpload(final PhotoUpload upload) {
		if (!isPhotoUploadSelected(upload)) {
			mPhotoUploadsHashSet.add(upload);
			mPhotoUploadsList.add(upload);
		}

		for (OnUploadChangedListener l : mSelectionChangedListeners) {
			l.onUploadChanged(upload, true);
		}
	}

	public void removePhotoUpload(final PhotoUpload upload) {
		if (mPhotoUploadsHashSet.remove(upload)) {
			mPhotoUploadsList.remove(upload);
		}

		for (OnUploadChangedListener l : mSelectionChangedListeners) {
			l.onUploadChanged(upload, false);
		}
	}

	public boolean isPhotoUploadSelected(PhotoUpload upload) {
		return mPhotoUploadsHashSet.contains(upload);
	}
	
	public List<PhotoUpload> getSelectedPhotoUploads() {
		return new ArrayList<PhotoUpload>(mPhotoUploadsList);
	}

	public int getSelectedPhotoUploadsSize() {
		return mPhotoUploadsList.size();
	}

}
