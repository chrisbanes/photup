package uk.co.senab.photup;

import org.donations.DonationsActivity;

import uk.co.senab.photup.base.PhotupFragmentActivity;
import uk.co.senab.photup.events.UploadsModifiedEvent;
import uk.co.senab.photup.fragments.SelectedPhotosFragment;
import uk.co.senab.photup.fragments.UploadFragment;
import uk.co.senab.photup.fragments.UploadsFragment;
import uk.co.senab.photup.fragments.UserPhotosFragment;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import uk.co.senab.photup.receivers.ConnectivityReceiver;
import uk.co.senab.photup.views.UploadActionBarView;
import uk.co.senab.photup.views.UploadsActionBarView;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.greenrobot.event.EventBus;

public class PhotoSelectionActivity extends PhotupFragmentActivity implements OnPhotoSelectionChangedListener,
		TabListener, OnClickListener {

	public static final String EXTRA_DEFAULT_TAB = "extra_tab";

	public static final int TAB_PHOTOS = 0;
	public static final int TAB_SELECTED = 1;
	static final int TAB_UPLOADS = 2;

	private UploadActionBarView mUploadActionView;
	private UploadsActionBarView mUploadsActionView;

	private PhotoUploadController mPhotoController;
	private boolean mSinglePane;

	private Tab mPreviouslySelectedTab;

	public void onClick(View v) {
		if (v == mUploadActionView) {
			if (mPhotoController.getSelectedCount() == 0) {
				Toast.makeText(this, R.string.error_select_photos, Toast.LENGTH_SHORT).show();
			} else {
				if (ConnectivityReceiver.isConnected(this)) {
					new UploadFragment().show(getSupportFragmentManager(), "upload");
				} else {
					Toast.makeText(this, R.string.error_not_connected, Toast.LENGTH_LONG).show();
				}
			}
		} else if (v == mUploadsActionView) {
			showUploads();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_photos);

		mSinglePane = null == findViewById(R.id.frag_secondary);

		mPhotoController = PhotoUploadController.getFromContext(this);
		mPhotoController.addListener(this);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.addTab(ab.newTab().setText(R.string.tab_photos).setTag(TAB_PHOTOS).setTabListener(this));

		if (mSinglePane) {
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			ab.addTab(ab.newTab().setTag(TAB_SELECTED).setTabListener(this));

			Intent intent = getIntent();
			int defaultTab = intent.getIntExtra(EXTRA_DEFAULT_TAB, -1);
			if (defaultTab != -1) {
				ab.setSelectedNavigationItem(defaultTab);
			}
		} else {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			replacePrimaryFragment(TAB_PHOTOS, ft);
			ft.commit();
			
			EventBus.getDefault().register(this);
		}

		refreshSelectedPhotosTitle();
	}
	
	public void onEvent(UploadsModifiedEvent event) {
		refreshUploadsActionBarView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mUploadActionView = null;
		mUploadsActionView = null;

		if (mSinglePane) {
			switch (getSupportActionBar().getSelectedNavigationIndex()) {
				case TAB_PHOTOS:
					// Shown when no tabs too!
					getSupportMenuInflater().inflate(R.menu.menu_photo_grid_users, menu);
					setupUploadActionBarView(menu);
					break;

				case TAB_SELECTED:
					getSupportMenuInflater().inflate(R.menu.menu_photo_grid_selected, menu);
					setupUploadActionBarView(menu);
					break;

				case TAB_UPLOADS:
					getSupportMenuInflater().inflate(R.menu.menu_photo_grid_uploads, menu);
					break;
			}
		} else {
			getSupportMenuInflater().inflate(R.menu.menu_photo_grid_large, menu);
			setupUploadActionBarView(menu);
			setupUploadsActionBarView(menu);
		}

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				startActivity(new Intent(this, LoginActivity.class));
				overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
				return true;

			case R.id.menu_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;

			case R.id.menu_donate:
				startActivity(new Intent(this, DonationsActivity.class));
				return true;

			case R.id.menu_logout:
				startActivity(new Intent(Constants.INTENT_LOGOUT));
				finish();
				return true;

			case R.id.menu_retry_failed:
				if (mPhotoController.moveFailedToSelected()) {
					getSupportActionBar().setSelectedNavigationItem(TAB_SELECTED);
				}
				return true;

			case R.id.menu_clear_selection:
				mPhotoController.clearSelected();
				return true;

			case R.id.menu_select_all:
				UserPhotosFragment fragment = (UserPhotosFragment) getSupportFragmentManager().findFragmentById(
						R.id.frag_primary);
				if (null != fragment) {
					fragment.selectAll();
				}
				return true;

			case R.id.menu_uploads:
				showUploads();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void showUploads() {
		UploadsFragment frag = new UploadsFragment();
		frag.show(getSupportFragmentManager(), "uploads");
	}

	public void onPhotoSelectionChanged(PhotoUpload upload, boolean added) {
		refreshSelectedPhotosTitle();
		refreshUploadActionBarView();
	}

	public void onPhotoSelectionsAdded() {
		refreshSelectedPhotosTitle();
		refreshUploadActionBarView();
	}

	public void onPhotoSelectionsCleared() {
		checkTabsAndMenu();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// first saving my state, so the bundle wont be empty.
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		final int id = (Integer) tab.getTag();
		replacePrimaryFragment(id, ft);

		// Refresh Action Bar so correct Menu is displayed
		supportInvalidateOptionsMenu();
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		mPreviouslySelectedTab = tab;
	}

	public void onUploadsCleared() {
		checkTabsAndMenu();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPhotoController.removeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkTabsAndMenu();
	}

	private void addUploadsTab() {
		if (null == getTabWithId(TAB_UPLOADS)) {
			ActionBar ab = getSupportActionBar();
			ab.addTab(ab.newTab().setText(R.string.tab_uploads).setTag(TAB_UPLOADS).setTabListener(this));
		}
	}

	private void checkTabsAndMenu() {
		if (mSinglePane) {
			try {
				if (mPhotoController.hasUploads()) {
					addUploadsTab();
				} else {
					removeUploadsTab();
				}

				if (mPhotoController.getActiveUploadsCount() > 0) {
					// Load Uploads Tab if we need to
					final int lastTabIndex = getSupportActionBar().getTabCount() - 1;
					getSupportActionBar().setSelectedNavigationItem(lastTabIndex);
				} else if (mPhotoController.getSelectedCount() == 0) {
					// Else just show Media Lib tab
					getSupportActionBar().setSelectedNavigationItem(0);
				}
			} catch (IllegalStateException e) {
				// Getting FCs. Not core function so just hide it if it happens
				e.printStackTrace();
			}

			// This only needs to be done for single pane, as invalidating the
			// action bar does it anyway (for dual pane).
			refreshUploadActionBarView();

		} else {
			// Refresh Action Bar so 'Uploads' item is correctly visible/not.
			supportInvalidateOptionsMenu();
		}

		refreshSelectedPhotosTitle();
	}

	private CharSequence formatSelectedFragmentTitle() {
		return getString(R.string.tab_selected_photos, mPhotoController.getSelectedCount());
	}

	private Tab getTabWithId(final int id) {
		ActionBar ab = getSupportActionBar();
		Tab tab;
		for (int i = 0, z = ab.getTabCount(); i < z; i++) {
			tab = ab.getTabAt(i);
			if (((Integer) tab.getTag()) == id) {
				return tab;
			}
		}
		return null;
	}

	private void refreshSelectedPhotosTitle() {
		if (mSinglePane) {
			getSupportActionBar().getTabAt(1).setText(formatSelectedFragmentTitle());
		} else {
			SelectedPhotosFragment userPhotos = (SelectedPhotosFragment) getSupportFragmentManager().findFragmentById(
					R.id.frag_secondary);
			if (null != userPhotos) {
				userPhotos.setFragmentTitle(formatSelectedFragmentTitle());
			}
		}
	}

	private void refreshUploadActionBarView() {
		if (null != mUploadActionView) {
			if (mPhotoController.getSelectedCount() > 0) {
				mUploadActionView.animateBackground();
			} else {
				mUploadActionView.stopAnimatingBackground();
			}
		}
	}

	private void refreshUploadsActionBarView() {
		if (null != mUploadsActionView) {
			int total = mPhotoController.getUploadsCount();
			int active = mPhotoController.getActiveUploadsCount();
			mUploadsActionView.updateProgress(total - active, total);
		}
	}

	private void removeUploadsTab() {
		final Tab uploadsTab = getTabWithId(TAB_UPLOADS);
		if (null != uploadsTab) {
			// Move to the first tab, then remove the tab
			ActionBar ab = getSupportActionBar();
			ab.setSelectedNavigationItem(TAB_PHOTOS);
			ab.removeTab(uploadsTab);
		}
	}

	private void replacePrimaryFragment(int id, FragmentTransaction ft) {
		Fragment fragment;

		switch (id) {
			case TAB_SELECTED:
				fragment = new SelectedPhotosFragment();
				break;
			case TAB_UPLOADS:
				fragment = new UploadsFragment();
				break;
			case TAB_PHOTOS:
			default:
				fragment = new UserPhotosFragment();
				break;
		}

		if (null != mPreviouslySelectedTab) {
			final int oldId = (Integer) mPreviouslySelectedTab.getTag();
			final int enterAnim = id > oldId ? R.anim.slide_in_right : R.anim.slide_in_left;
			final int exitAnim = id > oldId ? R.anim.slide_out_left : R.anim.slide_out_right;
			ft.setCustomAnimations(enterAnim, exitAnim);
		}

		ft.replace(R.id.frag_primary, fragment);
	}

	private void setupUploadActionBarView(Menu menu) {
		MenuItem item = menu.findItem(R.id.menu_upload);
		mUploadActionView = (UploadActionBarView) item.getActionView();
		mUploadActionView.setOnClickListener(this);
		refreshUploadActionBarView();
	}

	private void setupUploadsActionBarView(Menu menu) {
		if (!mSinglePane) {
			MenuItem uploadsItem = menu.findItem(R.id.menu_uploads);
			mUploadsActionView = (UploadsActionBarView) uploadsItem.getActionView();
			mUploadsActionView.setOnClickListener(this);
			uploadsItem.setVisible(mPhotoController.hasUploads());
			refreshUploadsActionBarView();
		}
	}
}
