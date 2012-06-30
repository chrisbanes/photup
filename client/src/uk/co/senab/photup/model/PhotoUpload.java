package uk.co.senab.photup.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

public abstract class PhotoUpload {

	public abstract Uri getOriginalPhotoUri();

	public abstract Bitmap getThumbnail(Context context);

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
