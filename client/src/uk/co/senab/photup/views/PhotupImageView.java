package uk.co.senab.photup.views;

import java.lang.ref.WeakReference;

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
import android.widget.ImageView;

public class PhotupImageView extends ImageView {

	private static class PhotoTask extends AsyncTask<PhotoUpload, Void, CacheableBitmapWrapper> {

		private final WeakReference<PhotupImageView> mImageView;
		private final BitmapLruCache mCache;

		public PhotoTask(PhotupImageView imageView, BitmapLruCache cache) {
			mImageView = new WeakReference<PhotupImageView>(imageView);
			mCache = cache;
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
				Bitmap thumb = upload.getThumbnail(iv.getContext());
				if (null != thumb) {
					wrapper = new CacheableBitmapWrapper(thumb);
					mCache.put(upload, wrapper);
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

	public void requestThumbnailId(final PhotoUpload upload, final BitmapLruCache cache) {
		if (null != mCurrentTask) {
			mCurrentTask.cancel(false);
		}

		final CacheableBitmapWrapper cached = cache.get(upload);
		if (null != cached && cached.hasValidBitmap()) {
			setImageCachedBitmap(cached);
		} else {
			// Means we have an object with an invalid bitmap so remove it
			if (null != cached) {
				cache.remove(upload);
			}

			mCurrentTask = new PhotoTask(this, cache);

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
