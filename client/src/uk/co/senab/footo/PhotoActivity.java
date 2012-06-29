package uk.co.senab.footo;

import uk.co.senab.photup.R;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class PhotoActivity extends SherlockFragmentActivity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}
}
