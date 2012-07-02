package uk.co.senab.photup;

import uk.co.senab.photup.adapters.PhotoViewPagerAdapter;
import uk.co.senab.photup.listeners.OnUploadChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.views.FiltersRadioGroup;
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

public class PhotoViewerActivity extends SherlockActivity implements OnUploadChangedListener, OnTouchListener, OnCheckedChangeListener, OnPageChangeListener {

	public static final String EXTRA_POSITION = "extra_position";

	class TapListener extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			toggleActionBarVisibility();
			return true;
		}
	}

	private ViewGroup mContentView;
	private ViewPager mViewPager;
	private PhotoViewPagerAdapter mAdapter;
	private GestureDetector mGestureDectector;

	private FiltersRadioGroup mFilterGroup;

	private PhotoSelectionController mController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo_viewer);
		mContentView = (ViewGroup) findViewById(R.id.fl_root);

		mGestureDectector = new GestureDetector(this, new TapListener());

		mViewPager = (ViewPager) findViewById(R.id.vp_photos);
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
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mController.removePhotoSelectionListener(this);
	}

	public void onUploadChanged(PhotoUpload upload, boolean added) {
		mAdapter.notifyDataSetChanged();
	}

	public boolean onTouch(View v, MotionEvent event) {
		if (mGestureDectector.onTouchEvent(event)) {
			return true;
		}
		return false;
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
		
		mFilterGroup.setPhotoUpload(mAdapter.getItem(mViewPager.getCurrentItem()));
	}

	private void hideFiltersView() {
		if (null != mFilterGroup) {
			mFilterGroup.hide();
		}
	}

	public void onCheckedChanged(RadioGroup group, int checkedId) {
		
	}
	
	public void onPageSelected(int position) {
		if (null != mFilterGroup && mFilterGroup.getVisibility() == View.VISIBLE) {
			mFilterGroup.setPhotoUpload(mAdapter.getItem(position));
		}
	}

	public void onPageScrollStateChanged(int state) {
		// NO-OP
	}

	public void onPageScrolled(int position, float arg1, int arg2) {
		// NO-OP
	}
}
