package uk.co.senab.photup.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Place extends AbstractFacebookObject {
	
	public static final String GRAPH_FIELDS = "id,name";

	public Place(String id, String name) {
		super(id, name);
	}

	public Place(JSONObject object) throws JSONException {
		super(object);
	}

}
