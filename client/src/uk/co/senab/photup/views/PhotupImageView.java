package uk.co.senab.photup.views;

import java.lang.ref.WeakReference;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import uk.co.senab.bitmapcache.CacheableImageView;
import uk.co.senab.photup.PhotupApplication;
import uk.co.senab.photup.model.PhotoSelection;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;

public class PhotupImageView extends CacheableImageView {

	static final int FACE_DETECTION_DELAY = 800;

	private static class PhotoTask extends AsyncTask<Void, Void, CacheableBitmapWrapper> {

		private final WeakReference<PhotupImageView> mImageView;
		private final BitmapLruCache mCache;
		private final boolean mFetchFullSize;
		private final PhotoSelection mUpload;

		public PhotoTask(PhotupImageView imageView, PhotoSelection upload, BitmapLruCache cache, boolean fullSize) {
			mImageView = new WeakReference<PhotupImageView>(imageView);
			mCache = cache;
			mFetchFullSize = fullSize;
			mUpload = upload;
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
		protected CacheableBitmapWrapper doInBackground(Void... params) {
			CacheableBitmapWrapper wrapper = null;

			PhotupImageView iv = mImageView.get();
			if (null != iv) {
				Bitmap bitmap = mFetchFullSize ? mUpload.getDisplayImage(iv.getContext()) : mUpload
						.getThumbnailImage(iv.getContext());

				if (null != bitmap) {
					final String key = mFetchFullSize ? mUpload.getDisplayImageKey() : mUpload.getThumbnailImageKey();
					wrapper = new CacheableBitmapWrapper(key, bitmap);
				}
			}

			return wrapper;
		}

		@Override
		protected void onPostExecute(CacheableBitmapWrapper result) {
			super.onPostExecute(result);

			if (null != result) {
				PhotupImageView iv = mImageView.get();
				if (null != iv) {
					iv.setImageCachedBitmap(result);
				}

				mCache.put(result);
			}
		}
	}

	static class RequestFaceDetectionPassRunnable implements Runnable {

		private final PhotupImageView mImageView;
		private final PhotoSelection mSelection;

		public RequestFaceDetectionPassRunnable(PhotupImageView imageView, PhotoSelection selection) {
			mImageView = imageView;
			mSelection = selection;
		}

		public void run() {
			mImageView.requestFaceDetection(mSelection);
		}
	}

	static final class FilterRunnable implements Runnable {

		private final Context mContext;
		private final PhotupImageView mImageView;
		private final PhotoSelection mUpload;
		private final boolean mFullSize;
		private final BitmapLruCache mCache;

		public FilterRunnable(PhotupImageView imageView, PhotoSelection upload, final boolean fullSize) {
			mContext = imageView.getContext();
			mImageView = imageView;
			mUpload = upload;
			mFullSize = fullSize;
			mCache = PhotupApplication.getApplication(mContext).getImageCache();
		}

		public void run() {
			final Bitmap filteredBitmap;

			final String key = mFullSize ? mUpload.getDisplayImageKey() : mUpload.getThumbnailImageKey();
			CacheableBitmapWrapper wrapper = mCache.get(key);

			if (null == wrapper || !wrapper.hasValidBitmap()) {
				Bitmap bitmap = mFullSize ? mUpload.getDisplayImage(mContext) : mUpload.getThumbnailImage(mContext);
				wrapper = new CacheableBitmapWrapper(key, bitmap);
				wrapper.setBeingUsed(true);
				mCache.put(wrapper);
			} else {
				wrapper.setBeingUsed(true);
			}

			filteredBitmap = mUpload.processBitmap(wrapper.getBitmap(), false);
			wrapper.setBeingUsed(false);

			mImageView.post(new Runnable() {
				public void run() {
					mImageView.setImageBitmap(filteredBitmap);
				}
			});
		}
	};

	static final class FaceDetectionRunnable implements Runnable {

		private final PhotoSelection mUpload;
		private final CacheableBitmapWrapper mBitmapWrapper;

		public FaceDetectionRunnable(PhotoSelection upload, CacheableBitmapWrapper bitmap) {
			mUpload = upload;
			mBitmapWrapper = bitmap;
		}

