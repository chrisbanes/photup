package uk.co.senab.footo.cache;

import android.graphics.Bitmap;

public class CacheableBitmapWrapper {

	private final Bitmap mBitmap;
	private boolean mCurrentlyDisplayed;
	
	public CacheableBitmapWrapper(Bitmap bitmap) {
		mBitmap = bitmap;
		mCurrentlyDisplayed = false;
	}
	
	public void setDisplayed(boolean displayed) {
		mCurrentlyDisplayed = displayed;
	}
	
	public boolean getDisplayed() {
		return mCurrentlyDisplayed;
	}
	
	public Bitmap getBitmap() {
		return mBitmap;
	}
	
}
