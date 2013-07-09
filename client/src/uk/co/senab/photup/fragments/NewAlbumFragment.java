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

import com.actionbarsherlock.app.SherlockDialogFragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import uk.co.senab.photup.R;
import uk.co.senab.photup.model.Account;
import uk.co.senab.photup.tasks.NewAlbumAsyncTask;
import uk.co.senab.photup.tasks.NewAlbumAsyncTask.NewAlbumResultListener;

@SuppressLint("ValidFragment")
public class NewAlbumFragment extends SherlockDialogFragment
        implements View.OnClickListener, NewAlbumResultListener {

    public static interface OnAlbumCreatedListener {

        void onAlbumCreated();
    }

    private EditText mAlbumNameEditText, mAlbumDescEditText;
    private Spinner mPrivacySpinner;
    private ImageButton mSendButton;
    private ProgressBar mLoadingProgressBar;

    private OnAlbumCreatedListener mAlbumCreated;
    private Account mAccount;

    private String[] mPrivacyValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrivacyValues = getResources().getStringArray(R.array.privacy_settings_values);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
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
        if (null != mAccount) {
            String albumName = mAlbumNameEditText.getText().toString();
            // TODO Check for empty String

            String privacyValue = mPrivacyValues[mPrivacySpinner.getSelectedItemPosition()];
            String description = mAlbumDescEditText.getText().toString();

            v.setVisibility(View.GONE);
            mLoadingProgressBar.setVisibility(View.VISIBLE);

            new NewAlbumAsyncTask(mAccount, this).execute(albumName, description, privacyValue);
        }
    }

    public void onNewAlbumCreated(String albumId) {
        if (null != mAlbumCreated) {
            mAlbumCreated.onAlbumCreated();

            try {
                dismiss();
            } catch (Exception e) {
                // WTF moment. Shown up in logs.
            }
        }
    }

    public void setAccount(Account account) {
        mAccount = account;
    }

    public void setOnAlbumCreatedListener(OnAlbumCreatedListener listener) {
        mAlbumCreated = listener;
    }

}
