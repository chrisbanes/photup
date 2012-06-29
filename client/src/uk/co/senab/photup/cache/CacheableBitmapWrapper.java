package uk.co.senab.photup.cache;

import android.graphics.Bitmap;

public class CacheableBitmapWrapper {

	private final Bitmap mBitmap;
	private int mCurrentlyDisplayed;

	public CacheableBitmapWrapper(Bitmap bitmap) {
		mBitmap = bitmap;
		mCurrentlyDisplayed = 0;
	}

	public void setDisplayed(boolean displayed) {
		if (displayed) {
			mCurrentlyDisplayed++;
		} else {
			mCurrentlyDisplayed--;
		}
	}

	public boolean getDisplayed() {
		return mCurrentlyDisplayed > 0;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

}
