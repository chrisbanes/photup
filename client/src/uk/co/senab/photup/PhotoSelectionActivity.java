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
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import uk.co.senab.photup.events.PhotoSelectionAddedEvent;
import uk.co.senab.photup.events.PhotoSelectionRemovedEvent;
import uk.co.senab.photup.events.UploadsModifiedEvent;
import uk.co.senab.photup.events.UploadsStartEvent;
import uk.co.senab.photup.fragments.SelectedPhotosFragment;
import uk.co.senab.photup.fragments.UploadFragment;
import uk.co.senab.photup.fragments.UploadsFragment;
import uk.co.senab.photup.fragments.UserPhotosFragment;
import uk.co.senab.photup.views.UploadActionBarView;
import uk.co.senab.photup.views.UploadsActionBarView;

public class PhotoSelectionActivity extends AbstractPhotoUploadActivity
        implements TabListener, OnClickListener {

    public static final String EXTRA_DEFAULT_TAB = "extra_tab";

    public static final int TAB_PHOTOS = 0;
    public static final int TAB_SELECTED = 1;
    public static final int TAB_UPLOADS = 2;

    private UploadActionBarView mUploadActionView;
    private UploadsActionBarView mUploadsActionView;

    private boolean mSinglePane;

    private Tab mPreviouslySelectedTab;

    public void onClick(View v) {
        if (v == mUploadActionView) {
            if (!mPhotoController.hasSelections()) {
                Toast.makeText(this, R.string.error_select_photos, Toast.LENGTH_SHORT).show();
            } else {
                new UploadFragment().show(getSupportFragmentManager(), "upload");
            }
        } else if (v == mUploadsActionView) {
            startUploadsActivity();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_choose_photos);
        mSinglePane = null == findViewById(R.id.frag_secondary);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.addTab(ab.newTab().setText(R.string.tab_photos).setTag(TAB_PHOTOS).setTabListener(this));

        if (mSinglePane) {
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            ab.addTab(ab.newTab().setTag(TAB_SELECTED).setTabListener(this));
            ab.addTab(ab.newTab().setText(R.string.tab_uploads).setTag(TAB_UPLOADS)
                    .setTabListener(this));

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
                    // Happens in super...
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

    public void onEvent(UploadsStartEvent event) {
        ActionBar ab = getSupportActionBar();
        if (ab.getNavigationMode() != ActionBar.NAVIGATION_MODE_STANDARD) {
            ab.setSelectedNavigationItem(TAB_UPLOADS);
        } else {
            startUploadsActivity();
        }
    }

    public void onEventMainThread(UploadsModifiedEvent event) {
        refreshTabMenuItems();
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

            case R.id.menu_logout:
                startActivity(new Intent(Constants.INTENT_LOGOUT));
                finish();
                return true;

            case R.id.menu_clear_selection:
                mPhotoController.clearSelected();
                return true;

            case R.id.menu_select_all:
                UserPhotosFragment fragment = (UserPhotosFragment) getSupportFragmentManager()
                        .findFragmentById(
                                R.id.frag_primary);
                if (null != fragment) {
                    fragment.selectAll();
                }
                return true;

            case R.id.menu_uploads:
                startUploadsActivity();
                return true;

            case R.id.menu_retry_failed:
            case R.id.menu_uploading_stop:
            case R.id.menu_uploading_start:
                // Handled in super
                break;
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
    protected void onResume() {
        super.onResume();
        refreshTabMenuItems();
    }

    private CharSequence formatSelectedFragmentTitle() {
        return getString(R.string.tab_selected_photos, mPhotoController.getSelectedCount());
    }

    private void refreshSelectedPhotosTitle() {
        if (mSinglePane) {
            getSupportActionBar().getTabAt(1).setText(formatSelectedFragmentTitle());
        } else {
            SelectedPhotosFragment userPhotos = (SelectedPhotosFragment) getSupportFragmentManager()
                    .findFragmentById(
                            R.id.frag_secondary);
            if (null != userPhotos) {
                userPhotos.setFragmentTitle(formatSelectedFragmentTitle());
            }
        }
    }

    private void refreshTabMenuItems() {
        refreshUploadActionBarView();
        refreshUploadsActionBarView();
        refreshSelectedPhotosTitle();
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

    private void startUploadsActivity() {
        startActivity(new Intent(this, PhotoUploadsActivity.class));
    }
}
