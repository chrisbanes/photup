package uk.co.senab.photup.model;

import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

public class FbUser extends AbstractFacebookObject {

	public static final String GRAPH_FIELDS = "id,name";

	public FbUser(String id, String name) {
		super(id, name);
	}

	public FbUser(JSONObject object) throws JSONException {
		super(object);
	}

	public static Comparator<FbUser> getComparator() {
		return new Comparator<FbUser>() {
			public int compare(FbUser lhs, FbUser rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
		};
	}

	public static FbUser getMeFromAccount(Account account) {
		return new FbUser(account.getId(), account.getName());
	}

}
