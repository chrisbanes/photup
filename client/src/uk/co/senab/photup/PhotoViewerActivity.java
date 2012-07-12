package uk.co.senab.photup;

import java.util.Set;

import uk.co.senab.photup.adapters.PhotoViewPagerAdapter;
import uk.co.senab.photup.fragments.FriendsListFragment;
import uk.co.senab.photup.listeners.OnFriendPickedListener;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.listeners.OnPickFriendRequestListener;
import uk.co.senab.photup.listeners.OnSingleTapListener;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.model.PhotoSelection;
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
		OnSingleTapListener, OnCheckedChangeListener, OnPageChangeListener, OnPickFriendRequestListener {

	public static final String EXTRA_POSITION = "extra_position";
	public static final String EXTRA_MODE = "extra_mode";

	public static int MODE_ALL_VALUE = 100;
	public static int MODE_SELECTED_VALUE = 101;

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
	private PhotoViewPagerAdapter mAdapter;
	private ViewGroup mContentView;
	private FiltersRadioGroup mFilterGroup;

	private Animation mFadeOutAnimation;
	private PhotoUploadController mController;
	private FriendsListFragment mFriendsFragment;

	private boolean mIgnoreFilterCheckCallback = false;

	private int mMode = MODE_SELECTED_VALUE;

	@Override
	public void onBackPressed() {
		if (null != mFilterGroup && mFilterGroup.getVisibility() == View.VISIBLE) {
			hideFiltersView();
		} else {
			super.onBackPressed();
		}
	}

	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (!mIgnoreFilterCheckCallback) {
			PhotoTagItemLayout currentView = (PhotoTagItemLayout) getCurrentView();
			MultiTouchImageView imageView = currentView.getImageView();

			Filter filter = checkedId != -1 ? Filter.FILTERS[checkedId] : null;
			PhotoSelection upload = getCurrentUpload();
			upload.setFilterUsed(filter);

			imageView.requestFullSize(upload, true);
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
			case R.id.menu_filters:
				showFiltersView();
				return true;
			case R.id.menu_caption:
				showCaptionDialog();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void onPageScrolled(int position, float arg1, int arg2) {
		// NO-OP
	}

	public void onPageScrollStateChanged(int state) {
		// NO-OP
	}

	public void onPageSelected(int position) {
		if (null != mFilterGroup && mFilterGroup.getVisibility() == View.VISIBLE) {
			updateFiltersView();
		}

		PhotoSelection upload = mAdapter.getItem(position);
		String caption = upload.getCaption();
		if (null == caption) {
			caption = "";
		}
		getSupportActionBar().setTitle(caption);
	}

	public boolean onSingleTap(MotionEvent event) {
		return hideFiltersView();
	}

	public void onSelectionsAddedToUploads() {
		mAdapter.notifyDataSetChanged();
	}

	public void onPhotoSelectionChanged(PhotoSelection upload, boolean added) {
		if (mMode == MODE_SELECTED_VALUE) {
			View view = getCurrentView();
			mFadeOutAnimation.setAnimationListener(new PhotoRemoveAnimListener(view));
			view.startAnimation(mFadeOutAnimation);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo_viewer);
		mContentView = (ViewGroup) findViewById(R.id.fl_root);

		mController = PhotoUploadController.getFromContext(this);
		mController.addPhotoSelectionListener(this);

		final Intent intent = getIntent();
		mMode = intent.getIntExtra(EXTRA_MODE, MODE_SELECTED_VALUE);

		mViewPager = (ViewPager) findViewById(R.id.vp_photos);
		mViewPager.setOffscreenPageLimit(1);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.viewpager_margin));
		mViewPager.setOnPageChangeListener(this);

		mAdapter = new PhotoViewPagerAdapter(this, this, this);
		mViewPager.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();

		final int requestedPosition = intent.getIntExtra(EXTRA_POSITION, 0);
		if (mAdapter.getCount() > requestedPosition) {
			if (mViewPager.getCurrentItem() != requestedPosition) {
				mViewPager.setCurrentItem(requestedPosition);
			} else {
				onPageSelected(requestedPosition);
			}
		}

		mFadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.photo_fade_out);
		mFriendsFragment = new FriendsListFragment();
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

	private void showCaptionDialog() {
		final PhotoSelection currentUpload = getCurrentUpload();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.photo_caption);

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
						getSupportActionBar().setTitle(currentUpload.getCaption());
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
		if (null != mFilterGroup && mFilterGroup.getVisibility() == View.VISIBLE) {
			mFilterGroup.hide();
			getSupportActionBar().show();
			return true;
		}
		return false;
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
}
