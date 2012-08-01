package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Group extends AbstractFacebookObject {
	
	public static final String GRAPH_FIELDS = "id,name";

	public Group(JSONObject object) throws JSONException {
		super(object);
	}

}
