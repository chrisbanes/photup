package uk.co.senab.photup;

import uk.co.senab.photup.facebook.Session;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.android.Facebook.ServiceListener;
import com.facebook.android.FacebookError;

public class MainActivity extends Activity {

	static final int REQUEST_FACEBOOK_LOGIN = 99;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Session session = Session.restore(this);
		if (null == session) {
			launchLoginActivity();
		} else {
			launchSelectionActivity(session);
		}
	}

	private void launchLoginActivity() {
		startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_FACEBOOK_LOGIN);
	}

	private void launchSelectionActivity(final Session session) {
		// Extend Access Token if we're not on a debug build
		if (!Flags.DEBUG) {
			session.getFb().extendAccessTokenIfNeeded(getApplicationContext(), new ServiceListener() {
				public void onFacebookError(FacebookError e) {
					e.printStackTrace();
				}

				public void onError(Error e) {
					e.printStackTrace();
				}

				public void onComplete(Bundle values) {
					session.save(getApplicationContext());
				}
			});
		}

		startActivity(new Intent(this, PhotoSelectionActivity.class));
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_FACEBOOK_LOGIN:
				Session session = Session.restore(this);
				if (resultCode == RESULT_OK && null != session) {
					// Refresh Accounts
					PhotupApplication.getApplication(getApplicationContext()).getAccounts(null, true);

					// Start Selection Activity
					launchSelectionActivity(session);
				} else {
					finish();
				}
				return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

}
