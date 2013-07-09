/*
 * Copyright 2013 Chris Banes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.senab.photup.fragments;

import com.facebook.android.FacebookError;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotoUploadController;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.events.UploadsStartEvent;
import uk.co.senab.photup.fragments.NewAlbumFragment.OnAlbumCreatedListener;
import uk.co.senab.photup.listeners.OnPlacePickedListener;
import uk.co.senab.photup.model.AbstractFacebookObject;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.Event;
import uk.co.senab.photup.model.Group;
import uk.co.senab.photup.model.Place;
import uk.co.senab.photup.model.UploadQuality;
import uk.co.senab.photup.tasks.AccountsAsyncTask.AccountsResultListener;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;
import uk.co.senab.photup.tasks.EventsAsyncTask.EventsResultListener;
import uk.co.senab.photup.tasks.GroupsAsyncTask.GroupsResultListener;
import uk.co.senab.photup.util.Utils;
import uk.co.senab.photup.views.NetworkedCacheableImageView;

public class UploadFragment extends PhotupDialogFragment
        implements AlbumsResultListener, AccountsResultListener,
        GroupsResultListener, EventsResultListener, OnClickListener, OnAlbumCreatedListener,
        OnPlacePickedListener,
        OnItemSelectedListener, OnCheckedChangeListener {

    static final int DEFAULT_UPLOAD_TARGET_ID = R.id.rb_target_album;
    static final int REQUEST_FACEBOOK_LOGIN = 99;

    private final ArrayList<AbstractFacebookObject> mFacebookObjects
            = new ArrayList<AbstractFacebookObject>();
    private final ArrayList<Account> mAccounts = new ArrayList<Account>();

    private RadioGroup mQualityRadioGroup;
    private Spinner mTargetSpinner, mAccountsSpinner;
    private ImageButton mNewAlbumButton;
    private TextView mPlacesButton;
    private NetworkedCacheableImageView mPlacesIcon;

    private View mPlacesLayout, mTargetLayout;

    private RadioGroup mTargetRadioGroup;

    private ImageButton mAccountHelpBtn, mTargetHelpBtn, mPlaceRemoveBtn, mUploadBtn;

    private Place mPlace;

    private ArrayAdapter<AbstractFacebookObject> mTargetAdapter;
    private ArrayAdapter<Account> mAccountsAdapter;

    public Account getSelectedAccount() {
        return (Account) mAccountsSpinner.getSelectedItem();
    }

    public void onAccountsLoaded(List<Account> accounts) {
        mAccounts.clear();
        mAccounts.addAll(accounts);
        mAccountsAdapter.notifyDataSetChanged();
        mAccountsSpinner.setEnabled(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PhotupApplication app = PhotupApplication.getApplication(getActivity());
        app.getAccounts(this, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_FACEBOOK_LOGIN:
                if (resultCode == Activity.RESULT_OK) {
                    PhotupApplication.getApplication(getActivity()).getAccounts(this, true);
                }
                return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onAlbumCreated() {
        Account account = (Account) mAccountsSpinner.getSelectedItem();
        account.getAlbums(PhotupApplication.getApplication(getActivity()), this, true);
    }

    public void onAlbumsLoaded(Account account, List<Album> albums) {
        updateTargetList(R.id.rb_target_album, account, albums);
    }

    public void onCheckedChanged(RadioGroup group, final int checkedId) {
        final Account account = getSelectedAccount();
        mTargetLayout.setVisibility(View.GONE);

        if (null != account) {
            final Context appContext = getActivity().getApplicationContext();

            switch (checkedId) {
                case R.id.rb_target_album:
                    account.getAlbums(appContext, this, false);
                    mTargetHelpBtn.setVisibility(View.GONE);
                    mNewAlbumButton.setVisibility(View.VISIBLE);
                    break;
                case R.id.rb_target_event:
                    account.getEvents(appContext, this, false);
                    mTargetHelpBtn.setVisibility(View.VISIBLE);
                    mNewAlbumButton.setVisibility(View.GONE);
                    break;
                case R.id.rb_target_group:
                    account.getGroups(appContext, this, false);
                    mTargetHelpBtn.setVisibility(View.VISIBLE);
                    mNewAlbumButton.setVisibility(View.GONE);
                    break;
                case R.id.rb_target_wall:
                    break;
            }
        }
    }

    public void onClick(View v) {
        if (v == mNewAlbumButton) {
            startNewAlbumFragment();
        } else if (v == mPlacesLayout) {
            startPlaceFragment();
        } else if (v == mAccountHelpBtn) {
            showMissingItemsDialog(true);
        } else if (v == mTargetHelpBtn) {
            showMissingItemsDialog(false);
        } else if (v == mPlaceRemoveBtn) {
            onPlacePicked(null);
        } else if (v == mUploadBtn) {
            upload(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_upload, container, false);

        mQualityRadioGroup = (RadioGroup) view.findViewById(R.id.rg_upload_quality);
        mTargetRadioGroup = (RadioGroup) view.findViewById(R.id.rg_upload_target);
        mTargetRadioGroup.setOnCheckedChangeListener(this);

        mTargetLayout = view.findViewById(R.id.ll_upload_target);
        mTargetSpinner = (Spinner) view.findViewById(R.id.sp_upload_target);

        mAccountsSpinner = (Spinner) view.findViewById(R.id.sp_upload_account);
        mAccountsSpinner.setOnItemSelectedListener(this);
        mAccountsSpinner.setEnabled(false);

        mNewAlbumButton = (ImageButton) view.findViewById(R.id.btn_new_album);
        mNewAlbumButton.setOnClickListener(this);

        mTargetAdapter = new ArrayAdapter<AbstractFacebookObject>(getActivity(),
                Utils.getSpinnerItemResId(),
                mFacebookObjects);
        mTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTargetSpinner.setAdapter(mTargetAdapter);

        mAccountsAdapter = new ArrayAdapter<Account>(getActivity(), Utils.getSpinnerItemResId(),
                mAccounts);
        mAccountsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountsSpinner.setAdapter(mAccountsAdapter);

        mPlacesIcon = (NetworkedCacheableImageView) view.findViewById(R.id.iv_place_photo);
        mPlacesButton = (TextView) view.findViewById(R.id.btn_place);
        mPlacesLayout = view.findViewById(R.id.ll_place);
        mPlacesLayout.setOnClickListener(this);

        mUploadBtn = (ImageButton) view.findViewById(R.id.btn_upload);
        mUploadBtn.setOnClickListener(this);

        mPlaceRemoveBtn = (ImageButton) view.findViewById(R.id.btn_place_remove);
        mPlaceRemoveBtn.setOnClickListener(this);

        mAccountHelpBtn = (ImageButton) view.findViewById(R.id.btn_account_help);
        mAccountHelpBtn.setOnClickListener(this);

        mTargetHelpBtn = (ImageButton) view.findViewById(R.id.btn_target_help);
        mTargetHelpBtn.setOnClickListener(this);

        return view;
    }

    public void onEventsLoaded(Account account, List<Event> events) {
        updateTargetList(R.id.rb_target_event, account, events);
    }

    public void onFacebookError(FacebookError e) {
        // NO-OP
    }

    public void onGroupsLoaded(Account account, List<Group> groups) {
        updateTargetList(R.id.rb_target_group, account, groups);
    }

    public void onItemSelected(AdapterView<?> spinner, View view, int position, long id) {
        final View fragView = getView();

        if (null != fragView) {
            final Account account = (Account) mAccountsSpinner.getSelectedItem();

            if (null != account) {
                final int visibility = account.isMainAccount() ? View.VISIBLE : View.GONE;

                View eventRb = fragView.findViewById(R.id.rb_target_event), groupRb = fragView
                        .findViewById(R.id.rb_target_group);
                eventRb.setVisibility(visibility);
                groupRb.setVisibility(visibility);

                if (mTargetRadioGroup.getCheckedRadioButtonId() == DEFAULT_UPLOAD_TARGET_ID) {
                    onCheckedChanged(mTargetRadioGroup, DEFAULT_UPLOAD_TARGET_ID);
                } else {
                    mTargetRadioGroup.check(DEFAULT_UPLOAD_TARGET_ID);
                }
            }
        }
    }

    public void onNothingSelected(AdapterView<?> spinner) {
        // NO-OP
    }

    public void onPlacePicked(Place place) {
        mPlace = place;
        if (null != place) {
            mPlacesButton.setText(place.getName());
            mPlacesIcon
                    .loadImage(PhotupApplication.getApplication(getActivity()).getImageCache(),
                            place.getAvatarUrl());
            mPlaceRemoveBtn.setVisibility(View.VISIBLE);
        } else {
            mPlacesButton.setText(R.string.place);
            mPlacesIcon.setImageResource(R.drawable.ic_action_place);
            mPlaceRemoveBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkConnectionSpeed();
    }

    private void checkConnectionSpeed() {
        ConnectivityManager mgr = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
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

    private void showMissingItemsDialog(final boolean pages) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(
                pages ? R.string.dialog_missing_pages_title : R.string.dialog_missing_items_title);
        builder.setMessage(R.string.dialog_missing_items_text);

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case AlertDialog.BUTTON_POSITIVE:
                        startActivityForResult(new Intent(Constants.INTENT_NEW_PERMISSIONS),
                                REQUEST_FACEBOOK_LOGIN);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    private void startNewAlbumFragment() {
        Account account = getSelectedAccount();
        if (null != account) {
            NewAlbumFragment fragment = new NewAlbumFragment();
            fragment.setOnAlbumCreatedListener(this);
            fragment.setAccount(account);
            fragment.show(getActivity().getSupportFragmentManager(), "new_album");
        }
    }

    private void startPlaceFragment() {
        PlacesListFragment fragment = new PlacesListFragment();
        fragment.setOnPlacePickedListener(this);
        fragment.show(getActivity().getSupportFragmentManager(), "places");
    }

    private <T extends AbstractFacebookObject> void updateTargetList(final int targetId,
            final Account targetAccount,
            final List<T> objects) {
        if (targetId == mTargetRadioGroup.getCheckedRadioButtonId()
                && targetAccount == getSelectedAccount()) {
            mFacebookObjects.clear();
            mFacebookObjects.addAll(objects);
            mTargetAdapter.notifyDataSetChanged();
            // Disable Spinner if it's empty
            mTargetSpinner.setEnabled(!mTargetAdapter.isEmpty());
            mTargetLayout.setVisibility(View.VISIBLE);
        }
    }

    private void upload(final boolean force) {
        final PhotoUploadController controller = PhotoUploadController
                .getFromContext(getActivity());

        // If we're not being forced, do checks and show prompts
        if (!force) {
            // Show Place Overwrite dialog
            if (null != mPlace && controller.hasSelectionsWithPlace()) {
                showPlaceOverwriteDialog();
                return;
            }
        }

        UploadQuality quality = UploadQuality
                .mapFromButtonId(mQualityRadioGroup.getCheckedRadioButtonId());
        Account account = (Account) mAccountsSpinner.getSelectedItem();

        boolean validTarget = false;
        String targetId = null;

        switch (mTargetRadioGroup.getCheckedRadioButtonId()) {
            case R.id.rb_target_wall:
                validTarget = (null != account);
                break;

            case R.id.rb_target_album:
            case R.id.rb_target_group:
            case R.id.rb_target_event:
            default:
                AbstractFacebookObject object = (AbstractFacebookObject) mTargetSpinner
                        .getSelectedItem();
                if (null != object) {
                    targetId = object.getId();
                    validTarget = !TextUtils.isEmpty(targetId);
                }
                break;
        }

        if (validTarget) {
            controller.addUploadsFromSelected(account, targetId, quality, mPlace);
            getActivity().startService(Utils.getUploadAllIntent(getActivity()));
            dismiss();
            EventBus.getDefault().post(new UploadsStartEvent());
        } else {
            Toast.makeText(getActivity(), getString(R.string.error_select_album),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
