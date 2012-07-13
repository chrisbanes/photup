package uk.co.senab.photup;

import android.os.Bundle;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockActivity;

public class LicencesActivity extends SherlockActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WebView wv = new WebView(this);
		setContentView(wv);

		wv.loadUrl("file:///android_asset/libraries.html");
	}

}
