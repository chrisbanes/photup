package uk.co.senab.photup.fragments;

import uk.co.senab.photup.R;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.tasks.NewAlbumAsyncTask;
import uk.co.senab.photup.tasks.NewAlbumAsyncTask.NewAlbumResultListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;

@SuppressLint("ValidFragment")
public class NewAlbumFragment extends SherlockDialogFragment implements View.OnClickListener, NewAlbumResultListener {

	public static interface AccountProviderAccessor {
		Account getSelectedAccount();
	}

	public static interface OnAlbumCreatedListener {
		void onAlbumCreated();
	}

	private EditText mAlbumNameEditText, mAlbumDescEditText;
	private Spinner mPrivacySpinner;
	private ImageButton mSendButton;
	private ProgressBar mLoadingProgressBar;

	private OnAlbumCreatedListener mAlbumCreated;

	private String[] mPrivacyValues;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mAlbumCreated = (OnAlbumCreatedListener) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrivacyValues = getResources().getStringArray(R.array.privacy_settings_values);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_new_album, container, false);

		mAlbumNameEditText = (EditText) view.findViewById(R.id.et_album_name);
		mAlbumDescEditText = (EditText) view.findViewById(R.id.et_album_description);
		mPrivacySpinner = (Spinner) view.findViewById(R.id.sp_privacy);
		mLoadingProgressBar = (ProgressBar) view.findViewById(R.id.pb_loading);

		mSendButton = (ImageButton) view.findViewById(R.id.ib_create_album);
		mSendButton.setOnClickListener(this);

		return view;
	}

	@Override
	public void onActivityCreated(Bundle b) {
		super.onActivityCreated(b);
		getDialog().setTitle(R.string.upload_new_album);
	}

	public void onClick(View v) {
		String albumName = mAlbumNameEditText.getText().toString();
		// TODO Check for empty String

		String privacyValue = mPrivacyValues[mPrivacySpinner.getSelectedItemPosition()];
		String description = mAlbumDescEditText.getText().toString();

		v.setVisibility(View.GONE);
		mLoadingProgressBar.setVisibility(View.VISIBLE);

		final Account account = ((AccountProviderAccessor) getActivity()).getSelectedAccount();

		new NewAlbumAsyncTask(account, this).execute(albumName, description, privacyValue);
	}

	public void onNewAlbumCreated(String albumId) {
		if (null != mAlbumCreated) {
			mAlbumCreated.onAlbumCreated();
			dismiss();
		}
	}

}
