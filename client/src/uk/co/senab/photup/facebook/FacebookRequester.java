package uk.co.senab.photup.facebook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.model.Album;
import uk.co.senab.photup.model.Friend;
import android.content.Context;
import android.os.Bundle;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;

public class FacebookRequester {

	private final Context mContext;
	private final Facebook mFacebook;

	public FacebookRequester(Context context) {
		mContext = context;
		
		Session session = Session.restore(context);
		mFacebook = session.getFb();
	}

	public List<Friend> getFriends() {
		Bundle b = new Bundle();
		b.putString("date_format", "U");

		String response = null;
		try {
			response = mFacebook.request("me/friends", b);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (null == response) {
			return null;
		}

		try {
			JSONObject document = Util.parseJson(response);

			JSONArray data = document.getJSONArray("data");
			ArrayList<Friend> friends = new ArrayList<Friend>(data.length());

			JSONObject object;
			for (int i = 0, z = data.length(); i < z; i++) {
				try {
					object = data.getJSONObject(i);
					friends.add(new Friend(object));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			Collections.sort(friends, Friend.getComparator());
			
			return friends;
		} catch (FacebookError e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public List<Album> getAlbums() {
		return null;
	}

}
