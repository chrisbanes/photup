package uk.co.senab.photup.views;

import java.lang.ref.WeakReference;

import uk.co.senab.photup.Constants;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.cache.CacheableBitmapWrapper;
import uk.co.senab.photup.model.PhotoUpload;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class PhotupImageView extends ImageView {

	private static class PhotoTask extends AsyncTask<PhotoUpload, Void, CacheableBitmapWrapper> {

		private final WeakReference<PhotupImageView> mImageView;
		private final BitmapLruCache mCache;
		private final boolean mFetchFullSize;

		public PhotoTask(PhotupImageView imageView, BitmapLruCache cache, boolean fullSize) {
			mImageView = new WeakReference<PhotupImageView>(imageView);
			mCache = cache;
			mFetchFullSize = fullSize;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			PhotupImageView iv = mImageView.get();
			if (null != iv) {
				iv.setImageDrawable(null);
			}
		}

		@Override
		protected CacheableBitmapWrapper doInBackground(PhotoUpload... params) {
			final PhotoUpload upload = params[0];
			CacheableBitmapWrapper wrapper = null;

			PhotupImageView iv = mImageView.get();
			if (null != iv) {
				Bitmap bitmap = mFetchFullSize ? upload.getOriginal(iv.getContext()) : upload.getThumbnail(iv
						.getContext());

				if (null != bitmap) {
					wrapper = new CacheableBitmapWrapper(bitmap);
					final String key = mFetchFullSize ? upload.getOriginalKey() : upload.getThumbnailKey();
					mCache.put(key, wrapper);
				}
			}

			return wrapper;
		}

		@Override
		protected void onPostExecute(CacheableBitmapWrapper result) {
			super.onPostExecute(result);

			PhotupImageView iv = mImageView.get();
			if (null != iv) {
				iv.setImageCachedBitmap(result);
			}
		}
	}

	private PhotoTask mCurrentTask;
	private CacheableBitmapWrapper mCurrentCacheableBitmapWrapper;

	public PhotupImageView(Context context) {
		super(context);
	}

	public PhotupImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void requestThumbnail(final PhotoUpload upload, final BitmapLruCache cache) {
		requestImage(upload, cache, false);
	}

	public void requestFullSize(final PhotoUpload upload, final BitmapLruCache cache) {
		requestImage(upload, cache, true);
	}

	private void requestImage(final PhotoUpload upload, final BitmapLruCache cache, final boolean fullSize) {
		if (null != mCurrentTask) {
			mCurrentTask.cancel(false);
		}

		final String key = fullSize ? upload.getOriginalKey() : upload.getThumbnailKey();
		final CacheableBitmapWrapper cached = cache.get(key);

		if (null != cached && cached.hasValidBitmap()) {
			setImageCachedBitmap(cached);
		} else {
			// Means we have an object with an invalid bitmap so remove it
			if (null != cached) {
				cache.remove(key);
			}

			mCurrentTask = new PhotoTask(this, cache, fullSize);

			// FIXME Need to fix this for less than v11
			if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
				PhotupApplication app = PhotupApplication.getApplication(getContext());
				mCurrentTask.executeOnExecutor(app.getExecutorService(), upload);
			} else {
				mCurrentTask.execute(upload);
			}
		}
	}

	public void setImageCachedBitmap(final CacheableBitmapWrapper wrapper) {
		if (null != wrapper) {
			wrapper.setDisplayed(true);
			setImageBitmap(wrapper.getBitmap());
		} else {
			setImageDrawable(null);
		}

		mCurrentCacheableBitmapWrapper = wrapper;
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		BitmapDrawable d = new BitmapDrawable(getResources(), bm);
		d.setFilterBitmap(true);
		setImageDrawable(d);
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		super.setImageDrawable(drawable);

		if (null != mCurrentCacheableBitmapWrapper) {
			mCurrentCacheableBitmapWrapper.setDisplayed(false);
			mCurrentCacheableBitmapWrapper = null;
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		
		if (Constants.DEBUG) {
			Log.d(getClass().getSimpleName(), "Detached from Window");
		}

		// Will cause current cached drawable to be 'free-able'
		setImageDrawable(null);
	}

	public void recycleBitmap() {
		Bitmap currentBitmap = getCurrentBitmap();
		if (null != currentBitmap) {
			setImageDrawable(null);
			currentBitmap.recycle();
		}
	}

	private Bitmap getCurrentBitmap() {
		Drawable d = getDrawable();
		if (d instanceof BitmapDrawable) {
			return ((BitmapDrawable) d).getBitmap();
		}

		return null;
	}

}
