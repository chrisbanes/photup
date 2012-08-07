package uk.co.senab.photup.base;

import uk.co.senab.photup.Analytics;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class PhotupFragmentActivity extends SherlockFragmentActivity {

	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onStartSession(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onEndSession(this);
	}

}
