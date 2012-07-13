package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.AlbumsAsyncTask.AlbumsResultListener;
import uk.co.senab.photup.fragments.NewAlbumFragment;
import uk.co.senab.photup.fragments.NewAlbumFragment.OnAlbumCreatedListener;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.service.PhotoUploadService;
import uk.co.senab.photup.service.PhotoUploadService.ServiceBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.android.FacebookError;
import com.lightbox.android.photoprocessing.R;

public class UploadActivity extends SherlockFragmentActivity implements ServiceConnection, AlbumsResultListener,
		OnClickListener, OnAlbumCreatedListener {

	private final ArrayList<Album> mAlbums = new ArrayList<Album>();

	private ServiceBinder<PhotoUploadService> mBinder;

	private RadioGroup mQualityRadioGroup;
	private Spinner mAlbumSpinner;
	private ImageButton mNewAlbumButton;

	private ArrayAdapter<Album> mAlbumAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_upload);

		mQualityRadioGroup = (RadioGroup) findViewById(R.id.rg_upload_quality);
		mAlbumSpinner = (Spinner) findViewById(R.id.sp_upload_album);
		mAlbumSpinner.setEnabled(false);

		mNewAlbumButton = (ImageButton) findViewById(R.id.btn_new_album);
		mNewAlbumButton.setOnClickListener(this);

		mAlbumAdapter = new ArrayAdapter<Album>(this, android.R.layout.simple_spinner_item, mAlbums);
		mAlbumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAlbumSpinner.setAdapter(mAlbumAdapter);

		bindService(new Intent(this, PhotoUploadService.class), this, Context.BIND_AUTO_CREATE);

		PhotupApplication.getApplication(this).getAlbums(this, false);
	}

	private void upload() {
		PhotupApplication.getApplication(this).getPhotoUploadController().moveSelectedPhotosToUploads();

		UploadQuality quality = UploadQuality.mapFromButtonId(mQualityRadioGroup.getCheckedRadioButtonId());
		Album album = (Album) mAlbumSpinner.getSelectedItem();

		if (null != album) {
			mBinder.getService().uploadAll(album, quality);
			finish();
		} else {
			Toast.makeText(this, getString(R.string.error_select_album), Toast.LENGTH_SHORT).show();
		}
	}

	private void checkConnectionSpeed() {
		ConnectivityManager mgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mgr.getActiveNetworkInfo();

		if (null != info) {
			int checkedId;

			switch (info.getType()) {
				case ConnectivityManager.TYPE_MOBILE: {
					if (info.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE
							|| info.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS) {
						checkedId = R.id.rb_quality_low;
					} else {
						checkedId = R.id.rb_quality_medium;
					}
				}

				default:
				case ConnectivityManager.TYPE_WIFI:
					checkedId = R.id.rb_quality_high;
					break;
			}

			RadioButton button = (RadioButton) mQualityRadioGroup.findViewById(checkedId);
			button.setChecked(true);
		}
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
	protected void onResume() {
		super.onResume();
		checkConnectionSpeed();
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

	public void onAlbumsLoaded(List<Album> albums) {
		mAlbums.clear();
		mAlbums.addAll(albums);
		mAlbumAdapter.notifyDataSetChanged();
		mAlbumSpinner.setEnabled(true);
	}

	public void onClick(View v) {
		NewAlbumFragment fragment = new NewAlbumFragment();
		fragment.show(getSupportFragmentManager(), "new_album");
	}

	public void onAlbumCreated() {
		PhotupApplication.getApplication(this).getAlbums(this, true);
	}

	public void onFacebookError(FacebookError e) {
		// NO-OP
	}

}
