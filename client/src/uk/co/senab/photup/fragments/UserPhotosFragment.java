package uk.co.senab.photup.fragments;

import java.io.File;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotoViewerActivity;
import uk.co.senab.photup.R;
import uk.co.senab.photup.Utils;
import uk.co.senab.photup.adapters.CameraBaseAdapter;
import uk.co.senab.photup.adapters.UsersPhotosBaseAdapter;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.views.PhotoItemLayout;
import uk.co.senab.photup.views.PhotupImageView;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;
import com.commonsware.cwac.merge.MergeAdapter;
import com.jakewharton.activitycompat2.ActivityCompat2;
import com.jakewharton.activitycompat2.ActivityOptionsCompat2;

public class UserPhotosFragment extends SherlockFragment implements OnItemClickListener,
		OnPhotoSelectionChangedListener {

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

	static final int LOADER_USER_PHOTOS_EXTERNAL = 0x01;
	static final int LOADER_USER_PHOTOS_INTERNAL = 0x02;

	private MergeAdapter mAdapter;
	private UsersPhotosBaseAdapter mPhotoAdapter;

	private GridView mPhotoGrid;

	private PhotoUploadController mPhotoSelectionController;
	private File mPhotoFile;

	@Override
	public void onAttach(Activity activity) {
		mPhotoSelectionController = PhotoUploadController.getFromContext(activity);
		super.onAttach(activity);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		if (null != savedInstanceState) {
			if (savedInstanceState.containsKey(SAVE_PHOTO_URI)) {
				mPhotoFile = new File(savedInstanceState.getString(SAVE_PHOTO_URI));
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case RESULT_CAMERA:
				if (null != mPhotoFile) {
					if (resultCode == Activity.RESULT_OK) {
						Utils.sendMediaStoreBroadcast(getActivity(), mPhotoFile);
					} else {
						if (Constants.DEBUG) {
							Log.d("UserPhotosFragment", "Deleting Photo File");
						}
						mPhotoFile.delete();
					}
					mPhotoFile = null;
				}
				return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mAdapter = new MergeAdapter();
		mAdapter.addAdapter(new CameraBaseAdapter(getActivity()));

		mPhotoAdapter = new UsersPhotosBaseAdapter(getActivity());
		mAdapter.addAdapter(mPhotoAdapter);

		mPhotoSelectionController.addPhotoSelectionListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_user_photos, null);

		mPhotoGrid = (GridView) view.findViewById(R.id.gv_photos);
		mPhotoGrid.setAdapter(mAdapter);
		mPhotoGrid.setOnItemClickListener(this);

		return view;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mPhotoSelectionController.removePhotoSelectionListener(this);
	}

	public void onItemClick(AdapterView<?> gridView, View view, int position, long id) {
		if (view.getId() == R.id.iv_camera_button) {
			takePhoto();
		} else {
			ActivityOptionsCompat2 options = ActivityOptionsCompat2.makeThumbnailScaleUpAnimation(view,
					Utils.drawViewOntoBitmap(view), 0, 0);

			Intent intent = new Intent(getActivity(), PhotoViewerActivity.class);

			// Need take Camera icon into account so minus 1
			intent.putExtra(PhotoViewerActivity.EXTRA_POSITION, position - 1);
			intent.putExtra(PhotoViewerActivity.EXTRA_MODE, PhotoViewerActivity.MODE_ALL_VALUE);

			ActivityCompat2.startActivity(getActivity(), intent, options.toBundle());
		}
	}

	public void onSelectionsAddedToUploads() {
		mPhotoAdapter.refresh();
	}

	public void onPhotoSelectionChanged(PhotoSelection upload, boolean added) {
		for (int i = 0, z = mPhotoGrid.getChildCount(); i < z; i++) {
			View view = mPhotoGrid.getChildAt(i);

			if (view instanceof PhotoItemLayout) {
				PhotoItemLayout layout = (PhotoItemLayout) view;
				if (upload.equals(layout.getPhotoSelection())) {
					if (Constants.DEBUG) {
						Log.d("UserPhotosFragment", "Found View, setChecked");
					}
					layout.setChecked(added);
					break;
				}
			}
		}
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
		if (null != mPhotoFile) {
			outState.putString(SAVE_PHOTO_URI, mPhotoFile.getAbsolutePath());
		}
		super.onSaveInstanceState(outState);
	}

	// private void animateViewToButton(View view) {
	// // New ImageView with Bitmap of View
	// PhotupImageView iv = new PhotupImageView(getActivity());
	// iv.setImageBitmap(Utils.drawViewOntoBitmap(view));
	//
	// // Align it so that it's directly over the current View
	// AbsoluteLayout.LayoutParams lp = new
	// AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.WRAP_CONTENT,
	// AbsoluteLayout.LayoutParams.WRAP_CONTENT, view.getLeft(), view.getTop());
	// mAnimationLayout.addView(iv, lp);
	//
	// int halfTabHeight =
	// getResources().getDimensionPixelSize(R.dimen.abs__action_bar_default_height)
	// / 2;
	// int midSecondTabX;
	//
	// if (getSherlockActivity().getSupportActionBar().getTabCount() == 2) {
	// midSecondTabX = Math.round(mPhotoGrid.getWidth() * 0.75f);
	// } else {
	// midSecondTabX = mPhotoGrid.getWidth() / 2;
	// }
	//
	// Animation animaton = Utils.createScaleAnimation(view,
	// mPhotoGrid.getWidth(), mPhotoGrid.getHeight(),
	// midSecondTabX, mPhotoGrid.getTop() - halfTabHeight);
	// animaton.setAnimationListener(new
	// ScaleAnimationListener(mAnimationLayout, iv));
	// iv.startAnimation(animaton);
	// }

	public void onUploadsCleared() {
		// NO-OP
	}
}
