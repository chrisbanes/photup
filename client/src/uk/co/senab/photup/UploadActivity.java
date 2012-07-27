package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.fragments.NewAlbumFragment;
import uk.co.senab.photup.fragments.NewAlbumFragment.OnAlbumCreatedListener;
import uk.co.senab.photup.fragments.PlacesListFragment;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.service.PhotoUploadService;
import uk.co.senab.photup.service.PhotoUploadService.ServiceBinder;
import uk.co.senab.photup.tasks.AccountsAsyncTask.AccountsResultListener;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;
import uk.co.senab.photup.views.NetworkedCacheableImageView;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.android.FacebookError;
import com.lightbox.android.photoprocessing.R;

public class UploadActivity extends SherlockFragmentActivity implements ServiceConnection, AlbumsResultListener,
		AccountsResultListener, OnClickListener, OnAlbumCreatedListener, OnPlacePickedListener, OnItemSelectedListener {
	
	static final int REQUEST_FACEBOOK_LOGIN = 99;

	private final ArrayList<Album> mAlbums = new ArrayList<Album>();
	private final ArrayList<Account> mAccounts = new ArrayList<Account>();

	private ServiceBinder<PhotoUploadService> mBinder;

	private RadioGroup mQualityRadioGroup;
	private Spinner mAlbumSpinner, mAccountsSpinner;
	private ImageButton mNewAlbumButton;
	private TextView mPlacesButton;
	private NetworkedCacheableImageView mPlacesIcon;
	private View mPlacesLayout;
	
	private ImageButton mAccountHelpBtn;

	private View mAlbumSpinnerLayout, mAlbumTitleTv;

	private Place mPlace;

	private ArrayAdapter<Album> mAlbumAdapter;
	private ArrayAdapter<Account> mAccountsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_upload);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mQualityRadioGroup = (RadioGroup) findViewById(R.id.rg_upload_quality);
		mAlbumSpinner = (Spinner) findViewById(R.id.sp_upload_album);
		mAlbumSpinner.setEnabled(false);

		mAccountsSpinner = (Spinner) findViewById(R.id.sp_upload_account);
		mAccountsSpinner.setEnabled(false);
		mAccountsSpinner.setOnItemSelectedListener(this);

		mNewAlbumButton = (ImageButton) findViewById(R.id.btn_new_album);
		mNewAlbumButton.setOnClickListener(this);

		mAlbumAdapter = new ArrayAdapter<Album>(this, android.R.layout.simple_spinner_item, mAlbums);
		mAlbumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAlbumSpinner.setAdapter(mAlbumAdapter);

		mAccountsAdapter = new ArrayAdapter<Account>(this, android.R.layout.simple_spinner_item, mAccounts);
		mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountsSpinner.setAdapter(mAccountsAdapter);

		mPlacesIcon = (NetworkedCacheableImageView) findViewById(R.id.iv_place_photo);
		mPlacesButton = (TextView) findViewById(R.id.btn_place);
		mPlacesLayout = findViewById(R.id.ll_place);
		mPlacesLayout.setOnClickListener(this);
		
		mAccountHelpBtn = (ImageButton) findViewById(R.id.btn_account_help);
		mAccountHelpBtn.setOnClickListener(this);

		mAlbumSpinnerLayout = findViewById(R.id.ll_album_spinner);
		mAlbumTitleTv = findViewById(R.id.tv_album_title);

		bindService(new Intent(this, PhotoUploadService.class), this, Context.BIND_AUTO_CREATE);

		PhotupApplication app = PhotupApplication.getApplication(this);
		app.getAlbums(this, false);
		app.getAccounts(this, false);
	}

	private void upload() {
		UploadQuality quality = UploadQuality.mapFromButtonId(mQualityRadioGroup.getCheckedRadioButtonId());
		Account account = (Account) mAccountsSpinner.getSelectedItem();
		Album album = null;
		if (mAccountsSpinner.getSelectedItemPosition() == 0) {
			album = (Album) mAlbumSpinner.getSelectedItem();
		}

		if (null != album || mAccountsSpinner.getSelectedItemPosition() > 0) {
			PhotupApplication.getApplication(this).getPhotoUploadController()
					.moveSelectedPhotosToUploads(account, album, quality, mPlace);

			mBinder.getService().uploadAll();
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_FACEBOOK_LOGIN:
				if (resultCode == RESULT_OK) {
					PhotupApplication.getApplication(this).getAccounts(this, true);
				}
				return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.menu_photo_upload, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				return true;
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
		if (v == mNewAlbumButton) {
			NewAlbumFragment fragment = new NewAlbumFragment();
			fragment.show(getSupportFragmentManager(), "new_album");
		} else if (v == mPlacesLayout) {
			PlacesListFragment fragment = new PlacesListFragment();
			fragment.setOnPlacePickedListener(this);
			fragment.show(getSupportFragmentManager(), "places");
		} else if (v == mAccountHelpBtn) {
			showMissingPagesDialog();
		}
	}

	public void onAlbumCreated() {
		PhotupApplication.getApplication(this).getAlbums(this, true);
	}

	public void onFacebookError(FacebookError e) {
		// NO-OP
	}

	public void onPlacePicked(Place place) {
		mPlace = place;
		mPlacesButton.setText(place.getName());
		mPlacesIcon.loadImage(PhotupApplication.getApplication(getApplicationContext()).getImageCache(), place.getAvatarUrl());
	}

	public void onAccountsLoaded(List<Account> accounts) {
		mAccounts.clear();
		mAccounts.addAll(accounts);
		mAccountsAdapter.notifyDataSetChanged();
		mAccountsSpinner.setEnabled(true);
	}

	public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
		if (position == 0) {
			mAlbumTitleTv.setVisibility(View.VISIBLE);
			mAlbumSpinnerLayout.setVisibility(View.VISIBLE);
		} else {
			mAlbumTitleTv.setVisibility(View.GONE);
			mAlbumSpinnerLayout.setVisibility(View.GONE);
		}
	}

	public void onNothingSelected(AdapterView<?> spinner) {
		// NO-OP
	}

	private void showMissingPagesDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(R.string.dialog_missing_pages_title);
		builder.setMessage(R.string.dialog_missing_pages_text);

		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case AlertDialog.BUTTON_POSITIVE:
						startActivityForResult(new Intent(Constants.INTENT_NEW_PERMISSIONS), REQUEST_FACEBOOK_LOGIN);
						break;
				}

				dialog.dismiss();
			}
		};

		builder.setPositiveButton(android.R.string.ok, listener);
		builder.setNegativeButton(android.R.string.cancel, listener);
		builder.show();
	}

}
