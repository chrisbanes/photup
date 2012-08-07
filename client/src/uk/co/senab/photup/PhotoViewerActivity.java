package uk.co.senab.photup;

import java.util.Set;

import uk.co.senab.photup.adapters.SelectedPhotosViewPagerAdapter;
import uk.co.senab.photup.adapters.UserPhotosViewPagerAdapter;
import uk.co.senab.photup.fragments.FriendsListFragment;
import uk.co.senab.photup.fragments.PlacesListFragment;
import uk.co.senab.photup.listeners.OnFriendPickedListener;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.views.FiltersRadioGroup;
import uk.co.senab.photup.views.MultiTouchImageView;
import uk.co.senab.photup.views.PhotoTagItemLayout;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.MotionEvent;
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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PhotoViewerActivity extends SherlockFragmentActivity implements OnPhotoSelectionChangedListener,
		OnSingleTapListener, OnCheckedChangeListener, OnPageChangeListener, OnPickFriendRequestListener,
		OnPlacePickedListener {

	public static final String EXTRA_POSITION = "extra_position";
	public static final String EXTRA_MODE = "extra_mode";

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

			if (mController.getSelectedPhotoUploadsSize() == 0) {
				finish();
			} else {
				View view = (View) mView.getParent();
				view.post(new Runnable() {
					public void run() {
						mAdapter.refresh();
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
	private SelectedPhotosViewPagerAdapter mAdapter;
	private ViewGroup mContentView;
	private FiltersRadioGroup mFilterGroup;

	private Animation mFadeOutAnimation;
	private PhotoUploadController mController;
	private FriendsListFragment mFriendsFragment;

	private boolean mIgnoreFilterCheckCallback = false;

	private int mMode = MODE_SELECTED_VALUE;

	@Override
	public void onBackPressed() {
		if (hideFiltersView()) {
			return;
		} else {
			super.onBackPressed();
		}
	}

	private void rotateCurrentPhoto() {
		PhotoTagItemLayout currentView = (PhotoTagItemLayout) getCurrentView();
		MultiTouchImageView imageView = currentView.getImageView();

		PhotoSelection upload = getCurrentUpload();
		upload.rotateClockwise();

		imageView.requestFullSize(upload, true, null);
	}

	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (!mIgnoreFilterCheckCallback) {
			PhotoTagItemLayout currentView = (PhotoTagItemLayout) getCurrentView();
			MultiTouchImageView imageView = currentView.getImageView();

			Filter filter = checkedId != -1 ? Filter.FILTERS[checkedId] : null;
			PhotoSelection upload = getCurrentUpload();
			upload.setFilterUsed(filter);

			imageView.requestFullSize(upload, true, null);
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
				return true;
			case R.id.menu_caption:
				showCaptionDialog();
				return true;
			case R.id.menu_rotate:
				rotateCurrentPhoto();
				return true;
			case R.id.menu_place:
				startPlaceFragment();
				return true;
			case R.id.menu_crop:
				CropImageActivity.CROP_SELECTION = getCurrentUpload();
				startActivityForResult(new Intent(this, CropImageActivity.class), REQUEST_CROP_PHOTO);
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
		PhotoSelection upload = mAdapter.getItem(position);

		if (null != upload) {
			getSupportActionBar().setTitle(upload.toString());

			// Request Face Detection
			PhotoTagItemLayout currentView = (PhotoTagItemLayout) getCurrentView();
			if (null != currentView) {
				currentView.getImageView().postFaceDetection(upload);
			}

			if (null != mFilterGroup && mFilterGroup.getVisibility() == View.VISIBLE) {
				updateFiltersView();
			}
		}
	}

	public boolean onSingleTap(MotionEvent event) {
		return hideFiltersView();
	}

	public void onSelectionsAddedToUploads() {
		mAdapter.refresh();
	}

	public void onPhotoSelectionChanged(PhotoSelection upload, boolean added) {
		if (mMode == MODE_SELECTED_VALUE) {
			View view = getCurrentView();
			mFadeOutAnimation.setAnimationListener(new PhotoRemoveAnimListener(view));
			view.startAnimation(mFadeOutAnimation);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CROP_PHOTO:
				if (resultCode == RESULT_OK) {
					PhotoTagItemLayout currentView = (PhotoTagItemLayout) getCurrentView();
					if (null != currentView) {
						MultiTouchImageView imageView = currentView.getImageView();
						PhotoSelection upload = getCurrentUpload();
						imageView.requestFullSize(upload, true, null);
					}
					// Else the ViewPager is being reset so will load the
					// correct image anyway
				}
				break;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_photo_viewer);
		mContentView = (ViewGroup) findViewById(R.id.fl_root);

		mController = PhotoUploadController.getFromContext(this);
		mController.addPhotoSelectionListener(this);

		final Intent intent = getIntent();
		mMode = intent.getIntExtra(EXTRA_MODE, MODE_ALL_VALUE);

		mViewPager = (ViewPager) findViewById(R.id.vp_photos);
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.viewpager_margin));
		mViewPager.setOnPageChangeListener(this);

		if (mMode == MODE_ALL_VALUE) {
			mAdapter = new UserPhotosViewPagerAdapter(this, this, this);
		} else {
			mAdapter = new SelectedPhotosViewPagerAdapter(this, this, this);
		}
		mViewPager.setAdapter(mAdapter);

		if (intent.hasExtra(EXTRA_POSITION)) {
			final int requestedPosition = intent.getIntExtra(EXTRA_POSITION, 0);
			mViewPager.setCurrentItem(requestedPosition);
		}

		mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.photo_fade_out);
		mFriendsFragment = new FriendsListFragment();

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();		
		mController.removePhotoSelectionListener(this);
	}

	private PhotoSelection getCurrentUpload() {
		return mAdapter.getItem(mViewPager.getCurrentItem());
	}

	private View getCurrentView() {
		final PhotoSelection upload = getCurrentUpload();

		for (int i = 0, z = mViewPager.getChildCount(); i < z; i++) {
			View child = mViewPager.getChildAt(i);
			if (null != child && child.getTag() == upload) {
				return child;
			}
		}

		return null;
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
		final PhotoSelection currentUpload = getCurrentUpload();

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

	private void updateFiltersView() {
		mIgnoreFilterCheckCallback = true;
		mFilterGroup.setPhotoUpload(mAdapter.getItem(mViewPager.getCurrentItem()));
		mIgnoreFilterCheckCallback = false;
	}

	public void onPickFriendRequested(OnFriendPickedListener listener, Set<FbUser> excludeSet) {
		mFriendsFragment.setOnFriendPickedListener(listener);
		mFriendsFragment.setExcludedFriends(excludeSet);
		mFriendsFragment.show(getSupportFragmentManager(), "friends");
	}

	public void onUploadsCleared() {
		// NO-OP
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
}
