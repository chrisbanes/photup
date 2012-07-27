package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Facebook;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.facebook.Session;
import android.content.Context;
import android.text.TextUtils;

public class Account extends AbstractFacebookObject {

	private final String mAccessToken;
	
	public Account(String id, String name, String accessToken) {
		super(id, name);
		mAccessToken = accessToken;
	}

	public Account(JSONObject object) throws JSONException {
		super(object);
		mAccessToken = object.optString("access_token", null);
	}

	public String getAccessToken() {
		return mAccessToken;
	}
	
	public boolean hasAccessToken() {
		return !TextUtils.isEmpty(mAccessToken);
	}
	
	public static Account getMeFromSession(Session session) {
		return new Account(session.getUid(), session.getName(), session.getFb().getAccessToken());
	}
	
	public static Account getAccountFromSession(Context context) {
		return getMeFromSession(Session.restore(context));
	}
	
	public Facebook getFacebook() {
		Facebook facebook = new Facebook(Constants.FACEBOOK_APP_ID);
		facebook.setAccessToken(mAccessToken);
		facebook.setAccessExpires(0);
		return facebook;
	}

}
