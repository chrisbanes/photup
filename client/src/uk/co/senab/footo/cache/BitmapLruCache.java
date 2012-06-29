package uk.co.senab.footo.cache;

import uk.co.senab.footo.Constants;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class BitmapLruCache extends LruCache<Long, CacheableBitmapWrapper> {

	static final int DEFAULT_CACHE_SIZE = 8; // 1/8th
	static final String LOG_TAG = "BitmapLruCache";

	public BitmapLruCache(Context context) {
		this(1024 * 1024 * ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass()
				/ DEFAULT_CACHE_SIZE);
	}

	public BitmapLruCache(int maxSize) {
		super(maxSize);
	}

	@Override
	protected int sizeOf(Long key, CacheableBitmapWrapper value) {
		Bitmap bitmap = value.getBitmap();
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	@Override
	protected void entryRemoved(boolean evicted, Long key, CacheableBitmapWrapper oldValue,
			CacheableBitmapWrapper newValue) {

		if (!oldValue.getDisplayed()) {
			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "entryRemoved and recycled: " + key);
			}

			Bitmap bitmap = oldValue.getBitmap();
			bitmap.recycle();
		} else {
			// Should handle here too
		}
	}

}
