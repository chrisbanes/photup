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
import android.text.TextUtils;

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
		b.putString("limit", "3000");

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

	public List<Album> getUploadableAlbums() {
		Bundle b = new Bundle();
		b.putString("date_format", "U");
		b.putString("limit", "3000");

		String response = null;
		try {
			response = mFacebook.request("me/albums", b);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (null == response) {
			return null;
		}

		try {
			JSONObject document = Util.parseJson(response);

			JSONArray data = document.getJSONArray("data");
			ArrayList<Album> albums = new ArrayList<Album>(data.length());

			JSONObject object;
			for (int i = 0, z = data.length(); i < z; i++) {
				try {
					object = data.getJSONObject(i);
					Album album = new Album(object);
					if (album.canUpload()) {
						albums.add(album);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			return albums;
		} catch (FacebookError e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String createNewAlbum(String albumName, String description, String privacy) {
		Bundle b = new Bundle();
		b.putString("name", albumName);

		if (!TextUtils.isEmpty(description)) {
			b.putString("message", description);
		}

		if (!TextUtils.isEmpty(privacy)) {
			try {
				JSONObject object = new JSONObject();
				object.put("value", privacy);
				b.putString("privacy", object.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		String response = null;
		try {
			response = mFacebook.request("me/albums", b, "POST");
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (null == response) {
			return null;
		}

		try {
			JSONObject document = Util.parseJson(response);
			return document.getString("id");
		} catch (FacebookError e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return null;
	}

}
