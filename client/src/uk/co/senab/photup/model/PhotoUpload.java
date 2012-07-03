package uk.co.senab.photup.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

public abstract class PhotoUpload {

	private Filter mFilter;

	public abstract Uri getOriginalPhotoUri();

	public abstract Bitmap getThumbnail(Context context);

	public abstract Bitmap getOriginal(Context context);

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

	public boolean hasFilter() {
		return null != mFilter;
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
