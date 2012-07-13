package uk.co.senab.photup.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.R;
import uk.co.senab.photup.listeners.OnFriendPickedListener;
import uk.co.senab.photup.model.FbUser;
import uk.co.senab.photup.tasks.FriendsAsyncTask.FriendsResultListener;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.facebook.android.FacebookError;

public class FriendsListFragment extends SherlockDialogFragment implements FriendsResultListener, OnItemClickListener,
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

		mAdapter = new ArrayAdapter<FbUser>(getActivity(), android.R.layout.simple_list_item_1, mDisplayedFriends);
		
		PhotupApplication.getApplication(getActivity()).getFriends(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_friends, container, false);

		mListView = (ListView) view.findViewById(R.id.lv_friends);
		mListView.setOnItemClickListener(this);
		mListView.setAdapter(mAdapter);

		mFilterEditText = (EditText) view.findViewById(R.id.et_friends_filter);
		mFilterEditText.addTextChangedListener(this);

		return view;
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
