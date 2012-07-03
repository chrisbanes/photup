package uk.co.senab.photup;

import uk.co.senab.photup.service.PhotoUploadService;
import uk.co.senab.photup.service.PhotoUploadService.ServiceBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class UploadActivity extends Activity implements ServiceConnection {
	
	private ServiceBinder<PhotoUploadService> mBinder;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bindService(new Intent(this, PhotoUploadService.class), this, Context.BIND_AUTO_CREATE);
		
		Button button = new Button(this);
		button.setText("Upload");
		button.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				mBinder.getService().uploadAll("609691583660");
			}
		});
		
		setContentView(button);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(this);
	}

	public void onServiceConnected(ComponentName name, IBinder service) {
		mBinder = (ServiceBinder<PhotoUploadService>) service;
	}

	public void onServiceDisconnected(ComponentName name) {
		mBinder = null;
	}

}
