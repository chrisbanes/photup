package uk.co.senab.photup.fragments;

import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import uk.co.senab.photup.adapters.PhotosAdapter;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsoluteLayout;
import android.widget.AdapterView;

import com.actionbarsherlock.app.SherlockFragment;

@SuppressWarnings("deprecation")
public class UserPhotosFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		OnItemCheckedListener {

	static final int LOADER_USER_PHOTOS = 0x01;

	private MultiChoiceGridView mPhotoGrid;
	private AbsoluteLayout mAnimationLayout;
	private PhotosAdapter mAdapter;
	private OnPhotoSelectionChangedListener mSelectionListener;

	@Override
	public void onAttach(Activity activity) {
		mSelectionListener = (OnPhotoSelectionChangedListener) activity;
		super.onAttach(activity);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(LOADER_USER_PHOTOS, null, this);
		mAdapter = new PhotosAdapter(getActivity(), R.layout.item_user_photo, null, true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_user_photos, null);

		mAnimationLayout = (AbsoluteLayout) view.findViewById(R.id.al_animation);

		mPhotoGrid = (MultiChoiceGridView) view.findViewById(R.id.gv_users_photos);
		mPhotoGrid.setAdapter(mAdapter);
		mPhotoGrid.setOnItemCheckedListener(this);
		mAdapter.setParentView(mPhotoGrid);

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mAdapter.cleanup();
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		String[] projection = { ImageColumns._ID };

		CursorLoader cursorLoader = new CursorLoader(getActivity(), Images.Media.EXTERNAL_CONTENT_URI, projection,
				null, null, Images.Media.DATE_ADDED + " desc");

		return cursorLoader;

	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	public void onItemCheckChanged(AdapterView<?> parent, View view, int position, long id, boolean checked) {
		StringBuffer msg = new StringBuffer();
		msg.append("onItemCheckChanged: ");
		msg.append(checked ? "Added " : "Removed ");
		msg.append(id);

		Log.d("UserPhotosFragment", msg.toString());

		if (checked) {
			animateViewToButton(view);
		}

		// Callback to listener
		if (null != mSelectionListener) {
			mSelectionListener.onPhotoChosen(id, checked);
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

		// FIXME Needs fixing really
		int abItemHeight = getResources().getDimensionPixelSize(R.dimen.abs__action_bar_default_height);
		int abItemWidth = getResources().getDimensionPixelSize(R.dimen.abs__action_button_min_width);

		Animation animaton = Utils.createScaleAnimation(view, mPhotoGrid.getWidth(), mPhotoGrid.getHeight(),
				mPhotoGrid.getRight() - Math.round(abItemWidth * 1.5f), mPhotoGrid.getTop() - abItemHeight);
		animaton.setAnimationListener(new ScaleAnimationListener(iv));
		iv.startAnimation(animaton);
	}

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

}
