package uk.co.senab.photup.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.lightbox.android.photoprocessing.PhotoProcessing;

public abstract class PhotoUpload {

	private Filter mFilter;
	private String mCaption;

	

	public abstract Uri getOriginalPhotoUri();

	public abstract Bitmap getThumbnail(Context context);

	public abstract Bitmap getOriginal(Context context);

	public Bitmap getProcessedOriginal(Bitmap bitmap) {
		if (null != mFilter) {
			return PhotoProcessing.filterPhoto(bitmap, mFilter.getId());
		} else {
			return bitmap;
		}
	}

	public String getThumbnailKey() {
		return "thumb_" + getOriginalPhotoUri();
	}

	public String getOriginalKey() {
		return "full_" + getOriginalPhotoUri();
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
