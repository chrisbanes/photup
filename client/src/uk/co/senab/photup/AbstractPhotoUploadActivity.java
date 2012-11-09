package uk.co.senab.photup;

import uk.co.senab.photup.base.PhotupFragmentActivity;
import uk.co.senab.photup.events.UploadingPausedStateChangedEvent;
import uk.co.senab.photup.util.Utils;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.greenrobot.event.EventBus;
import de.neofonie.mobile.app.android.widget.crouton.Crouton;
import de.neofonie.mobile.app.android.widget.crouton.Style;

public abstract class AbstractPhotoUploadActivity extends PhotupFragmentActivity {

	protected PhotoUploadController mPhotoController;

	/** Called when the activity is first created. */
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
