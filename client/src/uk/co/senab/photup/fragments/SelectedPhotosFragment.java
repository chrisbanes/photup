package uk.co.senab.photup.fragments;

import java.util.Collection;

import uk.co.senab.photup.adapters.PhotosBaseAdapter;
import android.os.Bundle;

public class SelectedPhotosFragment extends PhotoGridFragment {

	private PhotosBaseAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new PhotosBaseAdapter(getActivity(), mCache);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mAdapter.setParentView(mPhotoGrid);
		mPhotoGrid.setAdapter(mAdapter);

		setSelectedPhotos(mPhotoGrid.getSelectedIds());
	}

	public void setSelectedPhotos(Collection<Long> selectedIds) {
		if (null != mAdapter) {
			mAdapter.setItems(selectedIds);
		}
		
		super.setSelectedPhotos(selectedIds);
	}

}
