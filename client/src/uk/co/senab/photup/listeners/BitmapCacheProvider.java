package uk.co.senab.photup.listeners;

import uk.co.senab.photup.cache.BitmapLruCache;

public interface BitmapCacheProvider {

	BitmapLruCache getBitmapCache();

}
