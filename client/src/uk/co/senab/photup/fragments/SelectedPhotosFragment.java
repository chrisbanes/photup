package uk.co.senab.photup.fragments;

import java.util.Collection;

import uk.co.senab.photup.adapters.PhotosBaseAdapter;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.listeners.BitmapCacheProvider;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.listeners.PhotoListDisplayer;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

public class SelectedPhotosFragment extends SherlockListFragment implements PhotoListDisplayer {

	protected BitmapLruCache mCache;

	private PhotosBaseAdapter mAdapter;
	private OnPhotoSelectionChangedListener mSelectionListener;

	private Collection<Long> mSelectedIdsTemp;

	@Override
	public void onAttach(Activity activity) {
		mSelectionListener = (OnPhotoSelectionChangedListener) activity;
		mCache = ((BitmapCacheProvider) activity).getBitmapCache();
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		mAdapter = new PhotosBaseAdapter(getActivity(), mCache);
		setListAdapter(mAdapter);

		if (null != mSelectedIdsTemp) {
			setSelectedPhotos(mSelectedIdsTemp);
			mSelectedIdsTemp = null;
		}

		return view;
	}

	@Override
	public void onPause() {
		super.onPause();

		// TODO Save Scroll position
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		// Callback to listener
		if (null != mSelectionListener) {
			mSelectionListener.onPhotoChosen(id, false);
		}
	}

	public void setSelectedPhotos(Collection<Long> selectedIds) {
		if (null != mAdapter) {
			mAdapter.setItems(selectedIds);
		} else {
			mSelectedIdsTemp = selectedIds;
		}
	}

}