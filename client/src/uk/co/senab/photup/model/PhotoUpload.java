package uk.co.senab.photup.model;

import uk.co.senab.photup.Constants;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.lightbox.android.photoprocessing.PhotoProcessing;

public abstract class PhotoUpload {

	private Filter mFilter;
	private String mCaption;

	public abstract Uri getOriginalPhotoUri();

	public abstract Bitmap getThumbnailImage(Context context);

	public abstract Bitmap getDisplayImage(Context context);

	public abstract Bitmap getUploadImage(Context context, int biggestDimension);

	public Bitmap processBitmap(Bitmap bitmap, final boolean modifyOriginal) {
		if (null != mFilter) {
			return PhotoProcessing.filterPhoto(bitmap, mFilter.getId(), modifyOriginal);
		} else {
			return bitmap;
		}
	}

	public static Bitmap resizePhoto(final Bitmap bitmap, final int maxDimension) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();
		final int biggestDimension = Math.max(width, height);

		if (biggestDimension <= maxDimension) {
			return bitmap;
		}

		final float ratio = maxDimension / (float) biggestDimension;
		Bitmap resized = PhotoProcessing.resize(bitmap, Math.round(width * ratio), Math.round(height * ratio));
		if (Constants.DEBUG) {
			Log.d("PhotoUpload", "Finely resized to: " + resized.getWidth() + "x" + resized.getHeight());
		}
		
		return resized;
	}

	public String getThumbnailImageKey() {
		return "thumb_" + getOriginalPhotoUri();
	}

	public String getDisplayImageKey() {
		return "dsply_" + getOriginalPhotoUri();
	}

	public void setFilterUsed(Filter filter) {
		mFilter = filter;
	}

	public Filter getFilterUsed() {
		return mFilter;
	}

	public boolean requiresProcessing() {
		return null != mFilter;
	}

	public String getCaption() {
		return mCaption;
	}

	public void setCaption(String caption) {
		mCaption = caption;
	}

	@Override
	public int hashCode() {
		return getOriginalPhotoUri().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MediaStorePhotoUpload) {
			return getOriginalPhotoUri().equals(((MediaStorePhotoUpload) obj).getOriginalPhotoUri());
		}
		return false;
	}

}
