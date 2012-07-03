package uk.co.senab.photup.fragments;

import uk.co.senab.bitmapcache.R;
import uk.co.senab.photup.PhotoSelectionController;
import uk.co.senab.photup.PhotoViewerActivity;
import uk.co.senab.photup.adapters.PhotosBaseAdapter;
import uk.co.senab.photup.listeners.OnUploadChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.example.android.swipedismiss.SwipeDismissListViewTouchListener;

public class SelectedPhotosFragment extends SherlockFragment implements
		SwipeDismissListViewTouchListener.OnDismissCallback, OnUploadChangedListener, OnItemClickListener {

	private GridView mGridView;
	private PhotosBaseAdapter mAdapter;
	private PhotoSelectionController mPhotoSelectionController;

	@Override
	public void onAttach(Activity activity) {
		mPhotoSelectionController = PhotoSelectionController.getFromContext(activity);
		super.onAttach(activity);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPhotoSelectionController.addPhotoSelectionListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_selected_photos, container, false);
		mAdapter = new PhotosBaseAdapter(getActivity());
		
		mGridView = (GridView) view.findViewById(R.id.gv_selected_photos);
		mGridView.setOnItemClickListener(this);
		mGridView.setAdapter(mAdapter);
		
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		mAdapter.notifyDataSetChanged();
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
	public void onPause() {
		super.onPause();

		// TODO Save Scroll position
	}

	public void onUploadChanged(PhotoUpload id, boolean added) {
		mAdapter.notifyDataSetChanged();
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long arg3) {
		Intent intent = new Intent(getActivity(), PhotoViewerActivity.class);
		intent.putExtra(PhotoViewerActivity.EXTRA_POSITION, position);
		startActivity(intent);
	}

}