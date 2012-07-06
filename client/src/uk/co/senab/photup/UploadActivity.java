package uk.co.senab.photup;

import uk.co.senab.photup.service.PhotoUploadService;
import uk.co.senab.photup.service.PhotoUploadService.ServiceBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.lightbox.android.photoprocessing.R;

public class UploadActivity extends SherlockActivity implements ServiceConnection {

	private ServiceBinder<PhotoUploadService> mBinder;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bindService(new Intent(this, PhotoUploadService.class), this, Context.BIND_AUTO_CREATE);

		setContentView(R.layout.activity_upload);
	}

	private void upload() {
		mBinder.getService().uploadAll("609691583660");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu_photo_upload, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.menu_upload:
				upload();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(this);
	}

	@SuppressWarnings("unchecked")
	public void onServiceConnected(ComponentName name, IBinder service) {
		mBinder = (ServiceBinder<PhotoUploadService>) service;
	}

	public void onServiceDisconnected(ComponentName name) {
		mBinder = null;
	}

}
