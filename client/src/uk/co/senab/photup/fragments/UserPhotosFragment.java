package uk.co.senab.photup.fragments;

import java.io.File;

import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import uk.co.senab.photup.adapters.CameraBaseAdapter;
import uk.co.senab.photup.adapters.PhotosCursorAdapter;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.views.PhotupImageView;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Checkable;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;
import com.commonsware.cwac.merge.MergeAdapter;

@SuppressWarnings("deprecation")
public class UserPhotosFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		OnItemClickListener, OnPhotoSelectionChangedListener {

	static final int RESULT_CAMERA = 101;
	static final String SAVE_PHOTO_URI = "camera_photo_uri";

	static class ScaleAnimationListener implements AnimationListener {

		private final PhotupImageView mAnimatedView;
		private final ViewGroup mParent;

		public ScaleAnimationListener(ViewGroup parent, PhotupImageView view) {
			mParent = parent;
			mAnimatedView = view;
		}

		public void onAnimationEnd(Animation animation) {
			mAnimatedView.setVisibility(View.GONE);
			mParent.post(new Runnable() {
				public void run() {
					mParent.removeView(mAnimatedView);
					mAnimatedView.recycleBitmap();
				}
			});
		}

		public void onAnimationRepeat(Animation animation) {
			// NO-OP
		}

		public void onAnimationStart(Animation animation) {
			// NO-OP
		}
	}

	static final int LOADER_USER_PHOTOS = 0x01;

	private MergeAdapter mAdapter;
	private PhotosCursorAdapter mPhotoCursorAdapter;

	private AbsoluteLayout mAnimationLayout;
	private GridView mPhotoGrid;

	private PhotoUploadController mPhotoSelectionController;

	private File mPhotoFile;

	@Override
	public void onAttach(Activity activity) {
		mPhotoSelectionController = PhotoUploadController.getFromContext(activity);
		super.onAttach(activity);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case RESULT_CAMERA:
				Utils.sendMediaStoreBroadcast(getActivity(), mPhotoFile);
				mPhotoFile = null;
				break;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new MergeAdapter();
		mAdapter.addAdapter(new CameraBaseAdapter(getActivity()));

		getLoaderManager().initLoader(LOADER_USER_PHOTOS, null, this);
		mPhotoCursorAdapter = new PhotosCursorAdapter(getActivity(), R.layout.item_grid_photo, null, true);
		mAdapter.addAdapter(mPhotoCursorAdapter);

		mPhotoSelectionController.addPhotoSelectionListener(this);

		if (null != savedInstanceState) {
			if (savedInstanceState.containsKey(SAVE_PHOTO_URI)) {
				mPhotoFile = new File(savedInstanceState.getString(SAVE_PHOTO_URI));
			}
		}
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		String[] projection = { Images.Media._ID, Images.Media.MINI_THUMB_MAGIC };

		CursorLoader cursorLoader = new CursorLoader(getActivity(), Images.Media.EXTERNAL_CONTENT_URI, projection,
				Images.Media.MINI_THUMB_MAGIC + " IS NOT NULL", null, Images.Media.DATE_ADDED + " desc");

		return cursorLoader;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_user_photos, null);

		mPhotoGrid = (GridView) view.findViewById(R.id.gv_users_photos);
		mPhotoGrid.setAdapter(mAdapter);
		mPhotoGrid.setOnItemClickListener(this);

		mAnimationLayout = (AbsoluteLayout) view.findViewById(R.id.al_animation);
		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPhotoSelectionController.removePhotoSelectionListener(this);
	}

	public void onItemClick(AdapterView<?> gridView, View view, int position, long id) {
		PhotoSelection object = (PhotoSelection) view.getTag();

		if (null != object) {
			Checkable checkableView = (Checkable) view;

			if (checkableView.isChecked()) {
				mPhotoSelectionController.removePhotoSelection(object);
			} else {
				mPhotoSelectionController.addPhotoSelection(object);
				animateViewToButton(view);
			}
		} else {
			takePhoto();
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		mPhotoCursorAdapter.swapCursor(null);
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mPhotoCursorAdapter.swapCursor(data);
	}
	
	public void onSelectionsAddedToUploads() {
		mAdapter.notifyDataSetChanged();
	}

	public void onPhotoSelectionChanged(PhotoSelection upload, boolean added) {
		mAdapter.notifyDataSetChanged();
	}

	private void takePhoto() {
		if (null == mPhotoFile) {
			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			mPhotoFile = Utils.getCameraPhotoFile();
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPhotoFile));
			startActivityForResult(takePictureIntent, RESULT_CAMERA);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d("UserPhotosFragment", "onSaveInstanceState");
		if (null != mPhotoFile) {
			outState.putString(SAVE_PHOTO_URI, mPhotoFile.getAbsolutePath());
		}
		super.onSaveInstanceState(outState);
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
		animaton.setAnimationListener(new ScaleAnimationListener(mAnimationLayout, iv));
		iv.startAnimation(animaton);
	}

}
