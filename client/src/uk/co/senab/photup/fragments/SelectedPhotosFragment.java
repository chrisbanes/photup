package uk.co.senab.photup.fragments;

import uk.co.senab.photup.PhotoSelectionController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.adapters.PhotosBaseAdapter;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.listeners.BitmapCacheProvider;
import uk.co.senab.photup.listeners.OnUploadChangedListener;
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
		SwipeDismissListViewTouchListener.OnDismissCallback, OnUploadChangedListener {

	private BitmapLruCache mCache;
	private PhotosBaseAdapter mAdapter;
	private PhotoSelectionController mPhotoSelectionController;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ListView listView = getListView();
		SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(listView, this);
		listView.setOnTouchListener(touchListener);
		listView.setOnScrollListener(touchListener.makeScrollListener());
	}

	@Override
	public void onAttach(Activity activity) {
		mPhotoSelectionController = PhotupApplication.getApplication(activity).getPhotoSelectionController();
		mCache = ((BitmapCacheProvider) activity).getBitmapCache();
		super.onAttach(activity);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPhotoSelectionController.addPhotoSelectionListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		mAdapter = new PhotosBaseAdapter(getActivity(), mCache);
		setListAdapter(mAdapter);
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPhotoSelectionController.removePhotoSelectionListener(this);
	}

	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
		for (int position : reverseSortedPositions) {
			mPhotoSelectionController.removePhotoUpload((PhotoUpload) listView.getItemAtPosition(position));
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Open Photo Viewer
	}

	@Override
	public void onPause() {
		super.onPause();

		// TODO Save Scroll position
	}

	public void onUploadChanged(PhotoUpload id, boolean added) {
		mAdapter.notifyDataSetChanged();
	}

}