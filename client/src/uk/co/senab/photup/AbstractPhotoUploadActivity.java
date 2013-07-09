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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.os.Bundle;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import uk.co.senab.photup.base.PhotupFragmentActivity;
import uk.co.senab.photup.events.UploadingPausedStateChangedEvent;
import uk.co.senab.photup.util.Utils;

public abstract class AbstractPhotoUploadActivity extends PhotupFragmentActivity {

    protected PhotoUploadController mPhotoController;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhotoController = PhotoUploadController.getFromContext(this);
        EventBus.getDefault().register(this);

        if (Utils.isUploadingPaused(this)) {
            showUploadingDisabledCrouton();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu.size() == 0) {
            getSupportMenuInflater().inflate(R.menu.menu_photo_grid_uploads, menu);
            setupPauseUploadingMenuItems(menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_uploading_stop:
                Utils.setUploadingPaused(this, true);
                EventBus.getDefault().post(new UploadingPausedStateChangedEvent());
                return true;

            case R.id.menu_uploading_start:
                Utils.setUploadingPaused(this, false);
                EventBus.getDefault().post(new UploadingPausedStateChangedEvent());
                startService(Utils.getUploadAllIntent(this));
                return true;

            case R.id.menu_retry_failed:
                mPhotoController.moveFailedToSelected();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupPauseUploadingMenuItems(Menu menu) {
        MenuItem pauseItem = menu.findItem(R.id.menu_uploading_stop);
        MenuItem startItem = menu.findItem(R.id.menu_uploading_start);
        if (null != pauseItem && null != startItem) {
            startItem.setVisible(Utils.isUploadingPaused(this));
            pauseItem.setVisible(!startItem.isVisible());
        }
    }

    public void onEvent(UploadingPausedStateChangedEvent event) {
        // TODO Should probably check whether we're showing the pause/resume
        // items before invalidating
        supportInvalidateOptionsMenu();

        if (Utils.isUploadingPaused(this)) {
            showUploadingDisabledCrouton();
        } else {
            showUploadingEnabledCrouton();
        }
    }

    protected final void showUploadingDisabledCrouton() {
        Crouton.cancelAllCroutons();
        Crouton.showText(this, R.string.stopped_uploads, Style.ALERT);
    }

    protected final void showUploadingEnabledCrouton() {
        Crouton.cancelAllCroutons();
        Crouton.showText(this, R.string.started_uploads, Style.CONFIRM);
    }

}
