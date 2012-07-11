package uk.co.senab.photup.fragments;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.adapters.UploadsBaseAdapter;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoSelection;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListFragment;

public class UploadsFragment extends SherlockListFragment implements OnPhotoSelectionChangedListener {

	private PhotoUploadController mPhotoSelectionController;
	private UploadsBaseAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new UploadsBaseAdapter(getActivity());

		mPhotoSelectionController = PhotoUploadController.getFromContext(getActivity());
		mPhotoSelectionController.addPhotoSelectionListener(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setAdapter(mAdapter);
		setListShown(true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPhotoSelectionController.removePhotoSelectionListener(this);
	}

	public void onPhotoSelectionChanged(PhotoSelection upload, boolean added) {
	}

	public void onSelectionsAddedToUploads() {
		mAdapter.notifyDataSetChanged();
	}

}
