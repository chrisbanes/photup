package uk.co.senab.photup;

import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoSelection;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class PhotoSelectionActivity extends SherlockFragmentActivity implements OnPhotoSelectionChangedListener,
		TabListener {

	static final int TAB_PHOTOS = 0;
	static final int TAB_SELECTED = 1;
	static final int TAB_UPLOADS = 2;

	private ViewAnimator mFlipper;

	private PhotoUploadController mPhotoController;

	private Animation mSlideInLeftAnim, mSlideOutLeftAnim;
	private Animation mSlideInRightAnim, mSlideOutRightAnim;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_photos);

		mPhotoController = PhotoUploadController.getFromContext(this);
		mPhotoController.addPhotoSelectionListener(this);

		mFlipper = (ViewAnimator) findViewById(R.id.vs_frag_flipper);

		mSlideInLeftAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
		mSlideOutLeftAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
		mSlideInRightAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
		mSlideOutRightAnim = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ab.addTab(ab.newTab().setText(R.string.tab_photos).setTag(TAB_PHOTOS).setTabListener(this));
		ab.addTab(ab.newTab().setText(getSelectedTabTitle()).setTag(TAB_SELECTED).setTabListener(this));

		setCorrectAnimations(0, 1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (getSupportActionBar().getSelectedNavigationIndex() < 2) {
			getSupportMenuInflater().inflate(R.menu.menu_photo_grid, menu);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.menu_upload:
				if (mPhotoController.getSelectedPhotoUploadsSize() == 0) {
					Toast.makeText(this, R.string.error_select_photos, Toast.LENGTH_SHORT).show();
				} else {
					startActivity(new Intent(this, UploadActivity.class));
				}
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mPhotoController.hasUploads()) {
			addUploadTab();
		}

		if (mPhotoController.getActiveUploadsSize() > 0) {
			getSupportActionBar().setSelectedNavigationItem(2);
		} else if (mPhotoController.getSelectedPhotoUploadsSize() == 0) {
			getSupportActionBar().setSelectedNavigationItem(0);
		}
	}

	public void onSelectionsAddedToUploads() {
		addUploadTab();
		refreshSelectedTabTitle();
	}

	public void onPhotoSelectionChanged(PhotoSelection upload, boolean added) {
		refreshSelectedTabTitle();
	}

	private void setCorrectAnimations(final int currentPosition, final int newPosition) {
		if (newPosition > currentPosition) {
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
		mPhotoController.removePhotoSelectionListener(this);
	}

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		final int id = (Integer) tab.getTag();

		// Set correct animations for next flip
		setCorrectAnimations(mFlipper.getDisplayedChild(), tab.getPosition());

		mFlipper.setDisplayedChild(id);

		supportInvalidateOptionsMenu();
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

	private void refreshSelectedTabTitle() {
		getSupportActionBar().getTabAt(1).setText(getSelectedTabTitle());
	}

	private CharSequence getSelectedTabTitle() {
		return getString(R.string.tab_selected_photos, mPhotoController.getSelectedPhotoUploadsSize());
	}

	private void addUploadTab() {
		ActionBar ab = getSupportActionBar();
		
		// Bit of a hack as but we expect the upload tab to be the third
		if (ab.getTabCount() == 2) {
			ab.addTab(ab.newTab().setText(R.string.tab_uploads).setTag(TAB_UPLOADS).setTabListener(this));
		}
	}

}
