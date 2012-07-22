package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

abstract class AbstractFacebookObject {

	private final String mId;
	private final String mName;

	public AbstractFacebookObject(String id, String name) {
		mId = id;
		mName = name;
	}

	public AbstractFacebookObject(JSONObject object) throws JSONException {
		mId = object.getString("id");
		mName = object.getString("name");
	}

	public String getId() {
		return mId;
	}

	@Override
	public String toString() {
		return mName;
	}

	public String getName() {
		return mName;
	}
	
	public String getAvatarUrl() {
		return new StringBuffer("http://graph.facebook.com/").append(mId).append("/picture").toString();
	}
}
