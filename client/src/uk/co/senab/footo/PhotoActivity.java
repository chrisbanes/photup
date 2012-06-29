package uk.co.senab.footo;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import uk.co.senab.photup.R;

public class PhotoActivity extends FragmentActivity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}
}
