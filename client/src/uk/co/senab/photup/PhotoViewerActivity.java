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
package uk.co.senab.photup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import java.util.Set;

import de.greenrobot.event.EventBus;
import uk.co.senab.photup.adapters.SelectedPhotosViewPagerAdapter;
import uk.co.senab.photup.adapters.UserPhotosViewPagerAdapter;
import uk.co.senab.photup.base.PhotupFragmentActivity;
import uk.co.senab.photup.events.PhotoSelectionRemovedEvent;
import uk.co.senab.photup.fragments.FriendsListFragment;
import uk.co.senab.photup.fragments.PlacesListFragment;
import uk.co.senab.photup.listeners.OnFriendPickedListener;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.util.Analytics;
import uk.co.senab.photup.util.CursorPagerAdapter;
import uk.co.senab.photup.util.MediaStoreCursorHelper;
import uk.co.senab.photup.util.PhotupCursorLoader;
import uk.co.senab.photup.views.FiltersRadioGroup;
import uk.co.senab.photup.views.MultiTouchImageView;
import uk.co.senab.photup.views.PhotoTagItemLayout;

public class PhotoViewerActivity extends PhotupFragmentActivity implements OnSingleTapListener,
        OnCheckedChangeListener, OnPageChangeListener, OnPickFriendRequestListener,
        OnPlacePickedListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EXTRA_POSITION = "extra_position";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_BUCKET_ID = "extra_bucket_id";

    public static int MODE_ALL_VALUE = 100;
    public static int MODE_SELECTED_VALUE = 101;

    static final int REQUEST_CROP_PHOTO = 200;

    class PhotoRemoveAnimListener implements AnimationListener {

        private final View mView;

        public PhotoRemoveAnimListener(View view) {
            mView = view;
        }

        public void onAnimationEnd(Animation animation) {
            mView.setVisibility(View.GONE);
            animation.setAnimationListener(null);

            if (!mController.hasSelections()) {
                finish();
            } else {
                View view = (View) mView.getParent();
                view.post(new Runnable() {
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        public void onAnimationRepeat(Animation animation) {
        }

        public void onAnimationStart(Animation animation) {
        }

    }

    private ViewPager mViewPager;
    private PagerAdapter mAdapter;
    private ViewGroup mContentView;
    private FiltersRadioGroup mFilterGroup;

    private Animation mFadeOutAnimation;
    private PhotoUploadController mController;
    private FriendsListFragment mFriendsFragment;

    private boolean mIgnoreFilterCheckCallback = false;

    private int mMode = MODE_SELECTED_VALUE;
    private String mBucketId;
    private int mRequestedPosition = -1;

    @Override
    public void onBackPressed() {
        if (hideFiltersView()) {
            return;
        } else {
            super.onBackPressed();
        }
    }

    private void rotateCurrentPhoto() {
        PhotoTagItemLayout currentView = getCurrentView();
        PhotoUpload upload = currentView.getPhotoSelection();
        upload.rotateClockwise();
        reloadView(currentView);
    }

    private void resetCurrentPhoto() {
        PhotoTagItemLayout currentView = getCurrentView();
        PhotoUpload upload = currentView.getPhotoSelection();

        upload.reset();
        reloadView(currentView);
    }

    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (!mIgnoreFilterCheckCallback) {
            Filter filter = checkedId != -1 ? Filter.mapFromId(checkedId) : null;
            PhotoTagItemLayout currentView = getCurrentView();
            PhotoUpload upload = currentView.getPhotoSelection();

            upload.setFilterUsed(filter);
            reloadView(currentView);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_photo_viewer, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_filters:
                showFiltersView();
                Analytics.logEvent(Analytics.EVENT_PHOTO_FILTERS);
                return true;
            case R.id.menu_caption:
                Analytics.logEvent(Analytics.EVENT_PHOTO_CAPTION);
                showCaptionDialog();
                return true;
            case R.id.menu_rotate:
                Analytics.logEvent(Analytics.EVENT_PHOTO_ROTATE);
                rotateCurrentPhoto();
                return true;
            case R.id.menu_place:
                Analytics.logEvent(Analytics.EVENT_PHOTO_PLACE);
                startPlaceFragment();
                return true;
            case R.id.menu_crop:
                Analytics.logEvent(Analytics.EVENT_PHOTO_CROP);
                CropImageActivity.CROP_SELECTION = getCurrentUpload();
                startActivityForResult(new Intent(this, CropImageActivity.class),
                        REQUEST_CROP_PHOTO);
                return true;
            case R.id.menu_reset:
                Analytics.logEvent(Analytics.EVENT_PHOTO_RESET);
                resetCurrentPhoto();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onPageScrolled(int position, float arg1, int arg2) {
        // NO-OP
    }

    public void onPageScrollStateChanged(int state) {
        if (state != ViewPager.SCROLL_STATE_IDLE) {
            clearFaceDetectionPasses();
        }
    }

    public void onPageSelected(int position) {
        PhotoTagItemLayout currentView = getCurrentView();

        if (null != currentView) {
            PhotoUpload upload = currentView.getPhotoSelection();

            if (null != upload) {
                getSupportActionBar().setTitle(upload.toString());

                // Request Face Detection
                currentView.getImageView().postFaceDetection(upload);

                if (null != mFilterGroup && mFilterGroup.getVisibility() == View.VISIBLE) {
                    updateFiltersView();
                }
            }
        }
    }

    public boolean onSingleTap() {
        return hideFiltersView();
    }

    public void onEvent(PhotoSelectionRemovedEvent event) {
        if (event.isSingleChange()) {
            animatePhotoUploadOut(event.getTarget());
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void animatePhotoUploadOut(PhotoUpload upload) {
        if (mMode == MODE_SELECTED_VALUE) {
            PhotoTagItemLayout view = getCurrentView();

            if (upload.equals(view.getPhotoSelection())) {
                mFadeOutAnimation.setAnimationListener(new PhotoRemoveAnimListener(view));
                view.startAnimation(mFadeOutAnimation);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    reloadView(getCurrentView());
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_viewer);
        mContentView = (ViewGroup) findViewById(R.id.fl_root);

        mController = PhotoUploadController.getFromContext(this);
        EventBus.getDefault().register(this);

        final Intent intent = getIntent();
        mMode = intent.getIntExtra(EXTRA_MODE, MODE_ALL_VALUE);

        if (mMode == MODE_ALL_VALUE) {
            mBucketId = intent.getStringExtra(EXTRA_BUCKET_ID);
        }

        mViewPager = (ViewPager) findViewById(R.id.vp_photos);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.viewpager_margin));
        mViewPager.setOnPageChangeListener(this);

        if (mMode == MODE_ALL_VALUE) {
            mAdapter = new UserPhotosViewPagerAdapter(this, this, this);
            getSupportLoaderManager().initLoader(0, null, this);
        } else {
            mAdapter = new SelectedPhotosViewPagerAdapter(this, this, this);
        }
        mViewPager.setAdapter(mAdapter);

        if (intent.hasExtra(EXTRA_POSITION)) {
            mRequestedPosition = intent.getIntExtra(EXTRA_POSITION, 0);
            mViewPager.setCurrentItem(mRequestedPosition);
        }

        mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.photo_fade_out);
        mFriendsFragment = new FriendsListFragment();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(" ");

        /**
         * Nasty hack, basically we need to know when the ViewPager is laid out,
         * we then manually call onPageSelected. This is to fix onPageSelected
         * not being called on the first item.
         */
        mViewPager.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            public void onGlobalLayout() {
                mViewPager.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                onPageSelected(mViewPager.getCurrentItem());
                showTapToTagPrompt();
            }
        });
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        mController.updateDatabase();
        super.onDestroy();
    }

    private PhotoUpload getCurrentUpload() {
        PhotoTagItemLayout view = getCurrentView();
        if (null != view) {
            return view.getPhotoSelection();
        }
        return null;
    }

    private PhotoTagItemLayout getCurrentView() {
        final int currentPos = mViewPager.getCurrentItem();

        for (int i = 0, z = mViewPager.getChildCount(); i < z; i++) {
            PhotoTagItemLayout child = (PhotoTagItemLayout) mViewPager.getChildAt(i);
            if (null != child && child.getPosition() == currentPos) {
                return child;
            }
        }

        return null;
    }

    private void reloadView(PhotoTagItemLayout currentView) {
        if (null != currentView) {
            MultiTouchImageView imageView = currentView.getImageView();
            PhotoUpload selection = currentView.getPhotoSelection();
            imageView.requestFullSize(selection, true, false, null);
        }
    }

    private void clearFaceDetectionPasses() {
        for (int i = 0, z = mViewPager.getChildCount(); i < z; i++) {
            PhotoTagItemLayout child = (PhotoTagItemLayout) mViewPager.getChildAt(i);
            if (null != child) {
                child.getImageView().clearFaceDetection();
            }
        }
    }

    private void showCaptionDialog() {
        final PhotoUpload currentUpload = getCurrentUpload();
        if (null != currentUpload) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.photo_caption);
            builder.setIcon(R.drawable.ic_action_caption);

            // Set an EditText view to get user input
            final FrameLayout layout = new FrameLayout(this);
            final int margin = getResources().getDimensionPixelSize(R.dimen.spacing);
            layout.setPadding(margin, margin, margin, margin);

            final EditText input = new EditText(this);
            input.setMinLines(2);
            input.setText(currentUpload.getCaption());
            layout.addView(input);

            builder.setView(layout);

            final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    switch (whichButton) {
                        case AlertDialog.BUTTON_POSITIVE:
                            currentUpload.setCaption(input.getText().toString());
                            getSupportActionBar().setTitle(currentUpload.toString());
                            break;

                        case AlertDialog.BUTTON_NEGATIVE:
                        default:
                            dialog.dismiss();
                            break;
                    }
                }
            };

            builder.setPositiveButton(android.R.string.ok, listener);
            builder.setNegativeButton(android.R.string.cancel, listener);
            builder.show();
        }
    }

    private boolean hideFiltersView() {
        if (null != mFilterGroup && mFilterGroup.isShowing()) {
            mFilterGroup.hide();
            getSupportActionBar().show();
            return true;
        }
        return false;
    }

    private void startPlaceFragment() {
        PlacesListFragment fragment = new PlacesListFragment();
        fragment.setOnPlacePickedListener(this);
        fragment.setPhotoTagLocation(getCurrentUpload().getExifLocation(this));
        fragment.show(getSupportFragmentManager(), "places");
    }

    private void showFiltersView() {
        ActionBar ab = getSupportActionBar();
        if (ab.isShowing()) {
            ab.hide();
        }

        if (null == mFilterGroup) {
            View view = getLayoutInflater().inflate(R.layout.layout_filters, mContentView);
            mFilterGroup = (FiltersRadioGroup) view.findViewById(R.id.rg_filters);
            mFilterGroup.setOnCheckedChangeListener(this);
        }

        mFilterGroup.show();
        updateFiltersView();
    }

    private void showTapToTagPrompt() {
        Toast.makeText(this, R.string.tag_friend_prompt, Toast.LENGTH_SHORT).show();
    }

    private void updateFiltersView() {
        mIgnoreFilterCheckCallback = true;
        mFilterGroup.setPhotoUpload(getCurrentUpload());
        mIgnoreFilterCheckCallback = false;
    }

    public void onPickFriendRequested(OnFriendPickedListener listener, Set<FbUser> excludeSet) {
        mFriendsFragment.setOnFriendPickedListener(listener);
        mFriendsFragment.setExcludedFriends(excludeSet);
        mFriendsFragment.show(getSupportFragmentManager(), "friends");
    }

    public void onPhotoLoadStatusChanged(boolean finished) {
        // TODO Fix this setProgressBarIndeterminateVisibility(!finished);
    }

    public void onPlacePicked(Place place) {
        PhotoUpload upload = getCurrentUpload();
        if (null != upload) {
            upload.setPlace(place);
            getSupportActionBar().setTitle(upload.toString());
        }
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle params) {
        String selection = null;
        String[] selectionArgs = null;
        if (null != mBucketId) {
            selection = Images.Media.BUCKET_ID + " = ?";
            selectionArgs = new String[]{mBucketId};
        }

        return new PhotupCursorLoader(this, MediaStoreCursorHelper.MEDIA_STORE_CONTENT_URI,
                MediaStoreCursorHelper.PHOTOS_PROJECTION, selection, selectionArgs,
                MediaStoreCursorHelper.PHOTOS_ORDER_BY, false);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mAdapter instanceof CursorPagerAdapter) {
            ((CursorPagerAdapter) mAdapter).swapCursor(cursor);
        }

        if (mRequestedPosition != -1) {
            mViewPager.setCurrentItem(mRequestedPosition, false);
            mRequestedPosition = -1;
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        onLoadFinished(loader, null);
    }
}
