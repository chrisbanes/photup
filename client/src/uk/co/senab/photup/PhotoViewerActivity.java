package uk.co.senab.photup;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class PhotoViewerActivity extends Activity {
	
	private ViewPager mViewPager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_photo_viewer);
		
		mViewPager = (ViewPager) findViewById(R.id.vp_photos);
	}

}
