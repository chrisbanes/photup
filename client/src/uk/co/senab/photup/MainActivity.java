package uk.co.senab.photup;

import uk.co.senab.photup.facebook.Session;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

	static final int REQUEST_FACEBOOK_LOGIN = 99;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Session session = Session.restore(this);
		if (null == session) {
			launchLoginActivity();
		} else {
			launchSelectionActivity();
		}
	}

	private void launchLoginActivity() {
		startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_FACEBOOK_LOGIN);
	}

	private void launchSelectionActivity() {
		startActivity(new Intent(this, PhotoSelectionActivity.class));
		finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_FACEBOOK_LOGIN:
				if (resultCode == RESULT_OK) {
					launchSelectionActivity();
				} else {
					finish();
				}
				return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

}
