package uk.co.senab.photup;

import uk.co.senab.photup.adapters.PhotoViewPagerAdapter;
import uk.co.senab.photup.listeners.OnUploadChangedListener;
import uk.co.senab.photup.model.Filter;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.FiltersRadioGroup;
import uk.co.senab.photup.views.MultiTouchImageView;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.lightbox.android.photoprocessing.R;

public class PhotoViewerActivity extends SherlockActivity implements OnUploadChangedListener, OnTouchListener,
		OnCheckedChangeListener, OnPageChangeListener {

	class TapListener extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			toggleActionBarVisibility();
			return true;
		}
	}

	public static final String EXTRA_POSITION = "extra_position";

	private ViewPager mViewPager;
	private PhotoViewPagerAdapter mAdapter;
	private ViewGroup mContentView;
	private FiltersRadioGroup mFilterGroup;

	private GestureDetector mGestureDectector;

	private PhotoSelectionController mController;

	private boolean mIgnoreCheckCallback = false;

	@Override
	public void onBackPressed() {
		if (null != mFilterGroup && mFilterGroup.getVisibility() == View.VISIBLE) {
			toggleActionBarVisibility();
		} else {
			super.onBackPressed();
		}
	}

	public void onCheckedChanged(RadioGroup group, int checkedId) {
		if (!mIgnoreCheckCallback) {
			View currentView = getCurrentView();
			MultiTouchImageView imageView = (MultiTouchImageView) currentView.findViewById(R.id.iv_photo);

			Filter filter = checkedId != -1 ? Filter.FILTERS[checkedId] : null;
			PhotoUpload upload = getCurrentUpload();
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
			case R.id.menu_remove:
				mController.removePhotoUpload(getCurrentUpload());
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
	}

	public boolean onTouch(View v, MotionEvent event) {
		if (mGestureDectector.onTouchEvent(event)) {
			return true;
		}
		return false;
	}

	public void onUploadChanged(PhotoUpload upload, boolean added) {
		mAdapter.notifyDataSetChanged();

		if (mController.getSelectedPhotoUploadsSize() == 0) {
			finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo_viewer);
		mContentView = (ViewGroup) findViewById(R.id.fl_root);

		mGestureDectector = new GestureDetector(this, new TapListener());

		mViewPager = (ViewPager) findViewById(R.id.vp_photos);
		mViewPager.setOffscreenPageLimit(1);
		mViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.viewpager_margin));
		mAdapter = new PhotoViewPagerAdapter(this, this);
		mViewPager.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();
		mViewPager.setOnPageChangeListener(this);

		mController = PhotoSelectionController.getFromContext(this);
		mController.addPhotoSelectionListener(this);

		final Intent intent = getIntent();
		mViewPager.setCurrentItem(intent.getIntExtra(EXTRA_POSITION, 0));

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mController.removePhotoSelectionListener(this);
	}

	private PhotoUpload getCurrentUpload() {
		return mAdapter.getItem(mViewPager.getCurrentItem());
	}

	private View getCurrentView() {
		final PhotoUpload upload = getCurrentUpload();

		for (int i = 0, z = mViewPager.getChildCount(); i < z; i++) {
			View child = mViewPager.getChildAt(i);
			if (null != child && child.getTag() == upload) {
				return child;
			}
		}

		return null;
	}

	private void hideFiltersView() {
		if (null != mFilterGroup) {
			mFilterGroup.hide();
		}
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
		mIgnoreCheckCallback = true;
		mFilterGroup.setPhotoUpload(mAdapter.getItem(mViewPager.getCurrentItem()));
		mIgnoreCheckCallback = false;
	}

	private void toggleActionBarVisibility() {
		ActionBar ab = getSupportActionBar();
		if (ab.isShowing()) {
			ab.hide();
		} else {
			ab.show();
			hideFiltersView();
		}
	}
}
