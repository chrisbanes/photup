package uk.co.senab.photup.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.tasks.AlbumsAsyncTask;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;
import uk.co.senab.photup.tasks.EventsAsyncTask;
import uk.co.senab.photup.tasks.EventsAsyncTask.EventsResultListener;
import uk.co.senab.photup.tasks.GroupsAsyncTask;
import uk.co.senab.photup.tasks.GroupsAsyncTask.GroupsResultListener;
import android.content.Context;
import android.text.TextUtils;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

public class Account extends AbstractFacebookObject implements AlbumsResultListener, EventsResultListener,
		GroupsResultListener {

	private final String mAccessToken;
	private final long mAccessExpires;
	private final boolean mIsMainAccount;

	private AlbumsResultListener mAlbumsListener;
	private GroupsResultListener mGroupsListener;
	private EventsResultListener mEventsListener;

	private ArrayList<Album> mAlbums;
	private ArrayList<Event> mEvents;
	private ArrayList<Group> mGroups;

	private Account(String id, String name, String accessToken, long accessExpires) {
		super(id, name, null);
		mAccessToken = accessToken;
		mAccessExpires = accessExpires;
		mIsMainAccount = true;
	}

	public Account(JSONObject object) throws JSONException {
		super(object, null);
		mAccessToken = object.optString("access_token", null);
		mAccessExpires = 0;
		mIsMainAccount = false;
	}

	public String getAccessToken() {
		return mAccessToken;
	}

	public boolean isMainAccount() {
		return mIsMainAccount;
	}

	public void getAlbums(Context context, AlbumsResultListener listener, boolean forceRefresh) {
		if (null == mAlbums) {
			mAlbums = new ArrayList<Album>();
		}

		if (forceRefresh || mAlbums.isEmpty()) {
			mAlbumsListener = listener;
			new AlbumsAsyncTask(context, this, this).execute();
		} else {
			listener.onAlbumsLoaded(this, mAlbums);
		}
	}

	public void getGroups(Context context, GroupsResultListener listener, boolean forceRefresh) {
		if (null == mGroups) {
			mGroups = new ArrayList<Group>();
		}

		if (forceRefresh || mGroups.isEmpty()) {
			mGroupsListener = listener;
			new GroupsAsyncTask(context, this, this).execute();
		} else {
			listener.onGroupsLoaded(this, mGroups);
		}
	}

	public void getEvents(Context context, EventsResultListener listener, boolean forceRefresh) {
		if (null == mEvents) {
			mEvents = new ArrayList<Event>();
		}

		if (forceRefresh || mEvents.isEmpty()) {
			mEventsListener = listener;
			new EventsAsyncTask(context, this, this).execute();
		} else {
			listener.onEventsLoaded(this, mEvents);
		}
	}

	public boolean hasAccessToken() {
		return !TextUtils.isEmpty(mAccessToken);
	}

	public static Account getMeFromSession(Session session) {
		if (null != session) {
			final Facebook fb = session.getFb();
			return new Account(session.getUid(), session.getName(), fb.getAccessToken(), fb.getAccessExpires());
		}
		return null;
	}

	public static Account getAccountFromSession(Context context) {
		return getMeFromSession(Session.restore(context));
	}

	public Facebook getFacebook() {
		Facebook facebook = new Facebook(Constants.FACEBOOK_APP_ID);
		facebook.setAccessToken(mAccessToken);
		facebook.setAccessExpires(mAccessExpires);
		return facebook;
	}

	public void onFacebookError(FacebookError e) {
		// NO-OP
	}

	public void onAlbumsLoaded(Account account, List<Album> albums) {
		mAlbums.clear();

		if (null != albums) {
			mAlbums.addAll(albums);

			if (null != mAlbumsListener && mAlbumsListener != this) {
				mAlbumsListener.onAlbumsLoaded(account, mAlbums);
				mAlbumsListener = null;
			}
		}
	}

	public void onGroupsLoaded(Account account, List<Group> groups) {
		mGroups.clear();

		if (null != groups) {
			mGroups.addAll(groups);
			if (null != mGroupsListener && mGroupsListener != this) {
				mGroupsListener.onGroupsLoaded(account, mGroups);
				mGroupsListener = null;
			}
		}
	}

	public void onEventsLoaded(Account account, List<Event> events) {
		mEvents.clear();

		if (null != events) {
			mEvents.addAll(events);
			if (null != mEventsListener && mEventsListener != this) {
				mEventsListener.onEventsLoaded(account, mEvents);
				mEventsListener = null;
			}
		}
	}

	public void preloadAll(Context context) {
		getAlbums(context, null, false);
		getGroups(context, null, false);
		getEvents(context, null, false);
	}

}