		public void run() {
			if (mBitmapWrapper.hasValidBitmap()) {
				mUpload.detectPhotoTags(mBitmapWrapper.getBitmap());
			}
			mBitmapWrapper.setBeingUsed(false);
		}
	};

	private PhotoTask mCurrentTask;
	private boolean mFadeInDrawables = false;

	private Runnable mRequestFaceDetectionRunnable;

	public void setFadeInDrawables(boolean fadeIn) {
		mFadeInDrawables = fadeIn;
	}

	public PhotupImageView(Context context) {
		super(context);
	}

	public PhotupImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void postFaceDetection(PhotoSelection selection) {
		if (null == mRequestFaceDetectionRunnable && selection.requiresFaceDetectPass()) {
			mRequestFaceDetectionRunnable = new RequestFaceDetectionPassRunnable(this, selection);
			postDelayed(mRequestFaceDetectionRunnable, FACE_DETECTION_DELAY);
		}
	}

	public void clearFaceDetection() {
		if (null != mRequestFaceDetectionRunnable) {
			removeCallbacks(mRequestFaceDetectionRunnable);
			mRequestFaceDetectionRunnable = null;
		}
	}

	public void requestThumbnail(final PhotoSelection upload, final boolean honourFilter) {
		if (upload.requiresProcessing() && honourFilter) {
			requestFiltered(upload, false);
		} else {
			requestImage(upload, false);
		}
	}

	public void requestFullSize(final PhotoSelection upload, final boolean honourFilter) {
		if (upload.requiresProcessing() && honourFilter) {
			requestFiltered(upload, true);
		} else {
			requestImage(upload, true);
		}
	}

	void requestFiltered(final PhotoSelection upload, boolean fullSize) {
		PhotupApplication app = PhotupApplication.getApplication(getContext());
		app.getSingleThreadExecutorService().submit(new FilterRunnable(this, upload, fullSize));
	}

	void requestFaceDetection(final PhotoSelection upload) {
		CacheableBitmapWrapper wrapper = getCachedBitmapWrapper();
		if (null != wrapper && wrapper.hasValidBitmap()) {
			wrapper.setBeingUsed(true);

			PhotupApplication app = PhotupApplication.getApplication(getContext());
			app.getMultiThreadExecutorService().submit(new FaceDetectionRunnable(upload, wrapper));
		}
	}

	void requestImage(final PhotoSelection upload, final boolean fullSize) {
		if (null != mCurrentTask) {
			mCurrentTask.cancel(false);
		}

		final String key = fullSize ? upload.getDisplayImageKey() : upload.getThumbnailImageKey();
		BitmapLruCache cache = PhotupApplication.getApplication(getContext()).getImageCache();
		final CacheableBitmapWrapper cached = cache.get(key);

		if (null != cached && cached.hasValidBitmap()) {
			setImageCachedBitmap(cached);
		} else {
			// Means we have an object with an invalid bitmap so remove it
			if (null != cached) {
				cache.remove(key);
			}

			mCurrentTask = new PhotoTask(this, upload, cache, fullSize);

			if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
				PhotupApplication app = PhotupApplication.getApplication(getContext());
				mCurrentTask.executeOnExecutor(app.getMultiThreadExecutorService());
			} else {
				mCurrentTask.execute();
			}
		}
	}

	public void recycleBitmap() {
		Bitmap currentBitmap = getCurrentBitmap();
		if (null != currentBitmap) {
			setImageDrawable(null);
			currentBitmap.recycle();
		}
	}

	public Bitmap getCurrentBitmap() {
		Drawable d = getDrawable();
		if (d instanceof BitmapDrawable) {
			return ((BitmapDrawable) d).getBitmap();
		}

		return null;
	}

	@Override
	public void setImageDrawable(Drawable drawable) {
		if (mFadeInDrawables && null != drawable) {
			TransitionDrawable newDrawable = new TransitionDrawable(new Drawable[] {
					new ColorDrawable(Color.TRANSPARENT), drawable });
			super.setImageDrawable(newDrawable);
			newDrawable.startTransition(300);
		} else {
			super.setImageDrawable(drawable);
		}
	}

}
