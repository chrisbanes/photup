package uk.co.senab.photup.model;

import uk.co.senab.photup.Constants;
import android.util.Log;

public class PhotoTag {

	private final float mX, mY;
	private Friend mFriend;

	public PhotoTag(Friend friend, float x, float y) {
		mFriend = friend;
		mX = x;
		mY = y;

		if (Constants.DEBUG) {
			Log.d("PhotoTag", "X: " + x + " Y: " + y);
		}
	}

	public PhotoTag(float x, float y, int bitmapWidth, int bitmapHeight) {
		this(null, 100 * x / bitmapWidth, 100 * y / bitmapHeight);
	}

	public float getX() {
		return mX;
	}

	public float getY() {
		return mY;
	}

	public Friend getFriend() {
		return mFriend;
	}

	public boolean hasFriend() {
		return null != mFriend;
	}

	public void setFriend(Friend friend) {
		mFriend = friend;
	}

}
