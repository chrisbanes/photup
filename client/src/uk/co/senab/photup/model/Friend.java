package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Friend extends AbstractFacebookObject {
	
	public static final String GRAPH_FIELDS = "id,name";

	public Friend(String id, String name) {
		super(id, name);
	}
	
	public Friend(JSONObject object) throws JSONException {
		super(object);
	}
	
	@Override
	public String toString() {
		return getName();
	}

}
