package uk.co.senab.photup.fragments;

import java.util.Collection;

import uk.co.senab.photup.adapters.PhotosBaseAdapter;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.listeners.BitmapCacheProvider;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.example.android.swipedismiss.SwipeDismissListViewTouchListener;

public class SelectedPhotosFragment extends SherlockListFragment implements
		SwipeDismissListViewTouchListener.OnDismissCallback {

	protected BitmapLruCache mCache;

	private PhotosBaseAdapter mAdapter;
	private OnPhotoSelectionChangedListener mSelectionListener;

	private Collection<PhotoUpload> mTempPhotoUploads;

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

		if (null != mTempPhotoUploads) {
			setSelectedUploads(mTempPhotoUploads);
			mTempPhotoUploads = null;
		}

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ListView listView = getListView();

		SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(listView, this);
		listView.setOnTouchListener(touchListener);
		listView.setOnScrollListener(touchListener.makeScrollListener());
	}

	@Override
	public void onPause() {
		super.onPause();

		// TODO Save Scroll position
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

	}

	public void setSelectedUploads(Collection<PhotoUpload> selectedIds) {
		if (null != mAdapter) {
			mAdapter.setItems(selectedIds);
		} else {
			mTempPhotoUploads = selectedIds;
		}
	}

	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
		for (int position : reverseSortedPositions) {
			// Callback to listener
			if (null != mSelectionListener) {
				mSelectionListener.onPhotoChosen((PhotoUpload) listView.getItemAtPosition(position), false);
			}
		}
	}

}