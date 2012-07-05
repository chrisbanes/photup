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
	
	public String getName() {
		return mName;
	}
}
