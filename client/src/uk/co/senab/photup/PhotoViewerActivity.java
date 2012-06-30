package uk.co.senab.photup;

import uk.co.senab.photup.adapters.PhotoViewPagerAdapter;
import uk.co.senab.photup.listeners.OnUploadChangedListener;
import uk.co.senab.photup.model.PhotoUpload;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

public class PhotoViewerActivity extends Activity implements OnUploadChangedListener {

	private ViewPager mViewPager;
	private PagerAdapter mAdapter;

	private PhotoSelectionController mController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_photo_viewer);

		mViewPager = (ViewPager) findViewById(R.id.vp_photos);
		mAdapter = new PhotoViewPagerAdapter(this, null);
		mViewPager.setAdapter(mAdapter);
		mAdapter.notifyDataSetChanged();

		mController = PhotoSelectionController.getFromContext(this);
		mController.addPhotoSelectionListener(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mController.removePhotoSelectionListener(this);
	}

	public void onUploadChanged(PhotoUpload upload, boolean added) {
		mAdapter.notifyDataSetChanged();
	}

}
