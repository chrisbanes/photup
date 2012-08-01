package uk.co.senab.photup.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.facebook.Session;
import uk.co.senab.photup.tasks.AlbumsAsyncTask;
import uk.co.senab.photup.tasks.AlbumsAsyncTask.AlbumsResultListener;
import android.content.Context;
import android.text.TextUtils;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;

public class Account extends AbstractFacebookObject implements AlbumsResultListener {

	private final String mAccessToken;
	private final long mAccessExpires;
	private final boolean mIsMainAccount;

	private AlbumsResultListener mAlbumsListener;
	private ArrayList<Album> mAlbums;

	public Account(String id, String name, String accessToken, long accessExpires) {
		super(id, name);
		mAccessToken = accessToken;
		mAccessExpires = accessExpires;
		mIsMainAccount = true;
	}

	public Account(JSONObject object) throws JSONException {
		super(object);
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

	public void getAlbums(AlbumsResultListener listener, boolean forceRefresh) {
		if (null == mAlbums) {
			mAlbums = new ArrayList<Album>();
		}
		
		if (forceRefresh || mAlbums.isEmpty()) {
			mAlbumsListener = listener;
			new AlbumsAsyncTask(this, this).execute();
		} else {
			listener.onAlbumsLoaded(mAlbums);
		}
	}

	public boolean hasAccessToken() {
		return !TextUtils.isEmpty(mAccessToken);
	}

	public static Account getMeFromSession(Session session) {
		final Facebook fb = session.getFb();
		return new Account(session.getUid(), session.getName(), fb.getAccessToken(), fb.getAccessExpires());
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

	public void onAlbumsLoaded(List<Album> albums) {
		mAlbums.clear();

		if (null != albums) {
			mAlbums.addAll(albums);

			if (null != mAlbumsListener && mAlbumsListener != this) {
				mAlbumsListener.onAlbumsLoaded(mAlbums);
				mAlbumsListener = null;
			}
		}
	}

}
