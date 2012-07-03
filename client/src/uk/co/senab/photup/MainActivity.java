package uk.co.senab.photup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.facebook.Session;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class MainActivity extends Activity {

	static final int RESULT_FACEBOOK_AUTH = 100;

	private Facebook mFacebook;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Session session = Session.restore(this);
		if (null == session) {
			loginToFacebook();
		} else {
			launchSelectionActivity();
		}
	}

	private void loginToFacebook() {
		mFacebook = new Facebook(Constants.FACEBOOK_APP_ID);
		mFacebook.authorize(this, Constants.FACEBOOK_PERMISSIONS, RESULT_FACEBOOK_AUTH, new DialogListener() {

			public void onFacebookError(FacebookError e) {
				e.printStackTrace();
			}

			public void onError(DialogError e) {
				e.printStackTrace();
			}

			public void onComplete(Bundle values) {
				saveFacebookSession();
			}

			public void onCancel() {
				finish();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mFacebook.authorizeCallback(requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void launchSelectionActivity() {
		startActivity(new Intent(this, PhotoSelectionActivity.class));
		finish();
	}

	private void saveFacebookSession() {
		AsyncFacebookRunner fbRunner = new AsyncFacebookRunner(mFacebook);
		fbRunner.request("me", new RequestListener() {

			public void onMalformedURLException(MalformedURLException e, Object state) {
				e.printStackTrace();
			}

			public void onIOException(IOException e, Object state) {
				e.printStackTrace();
			}

			public void onFileNotFoundException(FileNotFoundException e, Object state) {
				e.printStackTrace();
			}

			public void onFacebookError(FacebookError e, Object state) {
				e.printStackTrace();
			}

			public void onComplete(String response, Object state) {
				try {
					JSONObject object = new JSONObject(response);
					String id = object.getString("id");
					String name = object.getString("name");

					Session session = new Session(mFacebook, id, name);
					session.save(getApplicationContext());
					
					launchSelectionActivity();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}

}
