package uk.co.senab.photup;

import java.util.HashSet;

import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.fragments.SelectedPhotosFragment;
import uk.co.senab.photup.fragments.UserPhotosFragment;
import uk.co.senab.photup.listeners.BitmapCacheProvider;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ViewAnimator;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PhotoActivity extends SherlockFragmentActivity implements OnPhotoSelectionChangedListener,
		BitmapCacheProvider, TabListener {

	static final int TAB_PHOTOS = 0;
	static final int TAB_SELECTED = 1;

	private HashSet<PhotoUpload> mPhotoUploads = new HashSet<PhotoUpload>();
	private BitmapLruCache mCache;

	private ViewAnimator mFlipper;

	private UserPhotosFragment mUserPhotosFragment;
	private SelectedPhotosFragment mSelectedPhotosFragment;

	private Animation mSlideInLeftAnim, mSlideOutLeftAnim;
	private Animation mSlideInRightAnim, mSlideOutRightAnim;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Needs to be done before super.onCreate
		mCache = new BitmapLruCache(this);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mFlipper = (ViewAnimator) findViewById(R.id.vs_frag_flipper);

		mSlideInLeftAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
		mSlideOutLeftAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
		mSlideInRightAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
		mSlideOutRightAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

		FragmentManager fm = getSupportFragmentManager();
		mUserPhotosFragment = (UserPhotosFragment) fm.findFragmentById(R.id.frag_photo_grid);
		mSelectedPhotosFragment = (SelectedPhotosFragment) fm.findFragmentById(
				R.id.frag_selected_photos);

		ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ab.addTab(ab.newTab().setText(R.string.tab_photos).setTag(TAB_PHOTOS).setTabListener(this));
		ab.addTab(ab.newTab().setText(getSelectedTabTitle()).setTag(TAB_SELECTED).setTabListener(this));

		setCorrectAnimations(0);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu_photo_grid, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	public void onPhotoChosen(PhotoUpload upload, boolean added) {
		if (added) {
			mPhotoUploads.add(upload);
		} else {
			mPhotoUploads.remove(upload);
		}

		getSupportActionBar().getTabAt(1).setText(getSelectedTabTitle());

		mUserPhotosFragment.setSelectedUploads(mPhotoUploads);
		mSelectedPhotosFragment.setSelectedUploads(mPhotoUploads);
	}

	private CharSequence getSelectedTabTitle() {
		return getString(R.string.tab_selected_photos, mPhotoUploads.size());
	}

	private void setCorrectAnimations(final int currentPosition) {
		if (currentPosition == 0) {
			mFlipper.setInAnimation(mSlideInRightAnim);
			mFlipper.setOutAnimation(mSlideOutLeftAnim);
		} else {
			mFlipper.setInAnimation(mSlideInLeftAnim);
			mFlipper.setOutAnimation(mSlideOutRightAnim);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		mCache.evictAll();
	}

	public BitmapLruCache getBitmapCache() {
		return mCache;
	}

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		final int id = (Integer) tab.getTag();
		mFlipper.setDisplayedChild(id);

		// Set correct animations for next flip
		setCorrectAnimations(tab.getPosition());
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

}
