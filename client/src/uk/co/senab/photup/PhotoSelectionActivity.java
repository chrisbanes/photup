package uk.co.senab.photup;

import org.donations.DonationsActivity;

import uk.co.senab.photup.base.PhotupFragmentActivity;
import uk.co.senab.photup.events.PhotoSelectionAddedEvent;
import uk.co.senab.photup.events.PhotoSelectionRemovedEvent;
import uk.co.senab.photup.events.UploadingPausedStateChangedEvent;
import uk.co.senab.photup.events.UploadsModifiedEvent;
import uk.co.senab.photup.fragments.SelectedPhotosFragment;
import uk.co.senab.photup.fragments.UploadFragment;
import uk.co.senab.photup.fragments.UploadsFragment;
import uk.co.senab.photup.fragments.UserPhotosFragment;
import uk.co.senab.photup.receivers.ConnectivityReceiver;
import uk.co.senab.photup.util.Utils;
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
import de.neofonie.mobile.app.android.widget.crouton.Crouton;
import de.neofonie.mobile.app.android.widget.crouton.Style;

public class PhotoSelectionActivity extends PhotupFragmentActivity implements TabListener, OnClickListener {

	public static final String EXTRA_DEFAULT_TAB = "extra_tab";

	public static final int TAB_PHOTOS = 0;
	public static final int TAB_SELECTED = 1;
	public static final int TAB_UPLOADS = 2;

	private UploadActionBarView mUploadActionView;
	private UploadsActionBarView mUploadsActionView;

	private PhotoUploadController mPhotoController;
	private boolean mSinglePane;

	private Tab mPreviouslySelectedTab;

	public void onClick(View v) {
		if (v == mUploadActionView) {
			if (!mPhotoController.hasSelections()) {
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
		EventBus.getDefault().register(this);

		setContentView(R.layout.activity_choose_photos);
		mSinglePane = null == findViewById(R.id.frag_secondary);

		mPhotoController = PhotoUploadController.getFromContext(this);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.addTab(ab.newTab().setText(R.string.tab_photos).setTag(TAB_PHOTOS).setTabListener(this));

		if (mSinglePane) {
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			ab.addTab(ab.newTab().setTag(TAB_SELECTED).setTabListener(this));
			ab.addTab(ab.newTab().setText(R.string.tab_uploads).setTag(TAB_UPLOADS).setTabListener(this));

			Intent intent = getIntent();
			int defaultTab = intent.getIntExtra(EXTRA_DEFAULT_TAB, -1);
			if (defaultTab != -1) {
				ab.setSelectedNavigationItem(defaultTab);
			}
		} else {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			replacePrimaryFragment(TAB_PHOTOS, ft);
			ft.commit();
		}

		refreshSelectedPhotosTitle();
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
					setupPauseUploadingMenuItems(menu);
					break;
			}
		} else {
			getSupportMenuInflater().inflate(R.menu.menu_photo_grid_large, menu);
			setupUploadActionBarView(menu);
			setupUploadsActionBarView(menu);
		}

		return super.onCreateOptionsMenu(menu);
	}

	public void onEvent(PhotoSelectionAddedEvent event) {
		refreshSelectedPhotosTitle();
		refreshUploadActionBarView();
	}

	public void onEvent(PhotoSelectionRemovedEvent event) {
		refreshSelectedPhotosTitle();
		refreshUploadActionBarView();
	}

	public void onEvent(UploadingPausedStateChangedEvent event) {
		// TODO Should probably check whether we're showing the pause/resume
		// items before invalidating
		supportInvalidateOptionsMenu();

		Crouton.cancelAllCroutons();
		if (Utils.isUploadingPaused(this)) {
			Crouton.showText(this, R.string.paused_uploads, Style.ALERT);
		} else {
			Crouton.showText(this, R.string.started_uploads, Style.CONFIRM);
		}
	}

	public void onEventMainThread(UploadsModifiedEvent event) {
		checkTabsAndMenu();
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

			case R.id.menu_uploading_pause:
				Utils.setUploadingPaused(this, true);
				EventBus.getDefault().post(new UploadingPausedStateChangedEvent());
				return true;

			case R.id.menu_uploading_start:
				Utils.setUploadingPaused(this, false);
				EventBus.getDefault().post(new UploadingPausedStateChangedEvent());
				startService(Utils.getUploadAllIntent(this));
				return true;
		}

		return super.onOptionsItemSelected(item);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		EventBus.getDefault().unregister(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		checkTabsAndMenu();
	}

	private void checkTabsAndMenu() {
		if (mSinglePane) {
			try {
				if (mPhotoController.getActiveUploadsCount() > 0) {
					// Load Uploads Tab if we need to
					final int lastTabIndex = getSupportActionBar().getTabCount() - 1;
					getSupportActionBar().setSelectedNavigationItem(lastTabIndex);
				} else if (!mPhotoController.hasSelections()) {
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
			refreshUploadsActionBarView();
		}

		refreshSelectedPhotosTitle();
	}

	private CharSequence formatSelectedFragmentTitle() {
		return getString(R.string.tab_selected_photos, mPhotoController.getSelectedCount());
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
			if (mPhotoController.hasSelections()) {
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

	private void setupPauseUploadingMenuItems(Menu menu) {
		MenuItem pauseItem = menu.findItem(R.id.menu_uploading_pause);
		MenuItem startItem = menu.findItem(R.id.menu_uploading_start);
		if (null != pauseItem && null != startItem) {
			startItem.setVisible(Utils.isUploadingPaused(this));
			pauseItem.setVisible(!startItem.isVisible());
		}
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

			if (uploadsItem.isVisible()) {
				mUploadsActionView = (UploadsActionBarView) uploadsItem.getActionView();
				mUploadsActionView.setOnClickListener(this);
				refreshUploadsActionBarView();
			}
		}
	}

	private void showUploads() {
		UploadsFragment frag = new UploadsFragment();
		frag.show(getSupportFragmentManager(), "uploads");
	}
}
