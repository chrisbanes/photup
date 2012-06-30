package uk.co.senab.photup.fragments;

import java.util.Collection;

import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import uk.co.senab.photup.adapters.PhotosCursorAdapter;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.listeners.BitmapCacheProvider;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.MultiChoiceGridView;
import uk.co.senab.photup.views.MultiChoiceGridView.OnItemCheckedListener;
import uk.co.senab.photup.views.PhotupImageView;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsoluteLayout;

import com.actionbarsherlock.app.SherlockFragment;

@SuppressWarnings("deprecation")
public class UserPhotosFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		OnItemCheckedListener {

	private static class ScaleAnimationListener implements AnimationListener {

		private final PhotupImageView mAnimatedView;

		public ScaleAnimationListener(PhotupImageView view) {
			mAnimatedView = view;
		}

		public void onAnimationEnd(Animation animation) {
			mAnimatedView.setVisibility(View.GONE);
			ViewGroup parent = (ViewGroup) mAnimatedView.getParent();
			parent.removeView(mAnimatedView);
			mAnimatedView.recycleBitmap();
		}

		public void onAnimationRepeat(Animation animation) {
			// NO-OP
		}

		public void onAnimationStart(Animation animation) {
			// NO-OP
		}
	}

	static final int LOADER_USER_PHOTOS = 0x01;

	private BitmapLruCache mCache;

	private MultiChoiceGridView mPhotoGrid;
	private PhotosCursorAdapter mAdapter;
	private Collection<PhotoUpload> mTempPhotoUploads;

	private AbsoluteLayout mAnimationLayout;

	private OnPhotoSelectionChangedListener mSelectionListener;

	@Override
	public void onAttach(Activity activity) {
		mSelectionListener = (OnPhotoSelectionChangedListener) activity;
		mCache = ((BitmapCacheProvider) activity).getBitmapCache();
		super.onAttach(activity);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(LOADER_USER_PHOTOS, null, this);
		mAdapter = new PhotosCursorAdapter(getActivity(), mCache, R.layout.item_user_photo, null, true);
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		String[] projection = { ImageColumns._ID };

		CursorLoader cursorLoader = new CursorLoader(getActivity(), Images.Media.EXTERNAL_CONTENT_URI, projection,
				null, null, Images.Media.DATE_ADDED + " desc");

		return cursorLoader;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_user_photos, null);

		mPhotoGrid = (MultiChoiceGridView) view.findViewById(R.id.gv_users_photos);
		mPhotoGrid.setAdapter(mAdapter);
		mPhotoGrid.setOnItemCheckedListener(this);
		mAdapter.setParentView(mPhotoGrid);

		if (null != mTempPhotoUploads) {
			setSelectedUploads(mTempPhotoUploads);
			mTempPhotoUploads = null;
		}

		mAnimationLayout = (AbsoluteLayout) view.findViewById(R.id.al_animation);
		return view;
	}

	public void onItemCheckChanged(View view, PhotoUpload upload, boolean checked) {
		// Callback to listener
		if (null != mSelectionListener) {
			mSelectionListener.onPhotoChosen(upload, checked);
		}

		if (checked) {
			animateViewToButton(view);
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	public void setSelectedUploads(Collection<PhotoUpload> selectedIds) {
		if (null != mPhotoGrid) {
			mPhotoGrid.setCheckedItems(selectedIds);
			mAdapter.notifyDataSetChanged();
		} else {
			mTempPhotoUploads = selectedIds;
		}
	}

	private void animateViewToButton(View view) {
		// New ImageView with Bitmap of View
		PhotupImageView iv = new PhotupImageView(getActivity());
		iv.setImageBitmap(Utils.drawViewOntoBitmap(view));

		// Align it so that it's directly over the current View
		AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.WRAP_CONTENT,
				AbsoluteLayout.LayoutParams.WRAP_CONTENT, view.getLeft(), view.getTop());
		mAnimationLayout.addView(iv, lp);

		int halfTabHeight = getResources().getDimensionPixelSize(R.dimen.abs__action_bar_default_height) / 2;
		int midSecondTabX = Math.round(mPhotoGrid.getWidth() * 0.75f);

		Animation animaton = Utils.createScaleAnimation(view, mPhotoGrid.getWidth(), mPhotoGrid.getHeight(),
				midSecondTabX, mPhotoGrid.getTop() - halfTabHeight);
		animaton.setAnimationListener(new ScaleAnimationListener(iv));
		iv.startAnimation(animaton);
	}

}
