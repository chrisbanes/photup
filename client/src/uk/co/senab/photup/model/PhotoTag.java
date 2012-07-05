package uk.co.senab.photup.model;

public class PhotoTag {

	private final float mX, mY;
	private Friend mFriend;

	public PhotoTag(Friend friend, float x, float y) {
		mFriend = friend;
		mX = x;
		mY = y;
	}

	public PhotoTag(float x, float y, int bitmapWidth, int bitmapHeight) {
		this(null, x / bitmapWidth, y / bitmapHeight);
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
