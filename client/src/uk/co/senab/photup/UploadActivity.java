package uk.co.senab.photup;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photup.fragments.NewAlbumFragment;
import uk.co.senab.photup.fragments.NewAlbumFragment.OnAlbumCreatedListener;
import uk.co.senab.photup.fragments.PlacesListFragment;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.model.AbstractFacebookObject;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.Event;
import uk.co.senab.photup.model.Group;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.service.PhotoUploadService;
import uk.co.senab.photup.service.PhotoUploadService.ServiceBinder;
import uk.co.senab.photup.tasks.AccountsAsyncTask.AccountsResultListener;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;
import uk.co.senab.photup.tasks.EventsAsyncTask.EventsResultListener;
import uk.co.senab.photup.tasks.GroupsAsyncTask.GroupsResultListener;
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
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.android.FacebookError;
import com.lightbox.android.photoprocessing.R;

public class UploadActivity extends SherlockFragmentActivity implements ServiceConnection, AlbumsResultListener,
		AccountsResultListener, GroupsResultListener, EventsResultListener, OnClickListener, OnAlbumCreatedListener,
		OnPlacePickedListener, OnItemSelectedListener, OnCheckedChangeListener {

	static final int DEFAULT_UPLOAD_TARGET_ID = R.id.rb_target_album;
	static final int REQUEST_FACEBOOK_LOGIN = 99;

	private final ArrayList<AbstractFacebookObject> mFacebookObjects = new ArrayList<AbstractFacebookObject>();
	private final ArrayList<Account> mAccounts = new ArrayList<Account>();

	private ServiceBinder<PhotoUploadService> mBinder;

	private RadioGroup mQualityRadioGroup;
	private Spinner mTargetSpinner, mAccountsSpinner;
	private ImageButton mNewAlbumButton;
	private TextView mPlacesButton;
	private NetworkedCacheableImageView mPlacesIcon;

	private View mPlacesLayout, mTargetLayout;

	private RadioGroup mTargetRadioGroup;

	private ImageButton mAccountHelpBtn, mTargetHelpBtn, mPlaceRemoveBtn;

	private Place mPlace;

	private ArrayAdapter<AbstractFacebookObject> mTargetAdapter;
	private ArrayAdapter<Account> mAccountsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_upload);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mQualityRadioGroup = (RadioGroup) findViewById(R.id.rg_upload_quality);
		mTargetRadioGroup = (RadioGroup) findViewById(R.id.rg_upload_target);
		mTargetRadioGroup.setOnCheckedChangeListener(this);

		mTargetLayout = findViewById(R.id.ll_upload_target);
		mTargetSpinner = (Spinner) findViewById(R.id.sp_upload_target);

		mAccountsSpinner = (Spinner) findViewById(R.id.sp_upload_account);
		mAccountsSpinner.setOnItemSelectedListener(this);
		mAccountsSpinner.setEnabled(false);

		mNewAlbumButton = (ImageButton) findViewById(R.id.btn_new_album);
		mNewAlbumButton.setOnClickListener(this);

		mTargetAdapter = new ArrayAdapter<AbstractFacebookObject>(this, android.R.layout.simple_spinner_item,
				mFacebookObjects);
		mTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mTargetSpinner.setAdapter(mTargetAdapter);

		mAccountsAdapter = new ArrayAdapter<Account>(this, android.R.layout.simple_spinner_item, mAccounts);
		mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAccountsSpinner.setAdapter(mAccountsAdapter);

		mPlacesIcon = (NetworkedCacheableImageView) findViewById(R.id.iv_place_photo);
		mPlacesButton = (TextView) findViewById(R.id.btn_place);
		mPlacesLayout = findViewById(R.id.ll_place);
		mPlacesLayout.setOnClickListener(this);

		mPlaceRemoveBtn = (ImageButton) findViewById(R.id.btn_place_remove);
		mPlaceRemoveBtn.setOnClickListener(this);

		mAccountHelpBtn = (ImageButton) findViewById(R.id.btn_account_help);
		mAccountHelpBtn.setOnClickListener(this);

		mTargetHelpBtn = (ImageButton) findViewById(R.id.btn_target_help);
		mTargetHelpBtn.setOnClickListener(this);

		bindService(new Intent(this, PhotoUploadService.class), this, Context.BIND_AUTO_CREATE);

		PhotupApplication app = PhotupApplication.getApplication(this);
		app.getAccounts(this, false);
	}

	private void upload(final boolean force) {
		final PhotoUploadController controller = PhotoUploadController.getFromContext(this);

		// If we're not being forced, do checks and show prompts
		if (!force) {
			// Show Place Overwrite dialog
			if (null != mPlace && controller.hasSelectionsWithPlace()) {
				showPlaceOverwriteDialog();
				return;
			}
		}

		UploadQuality quality = UploadQuality.mapFromButtonId(mQualityRadioGroup.getCheckedRadioButtonId());
		Account account = (Account) mAccountsSpinner.getSelectedItem();

		boolean validTarget = false;
		String targetId = null;

		switch (mTargetRadioGroup.getCheckedRadioButtonId()) {
			case R.id.rb_target_wall:
				validTarget = true;
				break;

			case R.id.rb_target_album:
			case R.id.rb_target_group:
			case R.id.rb_target_event:
			default:
				AbstractFacebookObject object = (AbstractFacebookObject) mTargetSpinner.getSelectedItem();
				if (null != object) {
					targetId = object.getId();
					validTarget = !TextUtils.isEmpty(targetId);
				}
				break;
		}

		if (validTarget) {
			controller.moveSelectedPhotosToUploads(account, targetId, quality, mPlace);
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
				upload(false);
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
		mFacebookObjects.clear();
		mFacebookObjects.addAll(albums);
		mTargetAdapter.notifyDataSetChanged();
		mTargetLayout.setVisibility(View.VISIBLE);
	}

	public void onClick(View v) {
		if (v == mNewAlbumButton) {
			NewAlbumFragment fragment = new NewAlbumFragment((Account) mAccountsSpinner.getSelectedItem());
			fragment.show(getSupportFragmentManager(), "new_album");
		} else if (v == mPlacesLayout) {
			startPlaceFragment();
		} else if (v == mAccountHelpBtn) {
			showMissingItemsDialog(true);
		} else if (v == mTargetHelpBtn) {
			showMissingItemsDialog(false);
		} else if (v == mPlaceRemoveBtn) {
			onPlacePicked(null);
		}
	}

	public void onAlbumCreated() {
		Account account = (Account) mAccountsSpinner.getSelectedItem();
		account.getAlbums(this, true);
	}

	public void onFacebookError(FacebookError e) {
		// NO-OP
	}

	public void onPlacePicked(Place place) {
		mPlace = place;
		if (null != place) {
			mPlacesButton.setText(place.getName());
			mPlacesIcon.loadImage(PhotupApplication.getApplication(getApplicationContext()).getImageCache(),
					place.getAvatarUrl());
		} else {
			mPlacesButton.setText(R.string.place);
			mPlacesIcon.setImageResource(R.drawable.ic_action_place);
		}
	}

	public void onAccountsLoaded(List<Account> accounts) {
		mAccounts.clear();
		mAccounts.addAll(accounts);
		mAccountsAdapter.notifyDataSetChanged();
		mAccountsSpinner.setEnabled(true);
	}

	public void onEventsLoaded(List<Event> events) {
		mFacebookObjects.clear();
		mFacebookObjects.addAll(events);
		mTargetAdapter.notifyDataSetChanged();
		mTargetLayout.setVisibility(View.VISIBLE);
	}

	public void onGroupsLoaded(List<Group> groups) {
		mFacebookObjects.clear();
		mFacebookObjects.addAll(groups);
		mTargetAdapter.notifyDataSetChanged();
		mTargetLayout.setVisibility(View.VISIBLE);
	}

	public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
		final Account account = (Account) mAccountsSpinner.getSelectedItem();

		View eventRb = findViewById(R.id.rb_target_event), groupRb = findViewById(R.id.rb_target_group);
		final int visibility = account.isMainAccount() ? View.VISIBLE : View.GONE;
		eventRb.setVisibility(visibility);
		groupRb.setVisibility(visibility);

		if (mTargetRadioGroup.getCheckedRadioButtonId() == DEFAULT_UPLOAD_TARGET_ID) {
			onCheckedChanged(mTargetRadioGroup, DEFAULT_UPLOAD_TARGET_ID);
		} else {
			mTargetRadioGroup.check(DEFAULT_UPLOAD_TARGET_ID);
		}
	}

	public void onNothingSelected(AdapterView<?> spinner) {
		// NO-OP
	}

	private void startPlaceFragment() {
		PlacesListFragment fragment = new PlacesListFragment();
		fragment.setOnPlacePickedListener(this);
		fragment.show(getSupportFragmentManager(), "places");
	}

	private void showMissingItemsDialog(final boolean pages) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(pages ? R.string.dialog_missing_pages_title : R.string.dialog_missing_items_title);
		builder.setMessage(R.string.dialog_missing_items_text);

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

	private void showPlaceOverwriteDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle(R.string.dialog_place_overwrite_title);
		builder.setMessage(R.string.dialog_place_overwrite_text);

		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case AlertDialog.BUTTON_POSITIVE:
						upload(true);
						break;
				}
				dialog.dismiss();
			}
		};

		builder.setPositiveButton(android.R.string.ok, listener);
		builder.setNegativeButton(android.R.string.cancel, listener);
		builder.show();
	}

	public void onCheckedChanged(RadioGroup group, final int checkedId) {
		final Account account = (Account) mAccountsSpinner.getSelectedItem();
		mTargetLayout.setVisibility(View.GONE);

		if (null != account) {
			switch (checkedId) {
				case R.id.rb_target_album:
					account.getAlbums(this, false);
					mTargetHelpBtn.setVisibility(View.GONE);
					mNewAlbumButton.setVisibility(View.VISIBLE);
					break;
				case R.id.rb_target_event:
					account.getEvents(this, false);
					mTargetHelpBtn.setVisibility(View.VISIBLE);
					mNewAlbumButton.setVisibility(View.GONE);
					break;
				case R.id.rb_target_group:
					account.getGroups(this, false);
					mTargetHelpBtn.setVisibility(View.VISIBLE);
					mNewAlbumButton.setVisibility(View.GONE);
					break;
				case R.id.rb_target_wall:
					break;
			}
		}
	}

}
