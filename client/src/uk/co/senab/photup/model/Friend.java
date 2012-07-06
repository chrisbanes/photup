package uk.co.senab.photup.model;

import java.util.Comparator;

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

	public static Comparator<Friend> getComparator() {
		return new Comparator<Friend>() {
			public int compare(Friend lhs, Friend rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
		};
	}

}
