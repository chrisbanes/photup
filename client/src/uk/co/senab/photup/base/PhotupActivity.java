package uk.co.senab.photup.base;

import uk.co.senab.photup.Analytics;

import com.actionbarsherlock.app.SherlockActivity;

public class PhotupActivity extends SherlockActivity {
	
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
