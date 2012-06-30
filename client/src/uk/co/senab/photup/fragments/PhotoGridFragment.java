package uk.co.senab.photup.fragments;

import java.util.Collection;

import uk.co.senab.photup.R;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.listeners.BitmapCacheProvider;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.listeners.PhotoListDisplayer;
import uk.co.senab.photup.views.MultiChoiceGridView;
import uk.co.senab.photup.views.MultiChoiceGridView.OnItemCheckedListener;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class PhotoGridFragment extends SherlockFragment implements OnItemCheckedListener, PhotoListDisplayer {

	protected MultiChoiceGridView mPhotoGrid;
	protected BitmapLruCache mCache;

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
		View view = inflater.inflate(R.layout.fragment_user_photos, null);

		mPhotoGrid = (MultiChoiceGridView) view.findViewById(R.id.gv_users_photos);
		mPhotoGrid.setOnItemCheckedListener(this);

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

	public void onItemCheckChanged(View view, long id, boolean checked) {
		// Callback to listener
		if (null != mSelectionListener) {
			mSelectionListener.onPhotoChosen(id, checked);
		}
	}

	public void setSelectedPhotos(Collection<Long> selectedIds) {
		if (null != mPhotoGrid) {
			mPhotoGrid.setCheckedItems(selectedIds);
		} else {
			mSelectedIdsTemp = selectedIds;
		}
	}

}

