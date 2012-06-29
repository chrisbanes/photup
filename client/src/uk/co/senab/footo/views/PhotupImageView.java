package uk.co.senab.footo.views;

import java.lang.ref.WeakReference;

import uk.co.senab.footo.PhotupApplication;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Thumbnails;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PhotupImageView extends ImageView {

	private static class PhotoTask extends AsyncTask<Long, Void, Bitmap> {

		private final ContentResolver mCr;
		private final WeakReference<PhotupImageView> mImageView;

		public PhotoTask(ContentResolver cr, PhotupImageView imageView) {
			mImageView = new WeakReference<PhotupImageView>(imageView);
			mCr = cr;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			PhotupImageView iv = mImageView.get();
			if (null != iv) {
				iv.recycleBitmap();
			}
		}

		@Override
		protected Bitmap doInBackground(Long... params) {
			return Images.Thumbnails.getThumbnail(mCr, params[0], Thumbnails.MICRO_KIND, null);
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);

			ImageView iv = mImageView.get();
			if (null != iv && null != result) {
				iv.setImageBitmap(result);
			}
		}
	}
	
	
	private PhotoTask mCurrentTask;

	public PhotupImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void requestThumbnailId(long id) {
		if (null != mCurrentTask) {
			mCurrentTask.cancel(false);
		}
		
		mCurrentTask = new PhotoTask(getContext().getContentResolver(), this);
		mCurrentTask.executeOnExecutor(PhotupApplication.getApplication(getContext()).getExecutorService(), id);
	}

	@Override
	public void setImageBitmap(Bitmap bm) {
		recycleBitmap();
		
		BitmapDrawable d = new BitmapDrawable(getResources(), bm);
		d.setFilterBitmap(true);
		setImageDrawable(d);
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
