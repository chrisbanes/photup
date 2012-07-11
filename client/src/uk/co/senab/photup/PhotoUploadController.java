package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;

public class PhotoUploadController {

	private final ArrayList<PhotoUpload> mSelectedPhotoList;
	private final HashSet<PhotoUpload> mSelectedPhotoSet;

	private final HashSet<OnPhotoSelectionChangedListener> mSelectionChangedListeners;
	
	private final ArrayList<PhotoUpload> mUploadingList;

	public static PhotoUploadController getFromContext(Context context) {
		return PhotupApplication.getApplication(context).getPhotoUploadController();
	}

	PhotoUploadController() {
		mSelectionChangedListeners = new HashSet<OnPhotoSelectionChangedListener>();
		
		mSelectedPhotoList = new ArrayList<PhotoUpload>();
		mSelectedPhotoSet = new HashSet<PhotoUpload>();
		
		mUploadingList = new ArrayList<PhotoUpload>();
	}

	public void addPhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.add(listener);
	}

	public void removePhotoSelectionListener(OnPhotoSelectionChangedListener listener) {
		mSelectionChangedListeners.remove(listener);
	}

	public void addPhotoSelection(final PhotoUpload upload) {
		if (!isPhotoUploadSelected(upload)) {
			mSelectedPhotoSet.add(upload);
			mSelectedPhotoList.add(upload);
		}

		for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
			l.onPhotoSelectionChanged(upload, true);
		}
	}

	public void removePhotoSelection(final PhotoUpload upload) {
		if (mSelectedPhotoSet.remove(upload)) {
			mSelectedPhotoList.remove(upload);
		}

		for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
			l.onPhotoSelectionChanged(upload, false);
		}
	}

	public boolean isPhotoUploadSelected(PhotoUpload upload) {
		return mSelectedPhotoSet.contains(upload);
	}
	
	public List<PhotoUpload> getSelectedPhotoUploads() {
		return new ArrayList<PhotoUpload>(mSelectedPhotoList);
	}
	
	public void moveSelectedPhotosToUploads() {
		mUploadingList.addAll(mSelectedPhotoList);
		
		mSelectedPhotoList.clear();
		mSelectedPhotoSet.clear();
		
		for (OnPhotoSelectionChangedListener l : mSelectionChangedListeners) {
			l.onPhotoSelectionCleared();
		}
	}

	public int getSelectedPhotoUploadsSize() {
		return mSelectedPhotoList.size();
	}

}
