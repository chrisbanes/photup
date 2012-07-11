package uk.co.senab.photup.fragments;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.adapters.UploadsBaseAdapter;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.PhotoUpload;
import android.os.Bundle;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.example.android.swipedismiss.SwipeDismissListViewTouchListener;
import com.example.android.swipedismiss.SwipeDismissListViewTouchListener.OnDismissCallback;

public class UploadsFragment extends SherlockListFragment implements OnPhotoSelectionChangedListener, OnDismissCallback {

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

		ListView listView = getListView();

		SwipeDismissListViewTouchListener swipeListener = new SwipeDismissListViewTouchListener(listView, this);
		listView.setOnTouchListener(swipeListener);
		listView.setOnScrollListener(swipeListener.makeScrollListener());

		listView.setAdapter(mAdapter);
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

	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
		for (int i = 0, z = reverseSortedPositions.length; i < z; i++) {
			PhotoSelection upload = (PhotoSelection) listView.getItemAtPosition(reverseSortedPositions[i]);
			mPhotoSelectionController.removePhotoFromUploads(upload);
		}
		mAdapter.notifyDataSetChanged();
	}

	public boolean canDismiss(ListView listView, int position) {
		return ((PhotoUpload) listView.getItemAtPosition(position)).getState() == PhotoUpload.STATE_UPLOAD_COMPLETED;
	}

	public void onUploadsCleared() {
		// NO-OP
	}

}
