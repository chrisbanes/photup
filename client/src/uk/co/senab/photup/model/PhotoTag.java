package uk.co.senab.photup.model;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.senab.photup.Flags;
import android.text.TextUtils;
import android.util.Log;

public class PhotoTag {

	private final float mX;
	private final float mY;
	private String mFriendId;
	private FbUser mFriend;

	public PhotoTag(float x, float y) {
		mX = x;
		mY = y;

		if (Flags.DEBUG) {
			Log.d("PhotoTag", "X: " + x + " Y: " + y);
		}
	}

	public PhotoTag(JSONObject object) throws JSONException {
		this((float) object.getDouble("x"), (float) object.getDouble("y"));
		mFriendId = object.getString("tag_uid");
	}

	public PhotoTag(float x, float y, float bitmapWidth, float bitmapHeight) {
		this(100 * x / bitmapWidth, 100 * y / bitmapHeight);
	}

	public PhotoTag(float x, float y, int bitmapWidth, int bitmapHeight) {
		this(x, y, (float) bitmapHeight, (float) bitmapWidth);
	}

	public float getX() {
		return mX;
	}

	public float getY() {
		return mY;
	}

	public FbUser getFriend() {
		return mFriend;
	}

	public boolean hasFriend() {
		return null != mFriend;
	}

	public void populateFromFriends(HashMap<String, FbUser> friends) {
		if (null == mFriend && !TextUtils.isEmpty(mFriendId)) {
			mFriend = friends.get(mFriendId);
		}
	}

	public void setFriend(FbUser friend) {
		mFriend = friend;
		mFriendId = null != friend ? friend.getId() : null;
	}

	public JSONObject toJsonObject() throws JSONException {
		JSONObject object = new JSONObject();
		if (hasFriend()) {
			object.put("tag_uid", mFriend.getId());
		}
		object.put("x", mX);
		object.put("y", mY);
		return object;
	}

}
