/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.senab.photup.fragments;

import com.commonsware.cwac.merge.MergeAdapter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import uk.co.senab.photup.Flags;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotoViewerActivity;
import uk.co.senab.photup.PreferenceConstants;
import uk.co.senab.photup.R;
import uk.co.senab.photup.adapters.CameraBaseAdapter;
import uk.co.senab.photup.adapters.UsersPhotosCursorAdapter;
import uk.co.senab.photup.events.PhotoSelectionAddedEvent;
import uk.co.senab.photup.events.PhotoSelectionRemovedEvent;
import uk.co.senab.photup.model.MediaStoreBucket;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.tasks.MediaStoreBucketsAsyncTask;
import uk.co.senab.photup.tasks.MediaStoreBucketsAsyncTask.MediaStoreBucketsResultListener;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import uk.co.senab.photup.util.PhotupCursorLoader;
import uk.co.senab.photup.util.Utils;
import uk.co.senab.photup.views.PhotoItemLayout;
import uk.co.senab.photup.views.PhotupImageView;

public class UserPhotosFragment extends AbstractPhotosFragment implements OnItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>, MediaStoreBucketsResultListener,
        OnItemSelectedListener,
        OnScanCompletedListener {

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

    static final int RESULT_CAMERA = 101;
    static final String SAVE_PHOTO_URI = "camera_photo_uri";

    static final String LOADER_PHOTOS_BUCKETS_PARAM = "bucket_id";
    static final int LOADER_USER_PHOTOS_EXTERNAL = 0x01;

    private MergeAdapter mAdapter;
    private UsersPhotosCursorAdapter mPhotoAdapter;
    private GridView mPhotoGrid;

    private ArrayAdapter<MediaStoreBucket> mBucketAdapter;
    private Spinner mBucketSpinner;
    private final ArrayList<MediaStoreBucket> mBuckets = new ArrayList<MediaStoreBucket>();

    private PhotoUploadController mPhotoSelectionController;
    private File mPhotoFile;
    private SharedPreferences mPrefs;

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
                        Utils.scanMediaJpegFile(getActivity(), mPhotoFile, this);
                    } else {
                        if (Flags.DEBUG) {
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

    @Override
    public void onAttach(Activity activity) {
        mPhotoSelectionController = PhotoUploadController.getFromContext(activity);
        super.onAttach(activity);
    }

    public void onBucketsLoaded(final List<MediaStoreBucket> buckets) {
        if (null != buckets && !buckets.isEmpty()) {
            mBuckets.clear();
            mBuckets.addAll(buckets);
            mBucketAdapter.notifyDataSetChanged();
            setSelectedBucketFromPrefs();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        mAdapter = new MergeAdapter();

        if (Utils.hasCamera(getActivity())) {
            mAdapter.addAdapter(new CameraBaseAdapter(getActivity()));
        }
        mPhotoAdapter = new UsersPhotosCursorAdapter(getActivity(), null);
        mAdapter.addAdapter(mPhotoAdapter);

        mBucketAdapter = new ArrayAdapter<MediaStoreBucket>(getActivity(),
                Utils.getSpinnerItemResId(), mBuckets);
        mBucketAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        EventBus.getDefault().register(this);
    }

    public Loader<Cursor> onCreateLoader(final int id, Bundle bundle) {

        CursorLoader cursorLoader = null;

        switch (id) {
            case LOADER_USER_PHOTOS_EXTERNAL:
                String selection = null;
                String[] selectionArgs = null;

                if (null != bundle && bundle.containsKey(LOADER_PHOTOS_BUCKETS_PARAM)) {
                    selection = Images.Media.BUCKET_ID + " = ?";
                    selectionArgs = new String[]{bundle.getString(LOADER_PHOTOS_BUCKETS_PARAM)};
                }

                cursorLoader = new PhotupCursorLoader(getActivity(),
                        MediaStoreCursorHelper.MEDIA_STORE_CONTENT_URI,
                        MediaStoreCursorHelper.PHOTOS_PROJECTION, selection, selectionArgs,
                        MediaStoreCursorHelper.PHOTOS_ORDER_BY, false);
                break;
        }

        return cursorLoader;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
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
        EventBus.getDefault().unregister(this);
        saveSelectedBucketToPrefs();
    }

    public void onItemClick(AdapterView<?> gridView, View view, int position, long id) {
        if (view.getId() == R.id.iv_camera_button) {
            takePhoto();
        } else {
            Bundle b = null;
            if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                ActivityOptionsCompat options = ActivityOptionsCompat
                        .makeThumbnailScaleUpAnimation(view,
                                Utils.drawViewOntoBitmap(view), 0, 0);
                b = options.toBundle();
            }

            Intent intent = new Intent(getActivity(), PhotoViewerActivity.class);

            // Need take Camera icon into account so minus 1
            intent.putExtra(PhotoViewerActivity.EXTRA_POSITION, position - 1);
            intent.putExtra(PhotoViewerActivity.EXTRA_MODE, PhotoViewerActivity.MODE_ALL_VALUE);

            MediaStoreBucket bucket = (MediaStoreBucket) mBucketSpinner.getSelectedItem();
            intent.putExtra(PhotoViewerActivity.EXTRA_BUCKET_ID, bucket.getId());

            ActivityCompat.startActivity(getActivity(), intent, b);
        }
    }

    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        MediaStoreBucket item = (MediaStoreBucket) adapterView.getItemAtPosition(position);
        if (null != item) {
            loadBucketId(item.getId());
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

    public void onNothingSelected(AdapterView<?> view) {
        // NO-OP
    }

    private void updateUploadView(PhotoUpload upload, boolean added) {
        for (int i = 0, z = mPhotoGrid.getChildCount(); i < z; i++) {
            View view = mPhotoGrid.getChildAt(i);

            if (view instanceof PhotoItemLayout) {
                PhotoItemLayout layout = (PhotoItemLayout) view;
                if (upload.equals(layout.getPhotoSelection())) {
                    if (Flags.DEBUG) {
                        Log.d("UserPhotosFragment", "Found View, setChecked");
                    }
                    layout.setChecked(added);
                    break;
                }
            }
        }
    }

    public void onEvent(PhotoSelectionAddedEvent event) {
        if (event.isSingleChange()) {
            updateUploadView(event.getTarget(), true);
        } else {
            mPhotoAdapter.notifyDataSetChanged();
        }
    }

    public void onEvent(PhotoSelectionRemovedEvent event) {
        if (event.isSingleChange()) {
            updateUploadView(event.getTarget(), false);
        } else {
            mPhotoAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (null != mPhotoFile) {
            outState.putString(SAVE_PHOTO_URI, mPhotoFile.getAbsolutePath());
        }
        super.onSaveInstanceState(outState);
    }

    public void onScanCompleted(String path, Uri uri) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                MediaStoreBucket bucket = getSelectedBucket();
                if (null != bucket) {
                    loadBucketId(bucket.getId());
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // Load buckets
        MediaStoreBucketsAsyncTask.execute(getActivity(), this);
    }

    public void selectAll() {
        Cursor cursor = mPhotoAdapter.getCursor();
        if (null != cursor) {
            ArrayList<PhotoUpload> selections = MediaStoreCursorHelper.photosCursorToSelectionList(
                    MediaStoreCursorHelper.MEDIA_STORE_CONTENT_URI, cursor);
            mPhotoSelectionController.addSelections(selections);
        }
    }

    private MediaStoreBucket getSelectedBucket() {
        if (null != mBucketSpinner) {
            return (MediaStoreBucket) mBucketSpinner.getSelectedItem();
        }
        return null;
    }

    private void loadBucketId(String id) {
        if (isAdded()) {
            Bundle bundle = new Bundle();
            if (null != id) {
                bundle.putString(LOADER_PHOTOS_BUCKETS_PARAM, id);
            }
            try {
                getLoaderManager().restartLoader(LOADER_USER_PHOTOS_EXTERNAL, bundle, this);
            } catch (IllegalStateException e) {
                e.printStackTrace();
                // Can sometimes catch with: Fragment not attached to Activity.
                // Not much we can do to recover
            }
        }
    }

    private void saveSelectedBucketToPrefs() {
        MediaStoreBucket bucket = getSelectedBucket();
        if (null != bucket && null != mPrefs) {
            mPrefs.edit()
                    .putString(PreferenceConstants.PREF_SELECTED_MEDIA_BUCKET_ID, bucket.getId())
                    .commit();
        }
    }

    private void setSelectedBucketFromPrefs() {
        if (null != mBucketSpinner) {
            int newSelection = 0;

            if (null != mPrefs) {
                final String lastBucketId = mPrefs
                        .getString(PreferenceConstants.PREF_SELECTED_MEDIA_BUCKET_ID, null);
                if (null != lastBucketId) {
                    for (int i = 0, z = mBuckets.size(); i < z; i++) {
                        if (lastBucketId.equals(mBuckets.get(i).getId())) {
                            newSelection = i;
                            break;
                        }
                    }
                }
            }

            // If we have a new position, then just set it. If it's our current
            // position, then call onItemSelected manually
            if (newSelection != mBucketSpinner.getSelectedItemPosition()) {
                mBucketSpinner.setSelection(newSelection);
            } else {
                onItemSelected(mBucketSpinner, null, newSelection, 0);
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
}
