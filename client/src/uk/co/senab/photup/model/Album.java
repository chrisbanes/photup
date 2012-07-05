package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Album extends AbstractFacebookObject {

	public static final String GRAPH_FIELDS = "id,name,can_upload,updated_time";

	private final boolean mCanUpload;
	private final long mUpdatedTime;

	public Album(String id, String name, boolean canUpload, long updatedTime) {
		super(id, name);
		mCanUpload = canUpload;
		mUpdatedTime = updatedTime;
	}

	public Album(JSONObject object) throws JSONException {
		super(object);
		mCanUpload = object.getBoolean("can_upload");
		mUpdatedTime = object.getLong("updated_time");
	}
	
	public boolean canUpload() {
		return mCanUpload;
	}
	
	public long getUpdatedTime() {
		return mUpdatedTime;
	}

}
