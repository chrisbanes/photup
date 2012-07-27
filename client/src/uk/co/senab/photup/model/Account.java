package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.facebook.Session;
import android.content.Context;
import android.text.TextUtils;

import com.facebook.android.Facebook;

public class Account extends AbstractFacebookObject {

	private final String mAccessToken;
	private final long mAccessExpires;

	public Account(String id, String name, String accessToken, long accessExpires) {
		super(id, name);
		mAccessToken = accessToken;
		mAccessExpires = accessExpires;
	}

	public Account(JSONObject object) throws JSONException {
		super(object);
		mAccessToken = object.optString("access_token", null);
		mAccessExpires = 0;
	}

	public String getAccessToken() {
		return mAccessToken;
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

}
