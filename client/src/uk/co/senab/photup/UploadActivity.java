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
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
		mNewAlbumButton = (ImageButton) findViewById(R.id.btn_new_album);

		mNewAlbumButton.setOnClickListener(this);

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

		if (null != album) {
			mBinder.getService().uploadAll(album.getId(), quality);
			finish();
		} else {
			Toast.makeText(this, getString(R.string.error_select_album), Toast.LENGTH_SHORT).show();
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
	}

	public void onClick(View v) {
		NewAlbumFragment fragment = new NewAlbumFragment();
		fragment.show(getSupportFragmentManager(), "new_album");
	}

	public void onAlbumCreated() {
		new AlbumsAsyncTask(this, this).execute();
	}

}
