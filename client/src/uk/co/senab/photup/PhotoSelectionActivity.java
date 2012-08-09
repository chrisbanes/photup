package uk.co.senab.photup;

import org.donations.DonationsActivity;

import uk.co.senab.photup.base.PhotupFragmentActivity;
import uk.co.senab.photup.fragments.SelectedPhotosFragment;
import uk.co.senab.photup.fragments.UploadsFragment;
import uk.co.senab.photup.fragments.UserPhotosFragment;
import uk.co.senab.photup.listeners.OnPhotoSelectionChangedListener;
import uk.co.senab.photup.model.PhotoSelection;
import uk.co.senab.photup.views.UploadActionBarView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

public class PhotoSelectionActivity extends PhotupFragmentActivity implements OnPhotoSelectionChangedListener,
		TabListener, OnClickListener {

	public static final String EXTRA_DEFAULT_TAB = "extra_tab";

	public static final int TAB_PHOTOS = 0;
	public static final int TAB_SELECTED = 1;
	static final int TAB_UPLOADS = 2;

	private UploadActionBarView mUploadActionView;

	private PhotoUploadController mPhotoController;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_photos);

		mPhotoController = PhotoUploadController.getFromContext(this);
		mPhotoController.addPhotoSelectionListener(this);

		ActionBar ab = getSupportActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ab.addTab(ab.newTab().setText(R.string.tab_photos).setTag(TAB_PHOTOS).setTabListener(this));
		ab.addTab(ab.newTab().setText(getSelectedTabTitle()).setTag(TAB_SELECTED).setTabListener(this));

		Intent intent = getIntent();
		int defaultTab = intent.getIntExtra(EXTRA_DEFAULT_TAB, -1);
		if (defaultTab != -1) {
			ab.setSelectedNavigationItem(defaultTab);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (getSupportActionBar().getSelectedNavigationIndex() <= TAB_SELECTED) {
			getSupportMenuInflater().inflate(R.menu.menu_photo_grid, menu);

			MenuItem item = menu.findItem(R.id.menu_upload);
			mUploadActionView = (UploadActionBarView) item.getActionView();
			mUploadActionView.setOnClickListener(this);
			refreshUploadActionBarView();
		} else {
			getSupportMenuInflater().inflate(R.menu.menu_photo_grid_uploads, menu);
			mUploadActionView = null;
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
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();
		showInstantUploadDialog();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mPhotoController.hasUploads()) {
			addUploadTab();
		}

		try {
			if (mPhotoController.getActiveUploadsSize() > 0) {
				// Load Uploads Tab if we need to
				getSupportActionBar().setSelectedNavigationItem(2);
			} else if (mPhotoController.getSelectedPhotoUploadsSize() == 0) {
				// Else just show Media Lib tab
				getSupportActionBar().setSelectedNavigationItem(0);
			}
		} catch (IllegalStateException e) {
			// Getting FCs. Not core function so just hide it if it happens
			e.printStackTrace();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// first saving my state, so the bundle wont be empty.
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	public void onSelectionsAddedToUploads() {
		addUploadTab();
		refreshSelectedTabTitle();
		refreshUploadActionBarView();
	}

	public void onPhotoSelectionChanged(PhotoSelection upload, boolean added) {
		refreshSelectedTabTitle();
		refreshUploadActionBarView();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mPhotoController.removePhotoSelectionListener(this);
	}

	private Tab mPreviouslySelectedTab;

	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		final int id = (Integer) tab.getTag();
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

		ft.replace(R.id.fl_fragment, fragment);
		supportInvalidateOptionsMenu();
	}

	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		mPreviouslySelectedTab = tab;
	}

	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// NO-OP
	}

	private void refreshUploadActionBarView() {
		if (null != mUploadActionView) {
			if (mPhotoController.getSelectedPhotoUploadsSize() > 0) {
				mUploadActionView.animateBackground();
			} else {
				mUploadActionView.stopAnimatingBackground();
			}
		}
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

	public void onUploadsCleared() {
		ActionBar ab = getSupportActionBar();

		// If we have 3 tabs...
		if (ab.getTabCount() == 3) {
			// If we're currently showing the tab, move to the first tab
			if (ab.getSelectedNavigationIndex() == TAB_UPLOADS) {
				ab.setSelectedNavigationItem(TAB_PHOTOS);
			}

			// Remove the tab
			ab.removeTabAt(TAB_UPLOADS);
		}
	}

	private void showInstantUploadDialog() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (prefs.getBoolean(PreferenceConstants.PREF_SHOWN_INSTANT_UPLOAD_DIALOG, false)) {
			// Already seen dialog
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(R.string.pref_instant_upload_title);
		builder.setMessage(R.string.dialog_instant_upload);

		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case AlertDialog.BUTTON_POSITIVE:
						startActivity(new Intent(PhotoSelectionActivity.this, SettingsActivity.class));
						break;
				}

				dialog.dismiss();
				prefs.edit().putBoolean(PreferenceConstants.PREF_SHOWN_INSTANT_UPLOAD_DIALOG, true).commit();
			}
		};

		builder.setPositiveButton(R.string.settings, listener);
		builder.setNegativeButton(android.R.string.cancel, listener);
		builder.show();
	}

	public void onClick(View v) {
		if (mPhotoController.getSelectedPhotoUploadsSize() == 0) {
			Toast.makeText(this, R.string.error_select_photos, Toast.LENGTH_SHORT).show();
		} else {
			startActivity(new Intent(this, UploadActivity.class));
		}
	}

}
