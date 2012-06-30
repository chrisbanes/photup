package uk.co.senab.photup.cache;

import java.util.Map.Entry;

import uk.co.senab.photup.Constants;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class BitmapLruCache extends LruCache<String, CacheableBitmapWrapper> {

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
	protected int sizeOf(String key, CacheableBitmapWrapper value) {
		Bitmap bitmap = value.getBitmap();
		return bitmap.getRowBytes() * bitmap.getHeight();
	}

	@Override
	protected void entryRemoved(boolean evicted, String key, CacheableBitmapWrapper oldValue,
			CacheableBitmapWrapper newValue) {

		if (!oldValue.isBeingDisplayed()) {
			if (Constants.DEBUG) {
				Log.d(LOG_TAG, "entryRemoved and recycled: " + key);
			}

			Bitmap bitmap = oldValue.getBitmap();
			bitmap.recycle();
		} else {
			// Should handle here too
		}
	}
	
	public void trimMemory() {
		for (Entry<String, CacheableBitmapWrapper> entry : snapshot().entrySet()) {
			CacheableBitmapWrapper value = entry.getValue();
			if (null == value || !value.isBeingDisplayed()) {
				remove(entry.getKey());
			}
		}
	}

}
