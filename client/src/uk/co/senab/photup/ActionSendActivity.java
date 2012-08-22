package uk.co.senab.photup;

import java.util.ArrayList;

import uk.co.senab.photup.model.PhotoUpload;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ActionSendActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent sharedIntent = getIntent();
		final String action = sharedIntent.getAction();

		ArrayList<Uri> uris = null;
		if (Intent.ACTION_SEND.equals(action)) {
			Uri uri = sharedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
			if (null != uri) {
				uris = new ArrayList<Uri>();
				uris.add(uri);
			}
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			uris = sharedIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
		}

		if (null != uris && !uris.isEmpty()) {
			PhotoUploadController controller = PhotupApplication.getApplication(this).getPhotoUploadController();
			for (Uri uri : uris) {
				controller.addPhotoSelection(PhotoUpload.getSelection(uri));
			}
		}

		Intent intent = new Intent(this, PhotoSelectionActivity.class);
		intent.putExtra(PhotoSelectionActivity.EXTRA_DEFAULT_TAB, PhotoSelectionActivity.TAB_SELECTED);
		startActivity(intent);

		finish();
	}

}
