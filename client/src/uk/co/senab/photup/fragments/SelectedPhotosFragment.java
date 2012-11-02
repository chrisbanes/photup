package uk.co.senab.photup.fragments;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotoViewerActivity;
import uk.co.senab.photup.R;
import uk.co.senab.photup.adapters.SelectedPhotosBaseAdapter;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.util.Utils;
import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.example.android.swipedismiss.SwipeDismissListViewTouchListener;
import com.example.android.swipedismiss.SwipeDismissListViewTouchListener.OnDismissCallback;
import com.jakewharton.activitycompat2.ActivityCompat2;
import com.jakewharton.activitycompat2.ActivityOptionsCompat2;

public class SelectedPhotosFragment extends AbstractPhotosFragment implements OnPhotoSelectionChangedListener,
		OnItemClickListener, OnDismissCallback {

	private GridView mGridView;
	private SelectedPhotosBaseAdapter mAdapter;
	private PhotoUploadController mPhotoSelectionController;

	@Override
	public void onAttach(Activity activity) {
		mPhotoSelectionController = PhotoUploadController.getFromContext(activity);
		super.onAttach(activity);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPhotoSelectionController.addListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup view = (ViewGroup) inflater.inflate(R.layout.fragment_selected_photos, container, false);
		mGridView = (GridView) view.findViewById(R.id.gv_photos);
		mGridView.setOnItemClickListener(this);

		final boolean swipeToDismiss = getResources().getBoolean(R.bool.swipe_selected);
		// Check if we're set to swipe
		if (swipeToDismiss) {
			SwipeDismissListViewTouchListener swipeListener = new SwipeDismissListViewTouchListener(mGridView, this);
			mGridView.setOnTouchListener(swipeListener);
			mGridView.setOnScrollListener(swipeListener.makeScrollListener());
		}

		mAdapter = new SelectedPhotosBaseAdapter(getActivity(), !swipeToDismiss);
		mGridView.setAdapter(mAdapter);

		View emptyView = inflater.inflate(R.layout.layout_empty_user_photos, container, false);
		view.addView(emptyView);
		mGridView.setEmptyView(emptyView);

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
		mPhotoSelectionController.removeListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		// TODO Save Scroll position
	}

	public void onPhotoSelectionsCleared() {
		mAdapter.notifyDataSetChanged();
	}

	public void onPhotoSelectionChanged(PhotoUpload id, boolean added) {
		mAdapter.notifyDataSetChanged();
	}

	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Bundle b = null;
		if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
			ActivityOptionsCompat2 options = ActivityOptionsCompat2.makeThumbnailScaleUpAnimation(view,
					Utils.drawViewOntoBitmap(view), 0, 0);
			b = options.toBundle();
		}

		Intent intent = new Intent(getActivity(), PhotoViewerActivity.class);
		intent.putExtra(PhotoViewerActivity.EXTRA_POSITION, position);
		intent.putExtra(PhotoViewerActivity.EXTRA_MODE, PhotoViewerActivity.MODE_SELECTED_VALUE);

		ActivityCompat2.startActivity(getActivity(), intent, b);
	}

	public void onUploadsCleared() {
		// NO-OP
	}

	public void onPhotoSelectionsAdded() {
		mAdapter.notifyDataSetChanged();
	}

	public boolean canDismiss(AbsListView listView, int position) {
		// All can be swiped
		return true;
	}

	public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
		try {
			for (int i = 0, z = reverseSortedPositions.length; i < z; i++) {
				PhotoUpload upload = (PhotoUpload) listView.getItemAtPosition(reverseSortedPositions[i]);
				mPhotoSelectionController.removeSelection(upload);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		mAdapter.notifyDataSetChanged();
	}

}