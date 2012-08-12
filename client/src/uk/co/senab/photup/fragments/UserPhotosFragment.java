package uk.co.senab.photup.fragments;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotoViewerActivity;
import uk.co.senab.photup.R;
import uk.co.senab.photup.adapters.CameraBaseAdapter;
import uk.co.senab.photup.adapters.UsersPhotosCursorAdapter;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.MediaStoreBucket;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.tasks.MediaStoreBucketsAsyncTask;
import uk.co.senab.photup.tasks.MediaStoreBucketsAsyncTask.MediaStoreBucketsResultListener;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import uk.co.senab.photup.util.Utils;
import uk.co.senab.photup.views.PhotoItemLayout;
import uk.co.senab.photup.views.PhotupImageView;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragment;
import com.commonsware.cwac.merge.MergeAdapter;
import com.jakewharton.activitycompat2.ActivityCompat2;
import com.jakewharton.activitycompat2.ActivityOptionsCompat2;

public class UserPhotosFragment extends SherlockFragment implements OnItemClickListener,
		OnPhotoSelectionChangedListener, OnScanCompletedListener, LoaderManager.LoaderCallbacks<Cursor>,
		MediaStoreBucketsResultListener, OnItemSelectedListener {

	static final int RESULT_CAMERA = 101;
	static final String SAVE_PHOTO_URI = "camera_photo_uri";
	static final String LOADER_PHOTOS_BUCKETS_PARAM = "bucket_id";

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

	private MergeAdapter mAdapter;
	private UsersPhotosCursorAdapter mPhotoAdapter;

	private GridView mPhotoGrid;

	private ArrayAdapter<MediaStoreBucket> mBucketAdapter;
	private Spinner mBucketSpinner;
	private final ArrayList<MediaStoreBucket> mBuckets = new ArrayList<MediaStoreBucket>();

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
						MediaScannerConnection.scanFile(getActivity(), new String[] { mPhotoFile.getAbsolutePath() },
								new String[] { "image/jpg" }, this);
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

		mPhotoAdapter = new UsersPhotosCursorAdapter(getActivity(), null, true);
		mAdapter.addAdapter(mPhotoAdapter);

		mBucketAdapter = new ArrayAdapter<MediaStoreBucket>(getActivity(), android.R.layout.simple_spinner_item,
				mBuckets);
		mBucketAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		new MediaStoreBucketsAsyncTask(getActivity(), this).execute();

		mPhotoSelectionController.addPhotoSelectionListener(this);
	}

	public Loader<Cursor> onCreateLoader(final int id, Bundle bundle) {

		CursorLoader cursorLoader = null;

		switch (id) {
			case LOADER_USER_PHOTOS_EXTERNAL:
				String selection = null;
				String[] selectionArgs = null;
				if (null != bundle) {
					if (bundle.containsKey(LOADER_PHOTOS_BUCKETS_PARAM)) {
						selection = Images.Media.BUCKET_ID + " = ?";
						selectionArgs = new String[] { bundle.getString(LOADER_PHOTOS_BUCKETS_PARAM) };
					}
				}

				cursorLoader = new CursorLoader(getActivity(), Images.Media.EXTERNAL_CONTENT_URI,
						MediaStoreCursorHelper.PHOTOS_PROJECTION, selection, selectionArgs,
						MediaStoreCursorHelper.PHOTOS_ORDER_BY);
				break;
		}

		return cursorLoader;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_user_photos, null);

		mPhotoGrid = (GridView) view.findViewById(R.id.gv_photos);
		mPhotoGrid.setAdapter(mAdapter);
		mPhotoGrid.setOnItemClickListener(this);

		mBucketSpinner = (Spinner) view.findViewById(R.id.sp_buckets);
		mBucketSpinner.setOnItemSelectedListener(this);
		mBucketSpinner.setAdapter(mBucketAdapter);

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
		mPhotoAdapter.notifyDataSetChanged();
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

	public void onUploadsCleared() {
		// NO-OP
	}

	public void onScanCompleted(String path, Uri uri) {
		if (null != uri) {
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					mPhotoAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
		switch (loader.getId()) {
			case LOADER_USER_PHOTOS_EXTERNAL:
				mPhotoAdapter.swapCursor(null);
				break;
		}
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()) {
			case LOADER_USER_PHOTOS_EXTERNAL:
				mPhotoAdapter.swapCursor(data);
				mPhotoGrid.setSelection(0);
				break;
		}
	}

	public void onBucketsLoaded(List<MediaStoreBucket> buckets) {
		mBuckets.clear();
		mBuckets.addAll(buckets);
		mBucketAdapter.notifyDataSetChanged();
		mBucketSpinner.setSelection(0);
	}

	public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
		MediaStoreBucket item = (MediaStoreBucket) adapterView.getItemAtPosition(position);
		if (null != item) {
			loadBucketId(item.getId());
		}
	}

	private void loadBucketId(String id) {
		Bundle bundle = new Bundle();
		if (null != id) {
			bundle.putString(LOADER_PHOTOS_BUCKETS_PARAM, id);
		}
		getLoaderManager().restartLoader(LOADER_USER_PHOTOS_EXTERNAL, bundle, this);
	}

	public void onNothingSelected(AdapterView<?> view) {
		// NO-OP
	}
}
