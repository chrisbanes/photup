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
import com.facebook.android.FacebookError;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.listeners.OnFriendPickedListener;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.tasks.FriendsAsyncTask.FriendsResultListener;

public class FriendsListFragment extends SherlockDialogFragment
        implements FriendsResultListener, OnItemClickListener,
        TextWatcher {

    private final ArrayList<FbUser> mFriends = new ArrayList<FbUser>();
    private final ArrayList<FbUser> mDisplayedFriends = new ArrayList<FbUser>();

    private Set<FbUser> mExcludedFriends;

    private ListView mListView;
    private EditText mFilterEditText;

    private ArrayAdapter<FbUser> mAdapter;

    private OnFriendPickedListener mPickedFriendListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(STYLE_NO_TITLE, 0);

        mAdapter = new ArrayAdapter<FbUser>(getActivity(), android.R.layout.simple_list_item_1,
                mDisplayedFriends);

        PhotupApplication.getApplication(getActivity()).getFriends(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        mListView = (ListView) view.findViewById(R.id.lv_friends);
        mListView.setOnItemClickListener(this);
        mListView.setAdapter(mAdapter);

        mFilterEditText = (EditText) view.findViewById(R.id.et_friends_filter);
        mFilterEditText.addTextChangedListener(this);

        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // Set Soft Input mode so it's always visible
        dialog.getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Clear filter text if needed
        if (!TextUtils.isEmpty(mFilterEditText.getText())) {
            mFilterEditText.setText("");
        }
    }

    public void onFriendsLoaded(List<FbUser> friends) {
        mFriends.clear();
        mFriends.addAll(friends);
        updateFriends();
    }

    public void setOnFriendPickedListener(OnFriendPickedListener listener) {
        mPickedFriendListener = listener;
    }

    public void setExcludedFriends(Set<FbUser> excludeSet) {
        mExcludedFriends = excludeSet;
        updateFriends();
    }

    private void updateFriends() {
        mDisplayedFriends.clear();

        if (null != mExcludedFriends && !mExcludedFriends.isEmpty()) {
            for (FbUser friend : mFriends) {
                if (!mExcludedFriends.contains(friend)) {
                    mDisplayedFriends.add(friend);
                }
            }
        } else {
            mDisplayedFriends.addAll(mFriends);
        }

        if (null != mAdapter) {
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FbUser friend = (FbUser) parent.getItemAtPosition(position);

        if (null != mPickedFriendListener) {
            mPickedFriendListener.onFriendPicked(friend);
        }

        dismiss();
    }

    public void afterTextChanged(Editable s) {
        // NO-OP
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // NO-OP
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mAdapter.getFilter().filter(s);
    }

    public void onFacebookError(FacebookError e) {
        // NO-OP
    }

}
