package uk.co.senab.photup.fragments;

import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import uk.co.senab.photup.adapters.PhotosCursorAdapter;
import uk.co.senab.photup.views.PhotupImageView;
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
import android.widget.AdapterView;

@SuppressWarnings("deprecation")
public class UserPhotosFragment extends PhotoGridFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	static final int LOADER_USER_PHOTOS = 0x01;

	private AbsoluteLayout mAnimationLayout;
	private PhotosCursorAdapter mAdapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getLoaderManager().initLoader(LOADER_USER_PHOTOS, null, this);
		mAdapter = new PhotosCursorAdapter(getActivity(), mCache, R.layout.item_user_photo, null, true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		mPhotoGrid.setAdapter(mAdapter);
		mPhotoGrid.setOnItemCheckedListener(this);
		mAdapter.setParentView(mPhotoGrid);

		mAnimationLayout = (AbsoluteLayout) view.findViewById(R.id.al_animation);
		return view;
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
		super.onItemCheckChanged(parent, view, position, id, checked);

		if (checked) {
			animateViewToButton(view);
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
