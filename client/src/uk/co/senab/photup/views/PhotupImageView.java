package uk.co.senab.photup.views;

import java.lang.ref.WeakReference;

import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.cache.BitmapLruCache;
import uk.co.senab.photup.cache.CacheableBitmapWrapper;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PhotupImageView extends ImageView {

	private static class PhotoTask extends AsyncTask<Long, Void, CacheableBitmapWrapper> {

		private final ContentResolver mCr;
		private final WeakReference<PhotupImageView> mImageView;
		private final BitmapLruCache mCache;

		public PhotoTask(ContentResolver cr, PhotupImageView imageView, BitmapLruCache cache) {
			mImageView = new WeakReference<PhotupImageView>(imageView);
			mCr = cr;
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
		protected CacheableBitmapWrapper doInBackground(Long... params) {
			long id = params[0];

			CacheableBitmapWrapper wrapper = null;

			Bitmap thumb = Thumbnails.getThumbnail(mCr, id, Thumbnails.MICRO_KIND, null);
			if (null != thumb) {
				wrapper = new CacheableBitmapWrapper(thumb);
				mCache.put(id, wrapper);
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

	public PhotupImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void requestThumbnailId(final long id, final BitmapLruCache cache) {
		if (null != mCurrentTask) {
			mCurrentTask.cancel(false);
		}

		CacheableBitmapWrapper cached = cache.get(id);
		if (null != cached) {
			setImageCachedBitmap(cached);
		} else {
			PhotupApplication app = PhotupApplication.getApplication(getContext());
			mCurrentTask = new PhotoTask(app.getContentResolver(), this, cache);

			// FIXME Need to fix this for less than v11
			mCurrentTask.executeOnExecutor(app.getExecutorService(), id);
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

			getDrawable().setCallback(null);
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
