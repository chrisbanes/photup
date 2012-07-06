package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.AlbumsAsyncTask.AlbumsResultListener;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.service.PhotoUploadService;
import uk.co.senab.photup.service.PhotoUploadService.ServiceBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.lightbox.android.photoprocessing.R;

public class UploadActivity extends SherlockActivity implements ServiceConnection, AlbumsResultListener {

	private final ArrayList<Album> mAlbums = new ArrayList<Album>();

	private ServiceBinder<PhotoUploadService> mBinder;

	private RadioGroup mQualityRadioGroup;
	private Spinner mAlbumSpinner, mPrivacySpinner;

	private ArrayAdapter<Album> mAlbumAdapter;

	private EditText mNewAlbumEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_upload);

		mQualityRadioGroup = (RadioGroup) findViewById(R.id.rg_upload_quality);
		mAlbumSpinner = (Spinner) findViewById(R.id.sp_upload_album);
		mNewAlbumEditText = (EditText) findViewById(R.id.et_album_name);
		mPrivacySpinner = (Spinner) findViewById(R.id.sp_privacy);

		mAlbumAdapter = new ArrayAdapter<Album>(this, android.R.layout.simple_spinner_dropdown_item, mAlbums);
		mAlbumSpinner.setAdapter(mAlbumAdapter);

		bindService(new Intent(this, PhotoUploadService.class), this, Context.BIND_AUTO_CREATE);

		if (mAlbums.isEmpty()) {
			new AlbumsAsyncTask(this, this).execute();
		}
	}

	private void upload() {
		UploadQuality quality = UploadQuality.mapFromButtonId(mQualityRadioGroup.getCheckedRadioButtonId());
		Album album = (Album) mAlbumSpinner.getSelectedItem();

		mBinder.getService().uploadAll(album.getId(), quality);
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

	public void onAlbumsLoaded(List<Album> albums) {
		mAlbums.addAll(albums);
		mAlbumAdapter.notifyDataSetChanged();
	}

}
