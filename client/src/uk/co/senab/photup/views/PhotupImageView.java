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

			CacheableBitmapWrapper wrapper = new CacheableBitmapWrapper(Thumbnails.getThumbnail(mCr, id,
					Thumbnails.MICRO_KIND, null));
			mCache.put(id, wrapper);
			return wrapper;
		}

		@Override
		protected void onPostExecute(CacheableBitmapWrapper result) {
			super.onPostExecute(result);

			PhotupImageView iv = mImageView.get();
			if (null != iv && null != result) {
				iv.setImageBitmap(result);
			}
		}
	}

	private PhotoTask mCurrentTask;
	private CacheableBitmapWrapper mCurrentCacheableBitmapWrapper;

	public PhotupImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void requestThumbnailId(long id, BitmapLruCache cache) {
		if (null != mCurrentTask) {
			mCurrentTask.cancel(false);
		}

		CacheableBitmapWrapper cached = cache.get(id);
		if (null != cached) {
			setImageBitmap(cached);
		} else {
			PhotupApplication app = PhotupApplication.getApplication(getContext());
			mCurrentTask = new PhotoTask(app.getContentResolver(), this, cache);

			// FIXME Need to fix this for less than v11
			mCurrentTask.executeOnExecutor(app.getExecutorService(), id);
		}
	}

	public void setImageBitmap(CacheableBitmapWrapper wrapper) {
		setImageBitmap(wrapper.getBitmap());

		mCurrentCacheableBitmapWrapper = wrapper;
		wrapper.setDisplayed(true);
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		if (null != mCurrentCacheableBitmapWrapper) {
			mCurrentCacheableBitmapWrapper.setDisplayed(false);
			mCurrentCacheableBitmapWrapper = null;
		}

		BitmapDrawable d = new BitmapDrawable(getResources(), bm);
		d.setFilterBitmap(true);
		setImageDrawable(d);
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
